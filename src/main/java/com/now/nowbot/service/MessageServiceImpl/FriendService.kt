package com.now.nowbot.service.MessageServiceImpl

import com.now.nowbot.dao.BindDao
import com.now.nowbot.model.BinUser
import com.now.nowbot.model.JsonData.MicroUser
import com.now.nowbot.model.JsonData.OsuUser
import com.now.nowbot.qq.contact.Contact
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageServiceImpl.FriendService.FriendParam
import com.now.nowbot.service.OsuApiService.OsuUserApiService
import com.now.nowbot.throwable.ServiceException.FriendException
import com.now.nowbot.util.CmdObject
import com.now.nowbot.util.CmdUtil
import com.now.nowbot.util.Instruction
import jakarta.annotation.Resource
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.util.CollectionUtils
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

@Service("FRIEND")
class FriendService(
    private val bindDao: BindDao,
    private val userApiService: OsuUserApiService,
    private val imageService: ImageService,
) : MessageService<FriendParam?> {

    data class FriendParam(val offset: Int, val limit: Int, val uid: Long = 0, val name: String? = null)

    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: MessageService.DataValue<FriendParam?>
    ): Boolean {
        val m = Instruction.FRIEND.matcher(messageText)
        if (!m.find()) {
            return false
        }

        val isMyself = AtomicBoolean(false)
        val range = CmdUtil.getUserWithRange(event, m, CmdObject(), isMyself)
        if (range.data != null && !isMyself.get()) {
            // 如果不是自己代表是 !f xxx / @
            val u = range.data
            data.value = FriendParam(0, 0, u?.userID ?: 0, u?.username)
        } else {
            val offset = range.getValue(0, false)
            val limit = range.getValue(12, true)
            data.value = FriendParam(offset, limit, 0)
        }
        return true
    }

    @Throws(Throwable::class)
    override fun HandleMessage(event: MessageEvent, param: FriendParam?) {
        val from = event.subject
        val binUser = try {
            bindDao!!.getUserFromQQ(event.sender.id)
        } catch (e: Exception) {
            throw FriendException(FriendException.Type.FRIEND_Me_NoPermission)
        }

        if (binUser == null) {
            throw FriendException(FriendException.Type.FRIEND_Me_NoBind)
        } else if (!binUser.isAuthorized) {
            throw FriendException(FriendException.Type.FRIEND_Me_NoPermission)
            //无权限
        }

        if (param!!.uid != 0L) {
            // 判断是不是好友
            checkMultiFriend(from, binUser, param)
        } else {
            sendFriendList(from, binUser, param)
        }
    }

    fun checkMultiFriend(from: Contact, binUser: BinUser, user: FriendParam) {
        val friendList = userApiService.getFriendList(binUser)
        val uid = user.uid
        val friend = friendList.find { it?.userID == uid }
        //
        val isMulti = try {
            val otherBinUser = bindDao?.getBindUser(uid) ?: throw Exception()
            if (!otherBinUser.isAuthorized) {
                throw Exception()
            }
            userApiService.getFriendList(otherBinUser).find { it?.userID == binUser.osuID } != null
        } catch (ignore: Exception) {
            // 对面没绑定不处理
            false
        }

        val message = if (friend == null) {
            "你并没有关注 ${user.name}"
        } else {
            if (isMulti) {
                "你与 ${friend.userName} 互相关注了"
            } else {
                "你已经关注 ${friend.userName} 了"
            }
        }
        from.sendText(message)
    }

    fun sendFriendList(from: Contact, binUser: BinUser, param: FriendParam) {
        val osuUser: OsuUser

        val friends: MutableList<MicroUser?> = ArrayList()

        //拿到参数,默认1-24个
        val offset = param.offset
        val limit = param.limit
        val doRandom = (offset == 0 && limit == 12)

        if (limit == 0 || 100 < limit - offset) {
            throw FriendException(FriendException.Type.FRIEND_Client_ParameterOutOfBounds)
        }


        try {
            osuUser = userApiService.getPlayerInfo(binUser)
        } catch (e: HttpClientErrorException.Unauthorized) {
            throw FriendException(FriendException.Type.FRIEND_Me_TokenExpired)
        } catch (e: WebClientResponseException.Unauthorized) {
            throw FriendException(FriendException.Type.FRIEND_Me_TokenExpired)
        } catch (e: Exception) {
            throw FriendException(FriendException.Type.FRIEND_Me_NotFound)
        }

        val friendList: List<MicroUser>
        try {
            friendList = userApiService.getFriendList(binUser)
        } catch (e: Exception) {
            throw FriendException(FriendException.Type.FRIEND_Me_FetchFailed)
        }

        var index: IntArray? = null
        if (doRandom) {
            //构造随机数组
            index = IntArray(friendList.size)
            for (i in index.indices) {
                index[i] = i
            }
            for (i in index.indices) {
                val rand = rand(i, index.size)
                if (rand != 1) {
                    val temp = index[rand]
                    index[rand] = index[i]
                    index[i] = temp
                }
            }
        }

        var i = offset
        while (i < limit && i < friendList.size) {
            if (doRandom) {
                friends.add(friendList[index!![i]])
            } else {
                try {
                    friends.add(friendList[offset + i])
                } catch (e: IndexOutOfBoundsException) {
                    log.error("Friend: 莫名其妙的数组越界", e)
                    throw FriendException(FriendException.Type.FRIEND_Send_Error)
                }
            }
            i++
        }

        if (CollectionUtils.isEmpty(friends)) throw FriendException(FriendException.Type.FRIEND_Client_NoFriend)

        try {
            val image = imageService.getPanelA1(osuUser, friends)
            from.sendImage(image)
        } catch (e: Exception) {
            log.error("Friend: ", e)
            throw FriendException(FriendException.Type.FRIEND_Send_Error)
        }
    }


    companion object {
        private val log: Logger = LoggerFactory.getLogger(FriendService::class.java)
        val random: Random = Random()

        fun rand(min: Int, max: Int): Int {
            return min + random.nextInt(max - min)
        }
    }
}