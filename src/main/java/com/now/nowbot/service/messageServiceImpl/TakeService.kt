package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.dao.BindDao
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.osu.BeatmapsetSearch
import com.now.nowbot.model.osu.OsuUser
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.service.osuApiService.OsuUserApiService
import com.now.nowbot.throwable.botRuntimeException.NoSuchElementException
import com.now.nowbot.util.*
import com.now.nowbot.util.command.FLAG_NAME
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.regex.Matcher
import kotlin.math.exp
import kotlin.math.floor

@Service("TAKE") class TakeService(
    private val bindDao: BindDao,
    private val userApiService: OsuUserApiService,
    private val beatmapApiService: OsuBeatmapApiService,
) : MessageService<TakeService.TakeParam> {

    data class TakeParam(
        val user: OsuUser?,
        val name: String = "",
        val isMyself: Boolean = false, // 是本人吗？
        val isPrevious: Boolean = false, // 曾用名
        val hostedCount: Int = 0, // 有这个名字的 上架谱面
        val hasBadge: Boolean = false, // 有牌子
    )

    override fun isHandle(
        event: MessageEvent, messageText: String, data: MessageService.DataValue<TakeParam>
    ): Boolean {
        val matcher = Instruction.TAKE.matcher(messageText)
        if (!matcher.find()) {
            return false
        }

        data.value = getParam(event, matcher)
        return true
    }

    override fun HandleMessage(event: MessageEvent, param: TakeParam) {
        val name = if (param.name.isEmpty()) {
            "此玩家"
        } else {
            "玩家 ${param.name} "
        }

        val type = if (param.isPrevious) {
            "曾用"
        } else {
            "玩家"
        }

        // 提前跳出
        if (param.hasBadge) {
            if (param.hostedCount > 0) {
                if (param.isMyself) {
                    event.reply("别人不能占据你的${type}名，因为你已经拥有上架 (ranked) 谱面，并且已经获取主页奖牌 (badges)。")
                } else {
                    event.reply("你不能占据${name}的${type}名，因为对方已经拥有上架 (ranked) 谱面，并且已经获取主页奖牌 (badges)。")
                }
            } else {
                if (param.isMyself) {
                    event.reply("别人不能占据你的${type}名，因为你已经获取主页奖牌 (badges)。")
                } else {
                    event.reply("你不能占据${name}的${type}名，因为对方已经获取主页奖牌 (badges)。")
                }
            }

            return
        } else if (param.hostedCount > 0) {
            if (param.isMyself) {
                event.reply("别人不能占据你的${type}名，因为你已经拥有上架 (ranked) 谱面。")
            } else {
                event.reply("你不能占据${name}的${type}名，因为对方已经拥有上架 (ranked) 谱面。")
            }

            return
        } else { // 进入下一轮
        }

        val user = param.user ?: throw NoSuchElementException.TakePlayer(name)

        val micro = try {
            userApiService.getUsers(listOf(user.userID), isVariant = true).first()
        } catch (e: Exception) {
            throw NoSuchElementException.TakePlayer(user.username)
        }

        val pc = (micro.rulesets?.osu?.playCount ?: 0L) + (micro.rulesets?.taiko?.playCount
            ?: 0L) + (micro.rulesets?.fruits?.playCount ?: 0L) + (micro.rulesets?.mania?.playCount ?: 0L)

        val plus = if (pc == 0L) {
            180L
        } else {
            floor(1580.0 * (1.0 - exp((-pc) / 5900.0)) + 180.0 + (8.0 * pc / 5900.0)).toLong()
        }

        val isShownOffline = user.lastVisit == null

        val visit = if (isShownOffline) {
            val rulesets = listOf(OsuMode.OSU, OsuMode.TAIKO, OsuMode.CATCH, OsuMode.MANIA)

            val actions = rulesets.map {
                return@map AsyncMethodExecutor.Supplier {
                    val lastMonth = userApiService.getOsuUser(user.userID, it).monthlyPlaycounts.lastOrNull()?.startDate
                        ?: OffsetDateTime.MIN.format(formatter2)

                    return@Supplier it.modeValue.toInt() to LocalDate.parse(lastMonth, formatter2).atTime(0, 0)
                        .atOffset(ZoneOffset.UTC)
                }
            }

            val result = AsyncMethodExecutor.awaitSupplierExecute(actions).toMap()

            val most = result.maxByOrNull { it.value.toInstant().toEpochMilli() }?.value
                ?: throw NoSuchElementException.PlayerPlay(user.username)

            if (most.format(formatter2) == OffsetDateTime.MIN.format(formatter2)) {
                throw NoSuchElementException.PlayerPlay(user.username)
            }

            most
        } else {
            user.lastVisit!!
        }

        val take = visit.plusDays(plus)
        val isImmediately = take.isBefore(OffsetDateTime.now())

        val takeHours = ChronoUnit.HOURS.between(OffsetDateTime.now(), take)
        val takeDays = ChronoUnit.DAYS.between(OffsetDateTime.now(), take)
        val takeMonths = ChronoUnit.MONTHS.between(OffsetDateTime.now(), take)
        val takeYears = ChronoUnit.YEARS.between(OffsetDateTime.now(), take)

        val takeUnit = if (isImmediately) "前" else "后"

        val takeTime = if (takeYears > 0L) {
            "$takeYears 年$takeUnit"
        } else if (takeMonths > 0L) {
            "$takeMonths 个月$takeUnit"
        } else if (takeDays > 0L) {
            "$takeDays 天$takeUnit"
        } else if (takeHours > 0L) {
            "$takeHours 小时$takeUnit"
        } else {
            "不久$takeUnit"
        }

        val visitHours = ChronoUnit.HOURS.between(visit, OffsetDateTime.now())
        val visitDays = ChronoUnit.DAYS.between(visit, OffsetDateTime.now())
        val visitMonths = ChronoUnit.MONTHS.between(visit, OffsetDateTime.now())
        val visitYears = ChronoUnit.YEARS.between(visit, OffsetDateTime.now())

        val visitTime = if (visitYears > 0L) {
            "$visitYears 年前"
        } else if (visitMonths > 0L) {
            "$visitMonths 个月前"
        } else if (visitDays > 0L) {
            "$visitDays 天前"
        } else if (visitHours > 0L) {
            "$visitHours 小时前"
        } else {
            "不久前"
        }

        val lastVisitFormat = if (isShownOffline) "保密" else visit.format(formatter)
        val visitTimeFormat = if (isShownOffline) "未知" else visitTime
        val takeTimeFormat = take.format(formatter)
        val soon = if (takeYears >= 4) {
            "在很久之后"
        } else if (takeYears >= 1) {
            "在一段时间之后"
        } else {
            "即将"
        }

        val username = user.username

        // 曾用名立刻可以使用
        if (isImmediately || param.isPrevious) {
            if (param.isMyself) {
                val tips = if (param.isPrevious) {
                    "你不会受任何影响。"
                } else {
                    "之后你会变成 ${username}_old。赶快回坑开一把！"
                }

                event.reply(
                    """
                    别人现在就可以使用你的${type}名：${username}。${tips}
                    你上次在线的时间：${lastVisitFormat}（${visitTimeFormat}）
                    你的游戏次数：${pc}
                    你的玩家名可被占用的时间：${takeTimeFormat}
                    """.trimIndent()
                )
            } else {
                event.reply(
                    """
                    你现在就可以使用${name}的${type}名。
                    玩家上次在线时间：${lastVisitFormat}（${visitTimeFormat}）
                    玩家的游戏次数：${pc}
                    玩家名可被占用的时间：${takeTimeFormat}
                    """.trimIndent()
                )
            }
            return
        }

        if (param.isMyself) {
            event.reply("""
                别人${soon}可以占据你的${type}名：${username}。
                你上次在线的时间：${lastVisitFormat}（${visitTimeFormat}）
                你的游戏次数：${pc}
                你的玩家名可被占用的时间：${takeTimeFormat}（${takeTime}）
                """.trimIndent()
            )
        } else {
            event.reply("""
                您${soon}可以占据对方的${type}名：${username}。
                玩家上次在线时间：${lastVisitFormat}（${visitTimeFormat}）
                玩家的游戏次数：${pc}
                玩家名可被占用的时间：${takeTimeFormat}（${takeTime}）
                """.trimIndent()
            )
        }
    }

    private fun getParam(event: MessageEvent, matcher: Matcher): TakeParam {
        val mode = CmdUtil.getMode(matcher)
        val id = UserIDUtil.getUserIDWithoutRange(event, matcher, mode)

        val bindUser = bindDao.getBindUser(event.sender.id)
        val nameStr = (matcher.group(FLAG_NAME) ?: "").trim()

        val user: OsuUser?
        val search: BeatmapsetSearch

        // 高效的获取方式
        if (id != null) { // 构建谱面查询

            val query = mapOf(
                "q" to "creator=" + nameStr.ifEmpty { id }, "sort" to "ranked_desc", "page" to 1
            )

            val async = AsyncMethodExecutor.awaitPairCallableExecute(
                {
                    try {
                        userApiService.getOsuUser(id)
                    } catch (e: Exception) {
                        return@awaitPairCallableExecute null
                    }
                },
                { beatmapApiService.searchBeatmapset(query) },
            )

            user = async.first
            search = async.second

        } else { // 经典？的获取方式

            user = try {
                CmdUtil.getUserWithoutRange(event, matcher, CmdObject(OsuMode.DEFAULT))
            } catch (_: Exception) {
                null
            }

            val query = mapOf(
                "q" to "creator=" + nameStr.ifEmpty { user?.userID ?: 0L }, "sort" to "ranked_desc", "page" to 1
            )

            search = beatmapApiService.searchBeatmapset(query)
        }

        val name = nameStr.ifEmpty { user?.username } ?: ""
        val isMyself = user?.userID == bindUser?.userID
        val isPrevious =
            user?.previousNames?.map { prev -> DataUtil.getStringSimilarity(name, prev) > 0.8 }?.contains(true) ?: false

        val hostedCount = search.beatmapsets.count { set ->
            (set.beatmapsetID != user?.userID) && (set.beatmaps?.all { that -> that.beatmapID != user?.userID } ?: true)
        }

        val hasBadge = user?.badges?.isNotEmpty() ?: false

        return TakeParam(user, name, isMyself, isPrevious, hostedCount, hasBadge)
    }

    companion object {
        private val formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd")
        private val formatter2 = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    }
}