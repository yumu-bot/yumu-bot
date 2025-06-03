package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.dao.BindDao
import com.now.nowbot.model.BindUser
import com.now.nowbot.model.osu.LazerFriend
import com.now.nowbot.model.osu.MicroUser
import com.now.nowbot.model.osu.OsuUser
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.messageServiceImpl.FriendService.Companion.SortDirection.*
import com.now.nowbot.service.messageServiceImpl.FriendService.Companion.SortType.*
import com.now.nowbot.service.messageServiceImpl.FriendService.FriendParam
import com.now.nowbot.service.osuApiService.OsuUserApiService
import com.now.nowbot.throwable.serviceException.FriendException
import com.now.nowbot.util.CmdObject
import com.now.nowbot.util.CmdUtil
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.QQMsgUtil
import com.yumu.core.extensions.isNotNull
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

@Service("FRIEND")
class FriendService(
    private val bindDao: BindDao,
    private val userApiService: OsuUserApiService,
    private val imageService: ImageService,
) : MessageService<FriendParam> {

    data class FriendParam(
        val offset: Int,
        val limit: Int,
        val uid: Long = 0,
        val user: OsuUser? = null,
        val sort: Pair<SortType, SortDirection> = NULL to RANDOM
    )

    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: MessageService.DataValue<FriendParam>
    ): Boolean {
        val m = Instruction.FRIEND.matcher(messageText)
        if (!m.find()) {
            return false
        }

        val sort: Pair<SortType, SortDirection> = getSort(m.group("sort"))

        val isMyself = AtomicBoolean(true)
        val range = CmdUtil.getUserWithRange(event, m, CmdObject(), isMyself)
        if (range.data != null && !isMyself.get()) {
            // 如果不是自己代表是 !f xxx / @
            val u = range.data
            data.value = FriendParam(0, 0, u?.userID ?: 0, u, sort)
        } else {
            val offset = range.getOffset(0, true)
            val limit = range.getLimit(20, true)

            // 如果有输入参数，则默认按名称排序
            val s = if ((range.start == null && range.end == null).not() && sort.second == RANDOM) {
                NAME to ASCEND
            } else {
                sort
            }

            data.value = FriendParam(offset, limit, 0, range.data, s)
        }
        return true
    }

    @Throws(Throwable::class)
    override fun HandleMessage(event: MessageEvent, param: FriendParam) {
        val bu = bindDao.getBindFromQQ(event.sender.id, true)


        if (!bu.isAuthorized) {
            throw FriendException(FriendException.Type.FRIEND_Me_NoPermission)
            // 无权限
        }

        val message = if (param.uid != 0L) {
            // 判断是不是好友
            checkMultiFriend(bu, param)
        } else {
            sendFriendList(bu, param)
        }

        event.reply(message)
    }

    fun checkMultiFriend(bindUser: BindUser, param: FriendParam): MessageChain {
        if (param.uid == bindUser.userID) {
            return MessageChain("你自己与你自己就是最好的朋友。")
        }

        val friendList = userApiService.getFriendList(bindUser)

        val message = getMutualInfo(bindUser, param, friendList)

        return MessageChain(message)
    }

    fun getMutualInfo(
        bindUser: BindUser,
        param: FriendParam,
        friendList: List<LazerFriend>
    ): String {
        val uid = param.uid
        val name = param.user?.username ?: bindUser.username
        val friend = friendList.find { it.target.userID == uid }

        // 加了对方 直接判断是否互 mu
        if (friend != null) {
            return if (friend.isMutual) {
                "恭喜！你已经与 $name 互相成为好友了。"
            } else {
                "你已经添加了 $name 作为你的好友，但对方似乎还没有添加你。"
            }
        }

        val other = try {
            bindDao.getBindUser(uid) ?: null
        } catch (ignored: Exception) {
            null
        }

        // 没加对方, 对方没绑定
        if (other?.isAuthorized == null) {
            return "你还没有将 $name 添加为你的好友，并且对方没有使用链接绑定，还不知道有没有添加你。"
        }

        val isFollowed = try {
            userApiService
                .getFriendList(other)
                .find { it.target.userID == bindUser.userID }
                .isNotNull()
        } catch (ignored: Exception) {
            false
        }

        // 对方是否加你
        return if (isFollowed) {
            "你还没有将 $name 添加为你的好友，但对方似乎已经悄悄添加了你。"
        } else {
            "你们暂未互相成为好友。或许可以考虑一下？"
        }
    }

    fun sendFriendList(bindUser: BindUser, param: FriendParam): MessageChain {
        val friends = mutableListOf<MicroUser>()
        val sortType = param.sort.first
        val sortDirection = param.sort.second

        val offset = param.offset
        val limit = param.limit

        if (limit == 0 || 100 < limit - offset) {
            throw FriendException(FriendException.Type.FRIEND_Client_ParameterOutOfBounds)
        }

        val osuUser = param.user ?: try {
            userApiService.getOsuUser(bindUser)
        } catch (e: HttpClientErrorException.Unauthorized) {
            throw FriendException(FriendException.Type.FRIEND_Me_TokenExpired)
        } catch (e: WebClientResponseException.Unauthorized) {
            throw FriendException(FriendException.Type.FRIEND_Me_TokenExpired)
        } catch (e: Exception) {
            throw FriendException(FriendException.Type.FRIEND_Me_NotFound)
        }

        val rawList = try {
            userApiService.getFriendList(bindUser).map {
                it.target
            }.toMutableList()
        } catch (e: Exception) {
            throw FriendException(FriendException.Type.FRIEND_Me_FetchFailed)
        }

        val sequence = if (sortDirection == DESCEND) {
            // 先翻一次，因为等会要翻回来，这样可以保证都是默认按名字升序排序的
            rawList.asSequence().filter { !it.isBot }.sortedByDescending { it.userName }
        } else {
            rawList.asSequence().filter { !it.isBot }.sortedBy { it.userName }
        }

        val sorted =
            when (sortType) {
                PERFORMANCE -> sequence
                    .filter { it.statistics!!.pp!! > 0 }
                    .sortedBy { it.statistics!!.pp!! }

                ACCURACY -> sequence
                    .filter { it.statistics!!.accuracy!! > 0 }
                    .sortedBy { it.statistics!!.accuracy!! }

                TIME -> sequence
                    .filter { it.lastVisitTime != null }
                    .sortedBy { it.lastVisitTime }

                PLAY_COUNT -> sequence
                    .sortedBy { it.statistics!!.playCount }
                PLAY_TIME -> sequence
                    .sortedBy { it.statistics!!.playTime }
                TOTAL_HITS -> sequence
                    .sortedBy { it.statistics!!.totalHits }
                ONLINE -> sequence
                    .filter { it.statistics!!.pp!! > 0 }
                    .sortedByDescending { it.statistics!!.pp!! }
                    .filter { it.isOnline }

                MUTUAL -> sequence
                    .filter { it.isMutual }
                UID -> sequence
                    .sortedBy { it.userID }
                COUNTRY -> sequence
                    .sortedBy { it.countryCode }
                else -> sequence
            }

        val result = when (sortDirection) {
            ASCEND, TRUE -> sorted.toList()
            DESCEND -> sorted.toList().reversed()
            RANDOM -> sorted.shuffled().toList()
            // 取差集
            FALSE -> {
                rawList
                    .asSequence()
                    .filter { ! it.isBot }
                    .sortedBy { it.userName }
                    .filter { ! sorted.contains(it) }
                    .toList()
            }
        }

        var i = offset
        while (i < offset + limit && i < result.size) {
            friends.add(result[i])
            i++
        }

        if (friends.isEmpty()) {
            if (sortType == NULL) {
                throw FriendException(FriendException.Type.FRIEND_Client_NoFriend)
            } else {
                throw FriendException(FriendException.Type.FRIEND_Client_NoMatch)
            }
        }

        userApiService.asyncDownloadAvatar(friends)
        userApiService.asyncDownloadBackground(friends)

        try {
            val type = param.sort.first.name.lowercase()

            val body = mapOf(
                "me_card_A1" to osuUser,
                "friend_card_A1" to friends,
                "type" to type,
            )

            val image = imageService.getPanel(body, "A1")
            return QQMsgUtil.getImage(image)
        } catch (e: Exception) {
            log.error("Friend: ", e)
            throw FriendException(FriendException.Type.FRIEND_Send_Error)
        }
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(FriendService::class.java)


        enum class SortType {
            NULL, PERFORMANCE, ACCURACY, PLAY_COUNT, PLAY_TIME, TOTAL_HITS, TIME, UID, COUNTRY, NAME, ONLINE, MUTUAL
        }

        enum class SortDirection {
            RANDOM, ASCEND, DESCEND, TRUE, FALSE
        }

        private fun getSort(type: String?): Pair<SortType, SortDirection> {
            if (type.isNullOrBlank()) return NULL to RANDOM

            return when (type.replace("\\s*".toRegex(), "").lowercase()) {
                "p", "pp", "performance", "p-", "pp-", "performance-" -> PERFORMANCE to DESCEND
                "p2", "pp2", "performance2", "p+", "pp+", "performance+" -> PERFORMANCE to ASCEND
                "a", "acc", "accuracy", "a-", "acc-", "accuracy-" -> ACCURACY to DESCEND
                "a2", "acc2", "accuracy2", "a+", "acc+", "accuracy+" -> ACCURACY to ASCEND
                "pc", "playcount", "pc-", "playcount-" -> PLAY_COUNT to DESCEND
                "pc2", "playcount2", "pc+", "playcount+" -> PLAY_COUNT to ASCEND
                "pt", "playtime", "pt-", "playtime-" -> PLAY_TIME to DESCEND
                "pt2", "playtime2", "pt+", "playtime+" -> PLAY_TIME to ASCEND
                "h", "th", "tth", "hit", "totalhit", "totalhits", "h-", "th-", "tth-", "hit-", "totalhit-", "totalhits-" -> TOTAL_HITS to DESCEND
                "h2", "th2", "tth2", "hit2", "totalhit2", "totalhits2", "h+", "th+", "tth+", "hit+", "totalhit+", "totalhits+" -> TOTAL_HITS to ASCEND

                "t", "time", "seen", "t+", "time+", "seen+" -> TIME to ASCEND
                "t2", "time2", "seen2", "t-", "time-", "seen-" -> TIME to DESCEND
                "u", "uid", "u+", "uid+" -> UID to ASCEND
                "u2", "uid2", "u-", "uid-" -> UID to DESCEND
                "c", "country", "c+", "country+" -> COUNTRY to ASCEND
                "c2", "country2", "c-", "country-" -> COUNTRY to DESCEND
                "n", "name", "n+", "name+" -> NAME to ASCEND
                "n2", "name2", "n-", "name-" -> NAME to DESCEND

                "o", "on", "online", "o+", "online+" -> ONLINE to TRUE
                "o2", "online2", "o-", "online-", "f", "off", "offline" -> ONLINE to FALSE

                "m", "mu", "mutual", "unidirectional", "single", "follow", "m-", "mu-", "mutual-" -> MUTUAL to FALSE
                "m2", "mu2", "mutual2", "m+", "mu+", "mutual+" -> MUTUAL to TRUE

                else -> NULL to RANDOM
            }
        }
    }
}
