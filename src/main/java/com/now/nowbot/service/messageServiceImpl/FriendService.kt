package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.dao.BindDao
import com.now.nowbot.model.filter.MicroUserFilter
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
import com.now.nowbot.throwable.TipsException
import com.now.nowbot.throwable.botException.FriendException
import com.now.nowbot.throwable.botRuntimeException.BindException
import com.now.nowbot.throwable.botRuntimeException.IllegalArgumentException
import com.now.nowbot.throwable.botRuntimeException.IllegalStateException
import com.now.nowbot.util.*
import com.now.nowbot.util.CmdUtil.getMode
import com.now.nowbot.util.CmdUtil.getUserWithRange
import com.now.nowbot.util.command.FLAG_RANGE
import com.now.nowbot.util.command.FLAG_UID
import com.now.nowbot.util.command.REG_HYPHEN
import com.now.nowbot.util.command.REG_RANGE
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.atomic.AtomicBoolean
import java.util.regex.Matcher

@Service("FRIEND")
class FriendService(
    private val bindDao: BindDao,
    private val userApiService: OsuUserApiService,
    private val imageService: ImageService,
) : MessageService<FriendParam> {

    abstract class FriendParam

    data class FriendListParam(
        val user: OsuUser,
        val friends: List<MicroUser>,
        val sortType: SortType,
    ) : FriendParam()

    data class FriendPairParam(
        val user: OsuUser,
        val partner: OsuUser,
        val following: Boolean,
        val followed: Boolean? = null,
    ) : FriendParam()

    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: MessageService.DataValue<FriendParam>
    ): Boolean {

        val m = Instruction.FRIEND.matcher(messageText)
        if (!m.find()) {
            return false
        }

        val param = getParam(event, m)
        data.value = param

        return true
    }

    override fun HandleMessage(event: MessageEvent, param: FriendParam) {
        event.reply(getMessageChain(param))
    }

    private fun getMessageChain(param: FriendParam): MessageChain {
        when(param) {
            is FriendPairParam -> {
                return MessageChain(getPairFriendsText(param))
            }

            is FriendListParam -> {
                userApiService.asyncDownloadAvatar(param.friends)
                userApiService.asyncDownloadBackground(param.friends)

                val image = try {
                    val type = param.sortType.name.lowercase()

                    val body = mapOf(
                        "me_card_A1" to param.user,
                        "friend_card_A1" to param.friends,
                        "type" to type,
                    )

                    imageService.getPanel(body, "A1")
                } catch (e: Exception) {
                    log.error("好友列表：渲染失败", e)
                    throw IllegalStateException.Render("好友列表")
                }

                return MessageChain(image)

            }

            else -> throw IllegalStateException.ClassCast("好友")
        }
    }

    /*

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
            bindDao.getBindUser(uid)
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
                .find { it.target.userID == bindUser.userID } != null
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

        val osuUser = param.user ?: userApiService.getOsuUser(bindUser)

        val rawList = try {
            userApiService.getFriendList(bindUser).map { it.target }
        } catch (e: Exception) {
            throw IllegalStateException.Fetch("好友列表")
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
            return MessageChain(image)
        } catch (e: Exception) {
            log.error("Friend: ", e)
            throw FriendException(FriendException.Type.FRIEND_Send_Error)
        }
    }

     */

    /**
     * 重写参数获取方式
     */
    private fun getParam(event: MessageEvent, matcher: Matcher): FriendParam {
        val any: String? = matcher.group("any")

        val me = try {
            bindDao.getBindFromQQ(event.sender.id, isMyself = true)
        } catch (ignored: BindException) {
            null
        }

        if (me == null || !me.isAuthorized) {
            throw FriendException(FriendException.Type.FRIEND_Me_NoPermission)
            // 无权限
        }

        val isMyself = AtomicBoolean(true) // 处理 range
        val mode = getMode(matcher)

        val id = UserIDUtil.getUserIDWithRange(event, matcher, mode, isMyself)

        val conditions = DataUtil.paramMatcher(any, MicroUserFilter.entries.map { it.regex }, REG_RANGE.toRegex())

        // 如果不加井号，则有时候范围会被匹配到这里来
        val rangeInConditions = conditions.lastOrNull()?.firstOrNull()
        val hasRangeInConditions = (rangeInConditions.isNullOrEmpty().not())
        val hasCondition = conditions.dropLast(1).sumOf { it.size } > 0

        if (hasRangeInConditions.not() && hasCondition.not() && any.isNullOrBlank().not()) {
            throw IllegalArgumentException.WrongException.Cabbage()
        }

        val ranges = if (hasRangeInConditions) {
            rangeInConditions
        } else {
            matcher.group(FLAG_RANGE)
        }?.split(REG_HYPHEN.toRegex())

        if (id.data == me.userID) {
            if (event.isAt && event.target == event.sender.id
                || matcher.group(FLAG_UID)?.toLongOrNull() == me.userID) {
                throw TipsException("你自己与你自己就是最好的朋友。")
            }

            // 好友列表模式

            val id2 = if (id.start != null) {
                id
            } else {
                val start = ranges?.firstOrNull()?.toIntOrNull()
                val end = if (ranges?.size == 2) {
                    ranges.last().toIntOrNull()
                } else {
                    null
                }

                CmdRange(id.data!!, start, end)
            }

            val async = AsyncMethodExecutor.awaitPairCallableExecute(
                { userApiService.getOsuUser(me) },
                { userApiService.getFriendList(me) },
            )

            val sortParam: Pair<SortType, SortDirection> =
                getSort(matcher.group("sort"))

            // 排序成绩
            val sortedFriends = sortFriends(async.second.map { it.target }, sortParam.first, sortParam.second)

            // 筛选成绩
            val offset = id2.getOffset()
            val limit = id2.getLimit(100)

            val filteredFriends = MicroUserFilter.filterUsers(sortedFriends, conditions).drop(offset).take(limit)

            if (filteredFriends.isEmpty()) {
                throw FriendException(FriendException.Type.FRIEND_Client_NoMatch)
            }

            return FriendListParam(async.first, filteredFriends, sortParam.first)
        } else {
            // 亲密好友模式
            val other = bindDao.getBindUser(id.data)

            if (other == null) {
                // 对方未绑定模式
                val range = getUserWithRange(event, matcher, mode, isMyself)
                range.setZeroToRange100()

                val range2 = if (range.start != null) {
                    range
                } else {
                    val start = ranges?.firstOrNull()?.toIntOrNull()
                    val end = if (ranges?.size == 2) {
                        ranges.last().toIntOrNull()
                    } else {
                        null
                    }

                    CmdRange(range.data!!, start, end)
                }

                val others = range2.data!!

                val async = AsyncMethodExecutor.awaitPairCallableExecute(
                    { userApiService.getOsuUser(me) },
                    { userApiService.getFriendList(me) }
                )
                
                val target = async.second.find { it.targetID == others.userID }
                
                // 如果 ing 不为空，则必然知道 ed
                val following = target != null
                val followed = target?.isMutual

                return FriendPairParam(async.first, others, following, followed)
            } else {
                // 对方已绑定模式
                val async = AsyncMethodExecutor.awaitQuadSupplierExecute(
                    { userApiService.getOsuUser(me) },
                    { userApiService.getOsuUser(id.data!!, other.mode) },
                    { userApiService.getFriendList(me) },
                    { userApiService.getFriendList(other) },
                )

                val following = async.second.first.find { it.targetID == other.userID } != null

                val followed = async.second.second.find { it.targetID == me.userID } != null

                val users = async.first

                return FriendPairParam(users.first, users.second, following, followed)
            }

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


        /**
         * 重写排序方式
         */
        private fun sortFriends(friends: List<MicroUser>, sortType: SortType, sortDirection: SortDirection): List<MicroUser> {
            val sequence = if (sortDirection == DESCEND) {
                // 先翻一次，因为等会要翻回来，这样可以保证都是默认按名字升序排序的
                friends.asSequence().sortedByDescending { it.userName }
            } else {
                friends.asSequence().sortedBy { it.userName }
            }

            val sorted = when (sortType) {
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
                    val set = sorted.toSet()

                    friends
                        .sortedBy { it.userName }
                        .filter { ! set.contains(it) }
                        .toList()
                }
            }

            if (result.isEmpty()) {
                if (sortType == NULL) {
                    throw FriendException(FriendException.Type.FRIEND_Client_NoFriend)
                } else {
                    throw FriendException(FriendException.Type.FRIEND_Client_NoMatch)
                }
            }

            return result
        }
        
        private fun getPairFriendsText(param: FriendPairParam): String {
            val name = param.partner.username

            return if (param.following) {
                // 此时必然知道 followed
                if (param.followed!!) {
                    "恭喜！你已经与 $name 互相成为好友了。"
                } else {
                    "你已经添加了 $name 作为你的好友，但对方似乎还没有添加你。"
                }
            } else if (param.followed == null) {
                 "你还没有将 $name 添加为你的好友，并且对方没有使用链接绑定，还不知道有没有添加你。"
            } else if (param.followed) {
                "你还没有将 $name 添加为你的好友，但对方似乎已经悄悄添加了你。"
            } else {
                "你们暂未互相成为好友。或许可以考虑一下？"
            }
        }
    }
}
