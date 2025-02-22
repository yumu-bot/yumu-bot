package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.json.OsuUser
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.osuApiService.OsuUserApiService
import com.now.nowbot.throwable.GeneralTipsException
import com.now.nowbot.util.CmdObject
import com.now.nowbot.util.CmdUtil
import com.now.nowbot.util.Instruction
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.exp
import kotlin.math.floor

@Service("TAKE")
class TakeService(
private val userApiService: OsuUserApiService,
) : MessageService<TakeService.TakeParam> {
    data class TakeParam(val user: OsuUser, val isMyself: Boolean = false)

    override fun isHandle(event: MessageEvent, messageText: String, data: MessageService.DataValue<TakeParam>): Boolean {

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

        val pc = (micro?.rulesets?.osu?.playCount ?: 0L) + (micro?.rulesets?.taiko?.playCount ?: 0L) + (micro?.rulesets?.fruits?.playCount ?: 0L) + (micro?.rulesets?.mania?.playCount ?: 0L)

        val plus = if (pc == 0L) {
            180L
        } else {
            floor(1580.0 * (1.0 - exp((-pc) / 5900.0)) + 180.0 + (8.0 * pc / 5900.0)).toLong()
        }

        val take = user.lastVisit.plusDays(plus)

        val takeHours = ChronoUnit.HOURS.between(OffsetDateTime.now(), take)
        val takeDays = ChronoUnit.DAYS.between(OffsetDateTime.now(), take)
        val takeMonths = ChronoUnit.MONTHS.between(OffsetDateTime.now(), take)
        val takeYears = ChronoUnit.YEARS.between(OffsetDateTime.now(), take)

        val takeTime = if (takeYears > 0L) {
            "$takeYears 年后"
        } else if (takeMonths > 0L) {
            "$takeMonths 个月后"
        } else if (takeDays > 0L) {
            "$takeDays 天后"
        } else if (takeHours > 0L) {
            "$takeHours 小时后"
        }  else {
            "不久后"
        }

        val visitHours = ChronoUnit.HOURS.between(user.lastVisit, OffsetDateTime.now())
        val visitDays = ChronoUnit.DAYS.between(user.lastVisit, OffsetDateTime.now())
        val visitMonths = ChronoUnit.MONTHS.between(user.lastVisit, OffsetDateTime.now())
        val visitYears = ChronoUnit.YEARS.between(user.lastVisit, OffsetDateTime.now())

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

        if (param.isMyself) {
            event.reply("""
            别人可以占据你的玩家名。
            你上次在线的时间：${user.lastVisit.format(formatter)}（${visitTime}）
            玩家的游戏次数：${pc}
            你的玩家名可被占用的时间：${take.format(formatter)}（${takeTime}）
            """.trimIndent())
        } else {
            event.reply("""
            您可以占据玩家 $name 的玩家名。
            玩家 $name 上次在线的时间：${user.lastVisit.format(formatter)}（${visitTime}）
            玩家的游戏次数：${pc}
            玩家名可用的时间：${take.format(formatter)}（${takeTime}）
            """.trimIndent())
        }
    }

    companion object {
        private val formatter= DateTimeFormatterBuilder()
            .appendPattern("YYYY/MM/dd")
            .toFormatter()
    }
}