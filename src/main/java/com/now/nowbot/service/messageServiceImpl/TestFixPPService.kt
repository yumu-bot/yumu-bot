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
import java.util.*
import java.util.regex.Matcher

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

        val names: List<String> =
            splitString(matcher.group("data")) ?: throw GeneralTipsException(GeneralTipsException.Type.G_Null_UserName)
        var mode = OsuMode.getMode(matcher.group("mode"))

        if (names.isEmpty()) throw GeneralTipsException(GeneralTipsException.Type.G_Fetch_List)

        val sb = StringBuilder()

        for (name in names) {
            if (name.isBlank()) {
                break
            }

            var user: OsuUser
            var bps: List<LazerScore>
            var playerPP: Double

            try {
                user = userApiService.getPlayerInfo(name)
                playerPP = user.pp ?: 0.0

                if (mode == OsuMode.DEFAULT) {
                    mode = user.currentOsuMode
                }

                bps = scoreApiService.getBestScores(user.userID, mode)
            } catch (e: Exception) {
                sb.append("name=").append(name).append(" not found").append('\n')
                break
            }

            if (bps.isEmpty()) {
                sb.append("name=").append(name).append(" bp is empty").append('\n')
            }

            var fixed: MutableList<LazerScore> = ArrayList(bps.size)

            var bpPP = 0f

            for (bp in bps) {
                beatmapApiService.applyBeatMapExtendFromDataBase(bp)

                val max = bp.totalHit
                val combo = bp.maxCombo

                val miss = bp.statistics.miss ?: 0

                // 断连击，mania 模式不参与此项筛选
                val isChoke = (miss == 0) && (combo < Math.round(max * 0.98f)) && (bp.mode != OsuMode.MANIA)

                // 含有 <1% 的失误
                val has1pMiss = (miss > 0) && ((1f * miss / max) <= 0.01f)

                // 并列关系，miss 不一定 choke（断尾不会计入 choke），choke 不一定 miss（断滑条
                if (isChoke || has1pMiss) {
                    bp.PP = calculateApiService.getScoreFullComboPP(bp).pp
                }

                fixed.add(bp)
                bpPP += Objects.requireNonNull<LazerScore.Weight?>(bp.weight).PP.toFloat()
            }

            fixed = fixed.stream().filter { score: LazerScore -> score.PP != null }
                .sorted(Comparator.comparing<LazerScore, Double> { it.PP ?: 0.0 }.reversed()).toList()

            var weight = 1.0 / 0.95

            for (f in fixed) {
                weight *= 0.95
                f.weight = LazerScore.Weight(weight, (f.PP ?: 0.0) * weight)
            }

            val fixedPP =
                fixed.stream()
                    .mapToDouble { s: LazerScore -> (s.weight?.PP ?: 0.0) }
                    .reduce { a: Double, b: Double ->
                    java.lang.Double.sum(
                        a, b
                    )
                }.orElse(0.0)

            val resultPP = playerPP - bpPP + fixedPP
            sb.append(Math.round(resultPP)).append(',').append(' ')
        }

        event.reply(sb.substring(0, sb.length - 2))
    }
}
