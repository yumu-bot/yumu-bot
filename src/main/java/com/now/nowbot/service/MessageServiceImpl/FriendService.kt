package com.now.nowbot.service.MessageServiceImpl

import com.now.nowbot.dao.BindDao
import com.now.nowbot.model.BinUser
import com.now.nowbot.model.JsonData.MicroUser
import com.now.nowbot.model.JsonData.OsuUser
import com.now.nowbot.qq.contact.Contact
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageServiceImpl.FriendService.FriendParam
import com.now.nowbot.service.OsuApiService.OsuUserApiService
import com.now.nowbot.throwable.GeneralTipsException
import com.now.nowbot.throwable.ServiceException.FriendException
import com.now.nowbot.util.CmdObject
import com.now.nowbot.util.CmdUtil
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.QQMsgUtil
import com.yumu.core.extensions.isNotNull
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.util.CollectionUtils
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.reactive.function.client.WebClientResponseException

@Service("FRIEND")
class FriendService(
    private val bindDao: BindDao,
    private val userApiService: OsuUserApiService,
    private val imageService: ImageService,
) : MessageService<FriendParam?> {

    data class FriendParam(
        val offset: Int,
        val limit: Int,
        val uid: Long = 0,
        val name: String? = null
    )

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
        } else if (m.group("ur").isNotBlank()) {
            throw GeneralTipsException(GeneralTipsException.Type.G_Null_Player, m.group("ur"))
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
            bindDao.getUserFromQQ(event.sender.id) ?: throw Exception()
        } catch (e: Exception) {
            throw FriendException(FriendException.Type.FRIEND_Me_NoBind)
        }


        if (!binUser.isAuthorized) {
            throw FriendException(FriendException.Type.FRIEND_Me_NoPermission)
            // 无权限
        }

        val message = if (param!!.uid != 0L) {
            // 判断是不是好友
            checkMultiFriend(binUser, param)
        } else {
            sendFriendList(binUser, param)
        }

        from.sendMessage(message)
    }

    fun checkMultiFriend(binUser: BinUser, param: FriendParam): MessageChain {
        if (param.uid == binUser.osuID) {
            return MessageChain("你自己与你自己就是最好的朋友")
        }

        val friendList = userApiService.getFriendList(binUser)

        val message = getMutualInfo(binUser, param, friendList)

        return MessageChain(message)
    }

    fun getMutualInfo(
        binUser: BinUser,
        param: FriendParam,
        friendList: MutableList<MicroUser?>
    ): String {
        val uid = param.uid
        val friend = friendList.find { it?.userID == uid }

        val other = try {
            bindDao.getBindUser(uid) ?: null
        } catch (ignored: Exception) {
            null
        }

        val isBind = other?.isAuthorized ?: false

        val isFollowed = try {
            if (isBind) {
                userApiService
                    .getFriendList(other)
                    .find { it?.userID == binUser.osuID }
                    .isNotNull()
            } else {
                false
            }
        } catch (ignored: Exception) {
            false
        }

        val isFollowing = friend.isNotNull()

        return if (isFollowing) {
            if (isFollowed) {
                "你已经与 ${param.name} 互相成为好友了。"
            } else if (isBind) {
                "你已经添加了 ${param.name} 作为你的好友，但对方似乎还没有添加你。"
            } else {
                "你已经添加了 ${param.name} 作为你的好友，但不知道对方有没有添加你。"
            }
        } else {
            if (isFollowed) {
                "你还没有将 ${param.name} 添加为你的好友，但对方似乎已经悄悄添加了你。"
            } else if (isBind) {
                "你们暂未互相成为好友。"
            } else {
                "你还没有将 ${param.name} 添加为你的好友，也不知道对方有没有添加你。"
            }
        }
    }

    fun sendFriendList(binUser: BinUser, param: FriendParam): MessageChain {
        val friends: MutableList<MicroUser?> = ArrayList()

        // 拿到参数,默认1-24个
        val offset = param.offset
        val limit = param.limit
        val doRandom = (offset == 0 && limit == 12)

        if (limit == 0 || 100 < limit - offset) {
            throw FriendException(FriendException.Type.FRIEND_Client_ParameterOutOfBounds)
        }

        val osuUser = try {
            userApiService.getPlayerInfo(binUser)
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

        if (doRandom) {
            // 随机打乱好友
            friendList.shuffle()
        }

        var i = offset
        while (i < limit && i < friendList.size) {
            friends.add(friendList[i])
            i++
        }

        if (CollectionUtils.isEmpty(friends))
            throw FriendException(FriendException.Type.FRIEND_Client_NoFriend)

        try {
            val image = imageService.getPanelA1(osuUser, friends)
            return QQMsgUtil.getImage(image)
        } catch (e: Exception) {
            log.error("Friend: ", e)
            throw FriendException(FriendException.Type.FRIEND_Send_Error)
        }
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(FriendService::class.java)
    }
}
