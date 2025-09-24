package com.now.nowbot.service.messageServiceImpl

import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
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
import kotlin.math.min

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
        val statistics: FriendPairStatistics
    ) : FriendParam()

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
    data class FriendPairStatistics(
        val userBind: Boolean,
        val partnerBind: Boolean,
        val isFollowing: Boolean,
        val isFollowed: Boolean? = null,
        val userFollowing: Int,
        val partnerFollowing: Int,

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

        val param = getParam(event, m)
        data.value = param

        /*
        data.value = FriendPairParam(
            userApiService.getOsuUser(7003013, OsuMode.OSU),
            userApiService.getOsuUser(9794030, OsuMode.OSU),
            statistics = FriendPairStatistics(
                userBind = true,
                partnerBind = true,
                isFollowing = true,
                isFollowed = true,
                userFollowing = 243,
                partnerFollowing = 280,
                )
        )
         */

        return true
    }

    override fun HandleMessage(event: MessageEvent, param: FriendParam) {
        event.reply(getMessageChain(param))
    }

    private fun getMessageChain(param: FriendParam): MessageChain {
        when(param) {
            is FriendPairParam -> {

                val image = try {
                    val stat = param.statistics

                    val body = mapOf(
                        "user" to param.user,
                        "partner" to param.partner,
                        "statistics" to mapOf(
                            "user_bind" to stat.userBind,
                            "partner_bind" to stat.partnerBind,
                            "is_following" to stat.isFollowing,
                            "is_followed" to stat.isFollowed,
                            "user_following" to stat.userFollowing,
                            "partner_following" to stat.partnerFollowing,
                        ),
                    )

                    imageService.getPanel(body, "U")
                } catch (e: Exception) {
                    log.error("好友列表：渲染失败", e)
                    return MessageChain(getPairFriendsText(param))
                }

                return MessageChain(image)
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
            val limit = min(id2.getLimit(20), 100)

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
                val others = getUserWithRange(event, matcher, mode, isMyself).data!!

                val async = AsyncMethodExecutor.awaitPairCallableExecute(
                    { userApiService.getOsuUser(me) },
                    { userApiService.getFriendList(me) }
                )
                
                val target = async.second.find { it.targetID == others.userID }
                
                // 如果 ing 不为空，则必然知道 ed
                val following = target != null
                val followed = target?.isMutual

                return FriendPairParam(async.first, others, FriendPairStatistics(
                    userBind = true,
                    partnerBind = false,
                    isFollowing = following,
                    isFollowed = followed,
                    userFollowing = async.second.size,
                    partnerFollowing = -1
                ))
            } else {
                // 对方已绑定模式
                val async = AsyncMethodExecutor.awaitQuadSupplierExecute(
                    { userApiService.getOsuUser(me) },
                    { userApiService.getOsuUser(id.data!!, other.mode) },
                    { userApiService.getFriendList(me) },
                    { userApiService.getFriendList(other) },
                )

                val users = async.first
                val friends = async.second

                val following = friends.first.find { it.targetID == other.userID } != null

                val followed = friends.second.find { it.targetID == me.userID } != null

                return FriendPairParam(users.first, users.second, FriendPairStatistics(
                    userBind = true,
                    partnerBind = true,
                    isFollowing = following,
                    isFollowed = followed,
                    userFollowing = friends.first.size,
                    partnerFollowing = friends.second.size
                ))
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
            val stat = param.statistics

            return if (stat.isFollowing) {
                // 此时必然知道 followed
                if (stat.isFollowed!!) {
                    "恭喜！你已经与 $name 互相成为好友了。"
                } else {
                    "你已经添加了 $name 作为你的好友，但对方似乎还没有添加你。"
                }
            } else if (stat.isFollowed == null) {
                 "你还没有将 $name 添加为你的好友，并且对方没有使用链接绑定，还不知道有没有添加你。"
            } else if (stat.isFollowed) {
                "你还没有将 $name 添加为你的好友，但对方似乎已经悄悄添加了你。"
            } else {
                "你们暂未互相成为好友。或许可以考虑一下？"
            }
        }
    }
}
