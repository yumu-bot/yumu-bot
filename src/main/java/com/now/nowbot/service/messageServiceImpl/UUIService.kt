package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.config.NowbotConfig
import com.now.nowbot.dao.OsuUserInfoDao
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.osu.OsuUser
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.messageServiceImpl.UUIService.UUIParam
import com.now.nowbot.service.osuApiService.OsuUserApiService
import com.now.nowbot.throwable.TipsException
import com.now.nowbot.util.CmdUtil.getMode
import com.now.nowbot.util.CmdUtil.getUserWithoutRange
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.command.FLAG_DAY
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.jvm.optionals.getOrNull
import kotlin.math.*

@Service("UU_INFO")
class UUIService(
    private val userApiService: OsuUserApiService,
    private val infoDao: OsuUserInfoDao,
) : MessageService<UUIParam> {

    @JvmRecord data class UUIParam(
        val user: OsuUser,
        val historyUser: OsuUser?,
        val mode: OsuMode,
    )

    @Throws(TipsException::class)
    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: DataValue<UUIParam>,
    ): Boolean {
        val matcher = Instruction.UU_INFO.matcher(messageText)
        if (!matcher.find()) {
            return false
        }
        val mode = getMode(matcher)
        val user = getUserWithoutRange(event, matcher, mode)

        val day = (matcher.group(FLAG_DAY) ?: "").toLongOrNull() ?: 1L

        val historyUser =
            infoDao.getLastFrom(
                user.userID,
                user.currentOsuMode,
                LocalDate.now().minusDays(day)
            ).map { OsuUserInfoDao.fromArchive(it) }.getOrNull()

        val currentMode = OsuMode.getMode(mode.data!!, user.currentOsuMode)

        data.value = UUIParam(user, historyUser, currentMode)
        return true
    }

    @Throws(Throwable::class)
    override fun HandleMessage(event: MessageEvent, param: UUIParam) {
        val user = param.user

        val avatar: ByteArray = userApiService.getAvatarByte(user)

        val message = getUUInfo(user, avatar, param.historyUser)
        try {
            event.reply(message)
        } catch (e: Exception) {
            log.error("UUI 数据发送失败", e)
            event.reply("UUI 请求超时。\n请重试。或使用增强的 !yminfo。")
        }
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(UUIService::class.java)

        // 这是 v0.1.0 的 ymi 文字版本，移到这里
        @Deprecated("user getUUInfo instead.")
        fun getUUInfoLegacy(user: OsuUser, mode: OsuMode?): String {
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
                .append(user.country!!.code)
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
            val pc = user.playCount
            if (pc > 10000) {
                sb.append(Math.round(pc / 100.0) / 100.0).append('w')
            } else {
                sb.append(pc)
            }
            sb.append(" TTH: ")
            val tth = user.totalHits
            if (tth > 10000) {
                sb.append(Math.round(tth / 100.0) / 100.0).append('w')
            } else {
                sb.append(tth)
            }
            sb.append('\n')
            // PT:24d2h7m ACC:98.16%
            sb.append("PT: ")
            val pt = user.playTime
            if (pt > 86400) {
                sb.append(pt / 86400).append('d')
            }
            if (pt > 3600) {
                sb.append((pt % 86400) / 3600).append('h')
            }
            if (pt > 60) {
                sb.append((pt % 3600) / 60).append('m')
            }
            sb.append(" ACC: ").append(user.accuracy).append('%').append('\n')
            // ♡:320 kds:245 SVIP2
            sb.append("♡: ")
                .append(user.followerCount)
                .append(" kds: ")
                .append(user.kudosu?.total)
                .append('\n')
            // SS:26(107) S:157(844) A:1083
            sb.append("SS: ")
                .append(user.statistics!!.countSS)
                .append('(')
                .append(user.statistics!!.countSSH)
                .append(')')
                .append(" S: ")
                .append(user.statistics!!.countS)
                .append('(')
                .append(user.statistics!!.countSH)
                .append(')')
                .append(" A: ")
                .append(user.statistics!!.countA)
                .append('\n')
            // uid:7003013
            sb.append('\n')
            sb.append("uid: ").append(user.userID).append('\n')

            val occupation = user.occupation
            val discord = user.discord
            val interests = user.interests
            if (occupation != null) {
                sb.append("occupation: ").append(occupation.trim()).append('\n')
            }
            if (discord != null) {
                sb.append("discord: ").append(discord.trim()).append('\n')
            }
            if (interests != null) {
                sb.append("interests: ").append(interests.trim())
            }

            return sb.toString()
        }

        // 改进 UUInfo
        fun getUUInfo(user: OsuUser, avatar: ByteArray?, historyUser: OsuUser? = null): MessageChain {
            val image = avatar ?: Files.readAllBytes(
                Path.of(NowbotConfig.EXPORT_FILE_PATH).resolve("avatar-guest.png")
            )

            /*
             * [头像]
             *
             * Muziyami (osu): 10086PP
             * Rank: #46037 CN#831
             *
             * PC: 12451
             * TTH: 231313241
             * PT: 28D 1H 17M
             *
             * SS: 34(120) S: 167(989) A: 1691
             *
             * Accuracy: 98.1111%
             * Follower: 577(13)
             * Lv: 100.51
             *
             * ID: 1234567
             * Occupation: Elite Graveyarded Mapper
             * Discord: 1234
             * Interests: your mom
             */

            // 如果玩家的全球排名是 0，那么进入历史最高排名模式
            val isHistoryHighestMode = user.globalRank == 0L

            val globalRank = if (isHistoryHighestMode) {
                "#" + (user.highestRank?.rank?.toString() ?: "0") + "^ (" +
                        (user.highestRank?.updatedAt?.format(DateTimeFormatter.ofPattern("yyyy/MM/dd")) ?: "-") + ")"
            } else {
                "#" + user.globalRank.toString()
            }

            val countryRank = (user.country?.code ?: "??") + if (isHistoryHighestMode) {
                " "
            } else {
                " #" + user.countryRank.toString()
            }

            val time = getTime(user.playTime)

            val level: String = when(user.levelProgress) {
                in 1..99 -> "${user.levelCurrent}.${user.levelProgress}"
                100 -> (user.levelCurrent + 1).toString()
                else -> user.levelCurrent.toString()
            }

            // 和历史的自己比较
            val deltaPP: String
            val deltaPC: String
            val deltaPT: String
            val deltaTTH: String
            val deltaACC: String
            val deltaGR: String
            val deltaCR: String

            if (historyUser != null) {
                deltaPP = getDelta(user.pp, historyUser.pp, digit = 2)
                deltaPC = getDelta(user.playCount, historyUser.playCount, digit = 0, hasSeperator = true)
                deltaPT = if (user.playTime - historyUser.playTime > 0){
                    " (+${getTime(user.playTime - historyUser.playTime)})"
                } else {
                    ""
                }
                deltaTTH = getDelta(user.totalHits, historyUser.totalHits, digit = 0, hasSeperator = true)
                deltaACC = getDelta(user.accuracy, historyUser.accuracy, digit = 2, additionalUnit = "%")
                deltaGR = if (isHistoryHighestMode) {
                    ""
                } else {
                    // 注意，排名是越小越高
                    getDelta(historyUser.globalRank, user.globalRank, digit = 0, hasSeperator = true)
                }
                deltaCR = if (isHistoryHighestMode) {
                    ""
                } else {
                    // 注意，排名是越小越高
                    getDelta(historyUser.countryRank, user.countryRank, digit = 0, hasSeperator = true)
                }
            } else {
                deltaPP = ""
                deltaPC = ""
                deltaPT = ""
                deltaTTH = ""
                deltaACC = ""
                deltaGR = ""
                deltaCR = ""
            }

            val pp = String.format("%.2f", round(user.pp * 100.0) / 100.0)

            val info = """
            
            ${user.username} (${user.currentOsuMode.shortName}): ${pp}PP $deltaPP
            Rank: $globalRank$deltaGR $countryRank$deltaCR
            
            PC: ${String.format("%,d", user.playCount)}$deltaPC
            PT: $time $deltaPT
            TTH: ${String.format("%,d", user.totalHits)}$deltaTTH
            
            SS: ${user.statistics?.countSS} (${user.statistics?.countSSH}) S: ${user.statistics?.countS} (${user.statistics?.countSH}) A: ${user.statistics?.countA}
            
            Accuracy: ${user.accuracy}% $deltaACC
            Follower: ${user.followerCount} (${user.mappingFollowerCount})
            Lv: $level
            
            ID: ${user.userID}
            
        """.trimIndent()

            val sb = StringBuilder(info)

            val occupation = user.occupation
            val discord = user.discord
            val interests = user.interests
            val website = user.website

            if (occupation != null) {
                sb.append("Occupation: ").append(occupation.trim()).append('\n')
            }
            if (discord != null) {
                sb.append("Discord: ").append(discord.trim()).append('\n')
            }
            if (interests != null) {
                sb.append("Interests: ").append(interests.trim()).append('\n')
            }
            if (website != null) {
                sb.append("Website: ").append(website.trim())
            }

            return MessageChain(sb.toString().trimEnd('\n'), image)
        }

        private fun getTime(seconds: Long): String {
            val time = StringBuilder()

            if (seconds > 86400) {
                time.append(seconds / 86400).append('D').append(' ')
            }

            if (seconds > 3600) {
                time.append((seconds % 86400) / 3600).append('H').append(' ')
            }

            if (seconds > 60) {
                time.append((seconds % 3600) / 60).append('M')
            } else {
                time.append(seconds).append('S')
            }

            return time.toString()
        }

        private fun getDelta(
            now: Number,
            before: Number,
            digit: Int = 0,
            hasSeperator: Boolean = false,
            additionalUnit: String = ""
        ): String {
            val bf = before.toDouble()
            val nw = now.toDouble()

            if (bf.absoluteValue < 1.0 || nw.absoluteValue < 1.0) {
                return ""
            }

            val delta = nw - bf

            val scale = 10.0.pow(digit)

            val sign = getSign(delta, scale)

            val formatter = if (hasSeperator) {
                "%,d"
            } else {
                "%.${digit}f"
            }

            val abs: Number = if (hasSeperator) {
                delta.absoluteValue.roundToInt()
            } else {
                round(delta.absoluteValue * scale) / scale
            }

            return if (delta in -0.5 ..< 0.5) {
                ""
            } else {
                (" (" + sign + String.format(formatter, abs) + additionalUnit + ")").replace("0.", ".")
            }
        }

        private fun getSign(number: Double, scale: Double): String {
            return when {
                number >= 0.5 / scale -> "+"
                number < -0.5 / scale -> "-"
                else -> ""
            }
        }
    }
}
