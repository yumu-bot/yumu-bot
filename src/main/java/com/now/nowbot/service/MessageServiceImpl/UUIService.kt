package com.now.nowbot.service.MessageServiceImpl

import com.now.nowbot.model.JsonData.OsuUser
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.MessageServiceImpl.UUIService.UUIParam
import com.now.nowbot.throwable.TipsException
import com.now.nowbot.util.CmdUtil.getMode
import com.now.nowbot.util.CmdUtil.getUserWithOutRange
import com.now.nowbot.util.Instruction
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.util.*

@Service("UU_INFO")
class UUIService(private val template: RestTemplate) : MessageService<UUIParam> {

    @JvmRecord data class UUIParam(val user: OsuUser, val mode: OsuMode?)

    @Throws(TipsException::class)
    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: DataValue<UUIParam>,
    ): Boolean {
        val m = Instruction.UU_INFO.matcher(messageText)
        if (!m.find()) {
            return false
        }
        val mode = getMode(m)
        val user = getUserWithOutRange(event, m, mode)
        data.value = UUIParam(user, mode.data)
        return true
    }

    @Throws(Throwable::class)
    override fun HandleMessage(event: MessageEvent, data: UUIParam) {
        val user = data.user
        val mode = data.mode

        val httpEntity = HttpEntity.EMPTY as HttpEntity<Array<Byte>>

        val avatar: ByteArray? =
            template
                .exchange<ByteArray>(
                    "https://a.ppy.sh/${user.userID}",
                    HttpMethod.GET,
                    httpEntity,
                    ByteArray::class.java,
                )
                .body

        val message = getText(user, mode)
        try {
            event.reply(avatar, message)
        } catch (e: Exception) {
            log.error("UUI 数据发送失败", e)
            event.reply("UUI 请求超时。\n请重试。或使用增强的 !yminfo。")
        }
    }

    // 这是 v0.1.0 的 ymi 文字版本，移到这里
    private fun getText(user: OsuUser, mode: OsuMode?): String {
        val sb = StringBuilder()
        // Muziyami(osu):10086PP
        sb.append(user.username)
            .append(' ')
            .append('(')
            .append(mode)
            .append(')')
            .append(':')
            .append(' ')
            .append(Math.round(user.pp))
            .append("PP")
            .append('\n')
        // #114514 CN#1919 (LV.100(32%))
        sb.append('#')
            .append(user.globalRank)
            .append(' ')
            .append(user.country.code)
            .append('#')
            .append(user.countryRank)
            .append(' ')
            .append("(LV.")
            .append(user.levelCurrent)
            .append('(')
            .append(user.levelProgress)
            .append("%))")
            .append('\n')
        // PC: 2.01w TTH: 743.52w
        sb.append("PC: ")
        val PC = user.playCount
        if (PC > 10000) {
            sb.append(Math.round(PC / 100.0) / 100.0).append('w')
        } else {
            sb.append(PC)
        }
        sb.append(" TTH: ")
        val TTH = user.totalHits
        if (TTH > 10000) {
            sb.append(Math.round(TTH / 100.0) / 100.0).append('w')
        } else {
            sb.append(TTH)
        }
        sb.append('\n')
        // PT:24d2h7m ACC:98.16%
        sb.append("PT: ")
        val PT = user.playTime
        if (PT > 86400) {
            sb.append(PT / 86400).append('d')
        }
        if (PT > 3600) {
            sb.append((PT % 86400) / 3600).append('h')
        }
        if (PT > 60) {
            sb.append((PT % 3600) / 60).append('m')
        }
        sb.append(" ACC: ").append(user.accuracy).append('%').append('\n')
        // ♡:320 kds:245 SVIP2
        sb.append("♡: ")
            .append(user.followerCount)
            .append(" kds: ")
            .append(user.kudosu.total)
            .append('\n')
        // SS:26(107) S:157(844) A:1083
        sb.append("SS: ")
            .append(user.statistics.ss)
            .append('(')
            .append(user.statistics.ssh)
            .append(')')
            .append(" S: ")
            .append(user.statistics.s)
            .append('(')
            .append(user.statistics.sh)
            .append(')')
            .append(" A: ")
            .append(user.statistics.a)
            .append('\n')
        // uid:7003013
        sb.append('\n')
        sb.append("uid: ").append(user.userID).append('\n')

        val occupation = user.occupation
        val discord = user.discord
        val interests = user.interests
        if (Objects.nonNull(occupation)) {
            sb.append("occupation: ").append(occupation.trim { it <= ' ' }).append('\n')
        }
        if (Objects.nonNull(discord)) {
            sb.append("discord: ").append(discord.trim { it <= ' ' }).append('\n')
        }
        if (Objects.nonNull(interests)) {
            sb.append("interests: ").append(interests.trim { it <= ' ' })
        }

        return sb.toString()
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(UUIService::class.java)
    }
}
