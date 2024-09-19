package com.now.nowbot.service.MessageServiceImpl

import com.now.nowbot.model.JsonData.MaiBestPerformance
import com.now.nowbot.model.JsonData.MaiFit.ChartData
import com.now.nowbot.model.JsonData.MaiFit.DiffData
import com.now.nowbot.model.JsonData.MaiScore
import com.now.nowbot.model.JsonData.MaiSong
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.AtMessage
import com.now.nowbot.service.DivingFishApiService.MaimaiApiService
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.throwable.GeneralTipsException
import com.now.nowbot.throwable.TipsException
import com.now.nowbot.util.CmdRange
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.QQMsgUtil
import com.now.nowbot.util.command.REG_HYPHEN
import com.yumu.core.extensions.isNotNull
import kotlin.math.max
import kotlin.math.min
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.util.CollectionUtils
import org.springframework.util.StringUtils
import org.springframework.web.reactive.function.client.WebClientResponseException

@Service("MAI_BP")
class MaiBestPerformanceService(
    private val maimaiApiService: MaimaiApiService,
    private val imageService: ImageService,
) : MessageService<MaiBestPerformanceService.MaiScoreParam> {

    data class MaiScoreParam(val name: String?, val qq: Long?, val range: CmdRange<Int>)

    @JvmRecord
    data class PanelMEParam(
        val user: MaiBestPerformance.User,
        val score: MaiScore,
        val song: MaiSong,
        val chart: ChartData,
        val diff: DiffData,
    ) {
        fun toMap(): Map<String, Any> {
            var out = mutableMapOf<String, Any>()

            out["user"] = user
            out["score"] = score
            out["song"] = song
            out["chart"] = chart
            out["diff"] = diff
            return out
        }
    }

    @JvmRecord
    data class PanelMAParam(
        val user: MaiBestPerformance.User,
        val scores: List<MaiScore>,
        val scores_latest: List<MaiScore>,
    ) {
        fun toMap(): Map<String, Any> {
            var out = mutableMapOf<String, Any>()

            out["user"] = user
            out["scores"] = scores
            out["scores_latest"] = scores_latest

            return out
        }
    }

    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: MessageService.DataValue<MaiScoreParam>,
    ): Boolean {
        val matcher = Instruction.MAI_BP.matcher(messageText)

        if (!matcher.find()) {
            return false
        }

        // TODO 这里用老式的获取方法，但是最好写成一个通用的，类似于 cmdUtil
        val rangeStr = matcher.group("range")

        val range =
            if (StringUtils.hasText(rangeStr)) {
                if (rangeStr.contains(Regex(REG_HYPHEN))) {
                    var s = rangeStr.split(Regex(REG_HYPHEN))

                    if (s.size == 2) {
                        CmdRange<Int>(null, s.first().toInt(), s.last().toInt())
                    } else if (s.size == 1) {
                        CmdRange<Int>(null, s.first().toInt(), null)
                    } else {
                        CmdRange<Int>(null, 1, 50)
                    }
                } else {
                    CmdRange<Int>(null, rangeStr.toInt(), null)
                }
            } else {
                CmdRange<Int>(null, 1, 50)
            }

        val at = QQMsgUtil.getType(event.message, AtMessage::class.java)

        if (StringUtils.hasText(matcher.group("name"))) {
            val name = matcher.group("name").trim()
            if (name.contains(Regex("\\s+"))) {
                val strs = name.split(Regex("\\s+"))

                if (strs.size == 2 && Regex("\\d{1,3}").matches(strs.last())) {
                    data.value =
                        MaiScoreParam(
                            strs.first().trim(),
                            null,
                            CmdRange<Int>(null, strs.last().toInt(), null),
                        )
                    return true
                }
            } else if (Regex("\\d{1,3}").matches(name)) {
                data.value =
                    MaiScoreParam(null, event.sender.id, CmdRange<Int>(null, name.toInt(), null))
                return true
            }

            data.value = MaiScoreParam(matcher.group("name").trim(), null, range)
        } else if (StringUtils.hasText(matcher.group("qq"))) {
            data.value = MaiScoreParam(null, matcher.group("qq").toLong(), range)
        } else if (at != null) {
            data.value = MaiScoreParam(null, at.target, range)
        } else {
            data.value = MaiScoreParam(null, event.sender.id, range)
        }

        return true
    }

    override fun HandleMessage(event: MessageEvent, param: MaiScoreParam) {
        val scores = getBestScores(param.qq, param.name, maimaiApiService)
        val songs = maimaiApiService.getMaimaiSongLibrary()
        val charts = getScore(param.range, scores, songs)
        val isMultipleScore = charts.deluxe.size + charts.standard.size > 1

        val user = scores.user
        val fit = maimaiApiService.getMaimaiFit()

        val image =
            if (isMultipleScore) {
                imageService.getPanelMA(PanelMAParam(user, charts.standard, charts.deluxe))
            } else {
                val score =
                    if (charts.deluxe.size > 0) {
                        charts.deluxe.first()
                    } else {
                        charts.standard.first()
                    }

                val chart = fit.getChartData(score.songID?.toString(), score.index)
                val diff = fit.getDiffData(chart)

                val song = songs.get(score.songID?.toInt()) ?: MaiSong()

                imageService.getPanelME(PanelMEParam(user, score, song, chart, diff))
            }
        event.reply(image)
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(MaiBestPerformanceService::class.java)

        fun getBestScores(
            qq: Long?,
            name: String?,
            maimaiApiService: MaimaiApiService,
        ): MaiBestPerformance {

            return if (qq.isNotNull()) {
                try {
                    maimaiApiService.getMaimaiBest50(qq)
                } catch (e: WebClientResponseException.BadRequest) {
                    throw GeneralTipsException(GeneralTipsException.Type.G_Maimai_QQBadRequest)
                } catch (e: WebClientResponseException.Forbidden) {
                    throw GeneralTipsException(GeneralTipsException.Type.G_Maimai_Forbidden)
                }
            } else if (name.isNotNull()) {
                try {
                    maimaiApiService.getMaimaiBest50(name)
                } catch (e: WebClientResponseException.BadRequest) {
                    log.error("?", e)
                    throw GeneralTipsException(GeneralTipsException.Type.G_Maimai_NameBadRequest)
                } catch (e: WebClientResponseException.Forbidden) {
                    throw GeneralTipsException(GeneralTipsException.Type.G_Maimai_Forbidden)
                }
            } else {
                throw GeneralTipsException(GeneralTipsException.Type.G_Null_PlayerUnknown)
            }
        }

        @JvmStatic
        fun getScore(
            range: CmdRange<Int>,
            bp: MaiBestPerformance,
            song: MutableMap<Int, MaiSong>,
        ): MaiBestPerformance.Charts {
            val offset = range.getOffset()
            val limit = range.getLimit()

            val c = bp.charts

            val isStandardEmpty = CollectionUtils.isEmpty(c.standard)
            val isDeluxeEmpty = CollectionUtils.isEmpty(c.deluxe)

            if (offset > 35) {
                // dx
                if (isDeluxeEmpty) {
                    throw TipsException("您的新版本成绩是空的！")
                } else {
                    MaiScore.insertSongData(c.deluxe, song)
                    MaiScore.insertPosition(c.deluxe, false)

                    return MaiBestPerformance.Charts(
                        c.deluxe.subList(
                            min(max(offset - 35, 0), c.deluxe.size - 1),
                            min(offset + limit - 35, c.deluxe.size),
                        ),
                        mutableListOf(),
                    )
                }
            } else if (offset + limit < 35) {
                // sd
                if (isStandardEmpty) {
                    throw TipsException("您的旧版本成绩是空的！")
                } else {
                    MaiScore.insertSongData(c.standard, song)
                    MaiScore.insertPosition(c.standard, true)

                    return MaiBestPerformance.Charts(
                        mutableListOf(),
                        c.standard.subList(
                            min(max(offset, 0), c.standard.size - 1),
                            min(offset + limit, c.standard.size),
                        ),
                    )
                }
            } else {
                // sd + dx

                if (isStandardEmpty && isDeluxeEmpty) {
                    throw TipsException("您的成绩是空的！")
                } else if (isDeluxeEmpty) {
                    MaiScore.insertSongData(c.standard, song)
                    MaiScore.insertPosition(c.standard, true)

                    return MaiBestPerformance.Charts(
                        mutableListOf(),
                        c.standard.subList(
                            min(max(offset, 0), c.standard.size - 1),
                            min(offset + limit, c.standard.size),
                        ),
                    )
                } else if (isStandardEmpty) {
                    MaiScore.insertSongData(c.deluxe, song)
                    MaiScore.insertPosition(c.deluxe, false)

                    return MaiBestPerformance.Charts(
                        c.deluxe.subList(
                            min(max(offset - 35, 0), c.deluxe.size - 1),
                            min(offset + limit - 35, c.deluxe.size),
                        ),
                        mutableListOf(),
                    )
                } else {
                    MaiScore.insertSongData(c.standard, song)
                    MaiScore.insertSongData(c.deluxe, song)

                    MaiScore.insertPosition(c.standard, true)
                    MaiScore.insertPosition(c.deluxe, false)
                    return MaiBestPerformance.Charts(
                        c.deluxe.subList(
                            min(max(offset - 35, 0), c.deluxe.size - 1),
                            min(offset + limit - 35, c.deluxe.size),
                        ),
                        c.standard.subList(
                            min(max(offset, 0), c.standard.size - 1),
                            min(offset + limit, c.standard.size),
                        ),
                    )
                }
            }
        }
    }
}
