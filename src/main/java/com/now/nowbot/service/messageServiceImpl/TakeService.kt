package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.json.OsuUser
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.osuApiService.OsuUserApiService
import com.now.nowbot.throwable.GeneralTipsException
import com.now.nowbot.util.AsyncMethodExecutor
import com.now.nowbot.util.CmdObject
import com.now.nowbot.util.CmdUtil
import com.now.nowbot.util.Instruction
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.exp
import kotlin.math.floor

@Service("TAKE") class TakeService(
    private val userApiService: OsuUserApiService,
) : MessageService<TakeService.TakeParam> {
    data class TakeParam(val user: OsuUser, val isMyself: Boolean = false)

    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: MessageService.DataValue<TakeParam>
    ): Boolean {

        val m = Instruction.TAKE.matcher(messageText)
        if (!m.find()) {
            return false
        }

        val isMyself = AtomicBoolean()

        val user = try {
            CmdUtil.getUserWithoutRange(event, m, CmdObject(OsuMode.DEFAULT), isMyself)
        } catch (e: Exception) {
            throw GeneralTipsException(GeneralTipsException.Type.G_Null_PlayerUnknown)
        }

        data.value = TakeParam(user, isMyself.get())
        return true
    }

    override fun HandleMessage(event: MessageEvent, param: TakeParam) {
        val user = param.user
        val name = user.username

        // 提前跳出
        if (user.badges?.isNotEmpty() == true) {
            if (user.rankedCount > 0) {
                if (param.isMyself) {
                    event.reply("别人不能占据你的玩家名，因为你已经拥有上架 (ranked) 谱面，并且已经获取主页奖牌 (badges)。")
                } else {
                    event.reply("你不能占据玩家 $name 的玩家名，因为 ta 已经拥有上架 (ranked) 谱面，并且已经获取主页奖牌 (badges)。")
                }
            } else {
                if (param.isMyself) {
                    event.reply("别人不能占据你的玩家名，因为你已经获取主页奖牌 (badges)。")
                } else {
                    event.reply("你不能占据玩家 $name 的玩家名，因为 ta 已经获取主页奖牌 (badges)。")
                }
            }
            return
        } else if (user.rankedCount > 0) {
            if (param.isMyself) {
                event.reply("别人不能占据你的玩家名，因为你已经拥有上架 (ranked) 谱面。")
            } else {
                event.reply("你不能占据玩家 $name 的玩家名，因为 ta 已经拥有上架 (ranked) 谱面。")
            }
            return
        }

        val micro = try {
            userApiService.getUsers(listOf(user.userID), true).first()
        } catch (e: Exception) {
            throw GeneralTipsException(GeneralTipsException.Type.G_Null_Player, user.username)
        }

        val pc = (micro?.rulesets?.osu?.playCount ?: 0L) + (micro?.rulesets?.taiko?.playCount
            ?: 0L) + (micro?.rulesets?.fruits?.playCount ?: 0L) + (micro?.rulesets?.mania?.playCount ?: 0L)

        val plus = if (pc == 0L) {
            180L
        } else {
            floor(1580.0 * (1.0 - exp((-pc) / 5900.0)) + 180.0 + (8.0 * pc / 5900.0)).toLong()
        }

        val isShownOffline = user.lastVisit == null

        val visit = if (isShownOffline) {
            val rulesets = listOf(OsuMode.OSU, OsuMode.TAIKO, OsuMode.CATCH, OsuMode.MANIA)

            val actions = rulesets.map {
                return@map AsyncMethodExecutor.Supplier<Pair<Int, OffsetDateTime>> {
                    val lastMonth =
                        userApiService.getPlayerInfo(user.userID, it).monthlyPlaycounts.lastOrNull()?.start_date
                            ?: OffsetDateTime.MIN.format(formatter2)

                    return@Supplier it.modeValue.toInt() to
                            LocalDate.parse(lastMonth, formatter2).atTime(0, 0).atOffset(ZoneOffset.UTC)
                }
            }

            val result = AsyncMethodExecutor.AsyncSupplier(actions)
                .filterNotNull().toMap()

            val most = result.maxByOrNull { it.value.toInstant().toEpochMilli() }?.value ?: throw GeneralTipsException(
                GeneralTipsException.Type.G_Null_Play
            )

            if (most.format(formatter2) == OffsetDateTime.MIN.format(formatter2)) {
                throw GeneralTipsException(GeneralTipsException.Type.G_Null_Play)
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

        if (isImmediately) {
            if (param.isMyself) {
                event.reply(
                    """
                    别人现在就可以变成 $name，之后你会变成 ${name}_old。赶快回坑开一把！
                    你上次在线的时间：${lastVisitFormat}（${visitTimeFormat}）
                    玩家的游戏次数：${pc}
                    你的玩家名可被占用的时间：${takeTimeFormat}
                    """.trimIndent()
                )
            } else {
                event.reply("""
                    您现在就可以变成玩家 $name。
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
                    别人${soon}可以占据你的玩家名。
                    你上次在线的时间：${lastVisitFormat}（${visitTimeFormat}）
                    玩家的游戏次数：${pc}
                    你的玩家名可被占用的时间：${takeTimeFormat}（${takeTime}）
                    """.trimIndent()
            )
        } else {
            event.reply("""
                    您${soon}可以占据玩家 $name 的玩家名。
                    玩家上次在线时间：${lastVisitFormat}（${visitTimeFormat}）
                    玩家的游戏次数：${pc}
                    玩家名可被占用的时间：${takeTimeFormat}（${takeTime}）
                    """.trimIndent()
            )
        }
    }

    companion object {
        private val formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd")
        private val formatter2 = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    }
}