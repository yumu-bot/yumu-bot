package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.dao.BindDao
import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.osu.OsuUser
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.osuApiService.OsuUserApiService
import com.now.nowbot.throwable.TipsException
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.InstructionUtil
import com.now.nowbot.util.UserIDUtil
import org.springframework.stereotype.Service
import java.util.regex.Matcher

@Service("QUICK_PLAY_INFO")
class QuickplayInfoService(
    private val userApiService: OsuUserApiService,
    private val bindDao: BindDao
): MessageService<QuickplayInfoService.QuickplayInfoParam> {
    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: MessageService.DataValue<QuickplayInfoParam>
    ): Boolean {
        val m = Instruction.QUICK_PLAY_INFO.matcher(messageText)
        if (!m.find()) {
            return false
        }

        data.value = getParam(event, m)
        return true
    }

    override fun handleMessage(
        event: MessageEvent,
        param: QuickplayInfoParam
    ): ServiceCallStatistic? {
        event.reply(param.getTextMessage())

        return ServiceCallStatistic.building(event)
    }

    private fun getParam(event: MessageEvent, matcher: Matcher): QuickplayInfoParam {
        val mode = InstructionUtil.getMode(matcher, bindDao.getGroupModeConfig(event))
        val id = UserIDUtil.getUserIDWithoutRange(event, matcher, mode)

        val user: OsuUser = if (id != null) {
            userApiService.getOsuUser(id, mode.data ?: OsuMode.DEFAULT)
        } else {
            InstructionUtil.getUserWithoutRange(event, matcher, mode)
        }

        return QuickplayInfoParam(user)
    }

    private fun QuickplayInfoParam.getTextMessage(): String {
        val mm = user.matchmakingStats.sortedByDescending { it.poolID }

        if (mm.isEmpty()) {
            throw TipsException("没有找到您的排位信息。")
        }

        return mm.joinToString("\n---\n") { m ->
            val p = m.pool

            val active = if (p.active) "当前赛季" else "历史赛季"
            val provisional = if (m.provisional) "(临时)" else ""
            val winRate = m.firstPlacements * 100.0 / m.plays.coerceAtLeast(1)
            val variant = if (p.variantID > 0) {
               " ${p.variantID}K"
            } else ""

            """
                ${p.name}: $active $provisional (${OsuMode.getMode(p.rulesetID).fullName}${variant})
                排名：#${m.rank}
                段位分：${m.rating}
                胜场：${m.firstPlacements} / ${m.plays} (${"%.0f".format(winRate)}%)
                积分：${m.totalPoints}
            """.trimIndent()
        }

    }

    data class QuickplayInfoParam(
        val user: OsuUser,
    )
}