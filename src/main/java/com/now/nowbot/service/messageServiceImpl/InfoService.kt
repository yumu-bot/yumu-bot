package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.dao.OsuUserInfoDao
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.osu.InfoLogStatistics
import com.now.nowbot.model.osu.LazerMod
import com.now.nowbot.model.osu.LazerScore
import com.now.nowbot.model.osu.OsuUser
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.qq.tencent.TencentMessageService
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.messageServiceImpl.InfoService.InfoParam
import com.now.nowbot.service.osuApiService.OsuCalculateApiService
import com.now.nowbot.service.osuApiService.OsuScoreApiService
import com.now.nowbot.service.osuApiService.OsuUserApiService
import com.now.nowbot.throwable.TipsException
import com.now.nowbot.throwable.botRuntimeException.IllegalStateException
import com.now.nowbot.util.*
import com.now.nowbot.util.CmdUtil.getMode
import com.now.nowbot.util.CmdUtil.getUserWithoutRange
import com.now.nowbot.util.command.FLAG_DAY
import kotlinx.coroutines.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.regex.Matcher
import kotlin.jvm.optionals.getOrNull
import kotlin.math.min

@Service("INFO")
class InfoService(
    private val userApiService: OsuUserApiService,
    private val scoreApiService: OsuScoreApiService,
    private val calculateApiService: OsuCalculateApiService,
    private val infoDao: OsuUserInfoDao,
    private val imageService: ImageService,
) : MessageService<InfoParam>, TencentMessageService<InfoParam> {

    data class InfoParam(
        val user: OsuUser,
        val bests: List<LazerScore>,
        val mode: OsuMode,
        val historyUser: OsuUser?,
        val isMyself: Boolean,
        val version: Int = 2,
    ) {
        fun toMap(): Map<String, Any> {
            when(this.version) {
                // 新面板 (v5.0) 的数据
                2 -> {
                    val out = mutableMapOf<String, Any>()

                    val scores = if (bests.size >= 6) {
                        bests.take(6)
                    } else {
                        bests
                    }

                    out["user"] = user
                    out["mode"] = mode
                    out["scores"] = scores
                    out["best_time"] = getBestTimes(bests)

                    if (historyUser != null) {
                        val stat = historyUser.statistics

                        if (stat is InfoLogStatistics) {
                            out["history_day"] = ChronoUnit.DAYS.between(stat.logTime.toLocalDate(), LocalDate.now())
                        }

                        out["history_user"] = historyUser
                    }

                    out["panel"] = "D2"

                    return out
                }

                // 老面板 (v3.6) 的数据
                else -> {
                    val out = mutableMapOf<String, Any>()

                    out["user"] = user
                    out["mode"] = mode
                    out["bp-times"] = getBestTimes(bests)
                    out["bonus_pp"] = getBonus(bests, user)

                    if (historyUser != null) {
                        val stat = historyUser.statistics

                        if (stat is InfoLogStatistics) {
                            out["day"] = ChronoUnit.DAYS.between(stat.logTime.toLocalDate(), LocalDate.now())
                        }

                        out["historyUser"] = historyUser
                    }

                    out["panel"] = "D"

                    return out
                }
            }
        }
    }

    @Throws(TipsException::class)
    override fun isHandle(event: MessageEvent, messageText: String, data: DataValue<InfoParam>): Boolean {
        val matcher = Instruction.INFO.matcher(messageText)
        val matcher2 = Instruction.INFO2.matcher(messageText)

        if (matcher.find()) {
            data.value = getParam(event, matcher, 1)
            return true
        } else if (matcher2.find()) {
            data.value = getParam(event, matcher2, 2)
            return true
        }

        return false
    }

    @Throws(Throwable::class)
    override fun HandleMessage(event: MessageEvent, param: InfoParam) {
        val image = param.getImage()
        try {
            event.reply(image)
        } catch (e: Exception) {
            log.error("玩家信息：发送失败", e)
            throw IllegalStateException.Send("玩家信息")
        }
    }

    override fun accept(event: MessageEvent, messageText: String): InfoParam? {
        val matcher = OfficialInstruction.INFO.matcher(messageText)

        if (!matcher.find()) return null
        return getParam(event, matcher, 2)
    }

    override fun reply(event: MessageEvent, param: InfoParam): MessageChain = QQMsgUtil.getImage(param.getImage())

    private fun InfoParam.getImage(): ByteArray {
        if (this.version == 2) {
            try {
                calculateApiService.applyStarToScores(bests
                    .take(min(bests.size - 1, 5))
                    .filter { LazerMod.hasStarRatingChange(it.mods) }
                )
            } catch (e: Exception) {
                log.info("玩家信息：获取新谱面星数失败")
            }
        }

        val name = when(version) {
            1 -> "D"
            else -> "D2"
        }

        return imageService.getPanel(this.toMap(), name)
    }
    private fun getParam(event: MessageEvent, matcher: Matcher, version: Int = 1): InfoParam {
        val isMyself = AtomicBoolean(false)

        val mode = getMode(matcher)
        val user: OsuUser
        val bests: List<LazerScore>

        val id = UserIDUtil.getUserIDWithoutRange(event, matcher, mode, isMyself)

        if (id != null) {
            val async = AsyncMethodExecutor.awaitPairCallableExecute(
                { userApiService.getOsuUser(id, mode.data!!) },
                {
                    val ss = scoreApiService.getBestScores(id, mode.data!!)

                    calculateApiService.applyBeatMapChanges(ss)
                    calculateApiService.applyStarToScores(ss)

                    ss
                }
            )

            user = async.first
            bests = async.second.toList()
        } else {
            user = getUserWithoutRange(event, matcher, getMode(matcher), isMyself)
            bests = scoreApiService.getBestScores(user.userID, mode.data!!)
        }

        val day = (matcher.group(FLAG_DAY) ?: "").toLongOrNull() ?: 1L

        val historyUser =
            infoDao.getLastFrom(
                user.userID,
                user.currentOsuMode,
                LocalDate.now().minusDays(day)
            ).map { OsuUserInfoDao.fromArchive(it) }.getOrNull()

        val currentMode = OsuMode.getMode(mode.data!!, user.currentOsuMode)

        return InfoParam(user, bests, currentMode, historyUser, isMyself.get(), version)
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(InfoService::class.java)

        private fun getBestTimes(bests: List<LazerScore>): IntArray {
            val times: List<OffsetDateTime> = bests.map { it.endedTime.withOffsetSameLocal(ZoneOffset.ofHours(8)) }
            val now = LocalDate.now()

            val timeArray = IntArray(90)

            times.forEach { time ->
                run {
                    val day = (now.toEpochDay() - time.toLocalDate().toEpochDay()).toInt()
                    if (day in 0..89) {
                        timeArray[89 - day]++
                    }
                }
            }

            return timeArray
        }

        private fun getBonus(bests: List<LazerScore>, user: OsuUser): Double {
            return if (bests.isNotEmpty()) {
                DataUtil.getBonusPP(user.pp, bests.map { it.pp })
            } else {
                0.0
            }
        }
    }
}
