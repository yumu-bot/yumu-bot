package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.config.Permission
import com.now.nowbot.model.LazerMod
import com.now.nowbot.model.enums.LazerModType
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.json.LazerScore
import com.now.nowbot.model.json.OsuUser
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.osuApiService.OsuScoreApiService
import com.now.nowbot.service.osuApiService.OsuUserApiService
import com.now.nowbot.throwable.GeneralTipsException
import com.now.nowbot.util.DataUtil.splitString
import com.now.nowbot.util.Instruction
import org.springframework.stereotype.Service
import org.springframework.util.CollectionUtils
import org.springframework.util.StringUtils
import java.util.regex.Matcher

@Service("TEST_HD")
class TestHiddenPPService(
    private val userApiService: OsuUserApiService,
    private val scoreApiService: OsuScoreApiService,
) : MessageService<Matcher> {

    @Throws(Throwable::class)
    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: DataValue<Matcher>,
    ): Boolean {
        val m = Instruction.TEST_HD.matcher(messageText)
        if (m.find()) {
            data.value = m
            return true
        } else return false
    }

    @Throws(Throwable::class)
    override fun HandleMessage(event: MessageEvent, matcher: Matcher) {
        if (Permission.isCommonUser(event)) {
            throw GeneralTipsException(GeneralTipsException.Type.G_Permission_Group)
        }

        val names: List<String?>? = splitString(matcher.group("data"))
        var mode = OsuMode.getMode(matcher.group("mode"))

        if (CollectionUtils.isEmpty(names))
            throw GeneralTipsException(GeneralTipsException.Type.G_Fetch_List)

        val sb = StringBuilder()

        for (name in names!!) {
            if (!StringUtils.hasText(name)) {
                break
            }

            var user: OsuUser
            var bps: List<LazerScore?>
            var hiddenPP = 0.0

            try {
                val id = userApiService.getOsuId(name)
                user = userApiService.getPlayerOsuInfo(id)

                if (mode == OsuMode.DEFAULT) {
                    mode = user.currentOsuMode
                }

                bps = scoreApiService.getBestScores(id, mode, 0, 100)
            } catch (e: Exception) {
                sb.append("name=").append(name).append(" not found").append('\n')
                break
            }

            if (CollectionUtils.isEmpty(bps)) {
                sb.append("name=").append(name).append(" bp is empty").append('\n')
            }

            for (bp in bps) {
                if (LazerMod.hasMod(bp.mods, LazerModType.Hidden)) {
                    hiddenPP += (bp.weight?.PP ?: 0.0)
                }
            }

            sb.append(Math.round(hiddenPP)).append(',').append(' ')
        }

        event.reply(sb.substring(0, sb.length - 2))
    }
}
