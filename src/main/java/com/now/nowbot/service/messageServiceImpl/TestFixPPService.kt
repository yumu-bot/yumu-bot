package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.config.Permission
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.json.LazerScore
import com.now.nowbot.model.json.OsuUser
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.service.osuApiService.OsuCalculateApiService
import com.now.nowbot.service.osuApiService.OsuScoreApiService
import com.now.nowbot.service.osuApiService.OsuUserApiService
import com.now.nowbot.throwable.GeneralTipsException
import com.now.nowbot.util.DataUtil.splitString
import com.now.nowbot.util.Instruction
import org.springframework.stereotype.Service
import java.util.regex.Matcher
import kotlin.math.floor
import kotlin.math.round

@Service("TEST_FIX") class TestFixPPService(
    private val userApiService: OsuUserApiService,
    private val beatmapApiService: OsuBeatmapApiService,
    private val scoreApiService: OsuScoreApiService,
    private val calculateApiService: OsuCalculateApiService
) : MessageService<Matcher> {

    @Throws(Throwable::class) override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: DataValue<Matcher>
    ): Boolean {
        val m = Instruction.TEST_FIX.matcher(messageText)
        if (m.find()) {
            data.value = m
            return true
        } else return false
    }

    @Throws(Throwable::class) override fun HandleMessage(event: MessageEvent, matcher: Matcher) {
        if (Permission.isCommonUser(event)) {
            throw GeneralTipsException(GeneralTipsException.Type.G_Permission_Group)
        }

        val names: List<String> = splitString(matcher.group("data"))
            ?: throw GeneralTipsException(GeneralTipsException.Type.G_Null_UserName)
        var mode = OsuMode.getMode(matcher.group("mode"))

        if (names.isEmpty()) throw GeneralTipsException(GeneralTipsException.Type.G_Fetch_List)

        val sb = StringBuilder()

        for (name in names) {
            if (name.isBlank()) {
                continue
            }

            val user: OsuUser
            val bests: List<LazerScore>
            val playerPP: Double

            try {
                user = userApiService.getOsuUser(name)
                playerPP = user.pp

                if (mode == OsuMode.DEFAULT) {
                    mode = user.currentOsuMode
                }

                bests = scoreApiService.getBestScores(user.userID, mode, 0, 100) + scoreApiService.getBestScores(user.userID, mode, 100, 100)
            } catch (e: Exception) {
                sb.append("name=").append(name).append(" not found").append('\n')
                continue
            }

            if (bests.isEmpty()) {
                sb.append("name=").append(name).append(" bp is empty").append('\n')
            }

            for (s in bests) {
                beatmapApiService.applyBeatMapExtendFromDataBase(s)

                val max = s.beatMap.maxCombo ?: 1
                val combo = s.maxCombo

                val miss = s.statistics.miss

                // 断连击，mania 模式不参与此项筛选
                val isChoke = (miss == 0) && (combo < round(max * 0.98f)) && (s.mode != OsuMode.MANIA)

                // 含有 <1% 的失误
                val has1pMiss = (miss > 0) && ((1f * miss / max) <= 0.01f)

                // 并列关系，miss 不一定 choke（断尾不会计入 choke），choke 不一定 miss（断滑条
                if (isChoke || has1pMiss) {
                    s.PP = calculateApiService.getScoreFullComboPP(s).pp
                }
            }

            val bpPP = bests.sumOf { it.weight?.PP ?: 0.0 }
            val fixed = bests.sortedByDescending{ it.PP ?: 0.0 }

            var weight = 1.0 / 0.95

            for (f in fixed) {
                weight *= 0.95
                f.weight = LazerScore.Weight(weight, (f.PP ?: 0.0) * weight)
            }

            val fixedPP = fixed.sumOf { it.weight?.PP ?: 0.0 }

            val resultPP = playerPP - bpPP + fixedPP
            sb.append(floor(resultPP).toInt()).append(',').append(' ')
        }

        event.reply(sb.toString().removeSuffix(","))
    }
}
