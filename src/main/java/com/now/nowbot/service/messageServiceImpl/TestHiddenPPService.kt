package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.config.Permission
import com.now.nowbot.model.osu.LazerMod
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.osu.LazerScore
import com.now.nowbot.model.osu.OsuUser
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.osuApiService.OsuScoreApiService
import com.now.nowbot.service.osuApiService.OsuUserApiService

import com.now.nowbot.throwable.botRuntimeException.IllegalStateException
import com.now.nowbot.throwable.botRuntimeException.PermissionException
import com.now.nowbot.util.DataUtil.splitString
import com.now.nowbot.util.Instruction
import org.springframework.stereotype.Service
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
    override fun HandleMessage(event: MessageEvent, param: Matcher) {
        if (Permission.isCommonUser(event)) {
            throw PermissionException.DeniedException.BelowGroupAdministrator()
        }

        val names: List<String>? = splitString(param.group("data"))
        var mode = OsuMode.getMode(param.group("mode"))

        if (names.isNullOrEmpty())
            throw throw IllegalStateException.Fetch("玩家名")

        val sb = StringBuilder()

        for (name in names) {
            if (name.isBlank()) {
                break
            }

            var user: OsuUser
            var bps: List<LazerScore?>
            var hiddenPP = 0.0

            try {
                user = userApiService.getOsuUser(name)

                if (mode == OsuMode.DEFAULT) {
                    mode = user.currentOsuMode
                }

                bps = scoreApiService.getBestScores(user, mode)
            } catch (e: Exception) {
                sb.append("name=").append(name).append(" not found").append('\n')
                break
            }

            if (bps.isEmpty()) {
                sb.append("name=").append(name).append(" bp is empty").append('\n')
            }

            for (bp in bps) {
                if (LazerMod.hasMod(bp.mods, LazerMod.Hidden)) {
                    hiddenPP += (bp.weight?.pp ?: 0.0)
                }
            }

            sb.append(Math.round(hiddenPP)).append(',').append(' ')
        }

        event.reply(sb.substring(0, sb.length - 2))
    }
}
