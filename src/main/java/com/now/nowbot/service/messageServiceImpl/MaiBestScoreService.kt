package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.model.json.MaiBestScore
import com.now.nowbot.model.json.MaiFit.ChartData
import com.now.nowbot.model.json.MaiFit.DiffData
import com.now.nowbot.model.json.MaiScore
import com.now.nowbot.model.json.MaiSong
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.divingFishApiService.MaimaiApiService
import com.now.nowbot.throwable.GeneralTipsException
import com.now.nowbot.throwable.TipsException
import com.now.nowbot.util.CmdRange
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.command.REG_HYPHEN
import com.yumu.core.extensions.isNotNull
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.util.CollectionUtils
import org.springframework.util.StringUtils
import org.springframework.web.reactive.function.client.WebClientResponseException
import kotlin.math.max
import kotlin.math.min

@Service("MAI_BP")
class MaiBestScoreService(
    private val maimaiApiService: MaimaiApiService,
    private val imageService: ImageService,
) : MessageService<MaiBestScoreService.MaiBestScoreParam> {

    data class MaiBestScoreParam(val name: String?, val qq: Long?, val range: CmdRange<Int>, val isMyself: Boolean = false)

    @JvmRecord
    data class PanelMEParam(
        val user: MaiBestScore.User,
        val score: MaiScore,
        val song: MaiSong,
        val chart: ChartData,
        val diff: DiffData,
    ) {
        fun toMap(): Map<String, Any> {
            val out = mutableMapOf<String, Any>()

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
        val user: MaiBestScore.User,
        val scores: List<MaiScore>,
        val scoresLatest: List<MaiScore>,
    ) {
        fun toMap(): Map<String, Any> {
            val out = mutableMapOf<String, Any>()

            out["user"] = user
            out["scores"] = scores
            out["scores_latest"] = scoresLatest
            out["panel"] = "MB"

            return out
        }
    }

    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: MessageService.DataValue<MaiBestScoreParam>,
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
                    val s = rangeStr.split(Regex(REG_HYPHEN))

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

        if (StringUtils.hasText(matcher.group("name"))) {
            val name = matcher.group("name").trim()
            if (name.contains(Regex("\\s+"))) {
                val strs = name.split(Regex("\\s+"))

                if (strs.size == 2 && Regex("\\d{1,3}").matches(strs.last())) {
                    data.value =
                        MaiBestScoreParam(
                            strs.first().trim(),
                            null,
                            CmdRange<Int>(null, strs.last().toInt(), null),
                        )
                    return true
                }
            } else if (Regex("\\d{1,3}").matches(name)) {
                data.value =
                    MaiBestScoreParam(null, event.sender.id, CmdRange<Int>(null, name.toInt(), null))
                return true
            }

            data.value = MaiBestScoreParam(matcher.group("name").trim(), null, range)
        } else if (StringUtils.hasText(matcher.group("qq"))) {
            data.value = MaiBestScoreParam(null, matcher.group("qq").toLong(), range)
        } else if (event.isAt) {
            data.value = MaiBestScoreParam(null, event.target, range)
        } else {
            data.value = MaiBestScoreParam(null, event.sender.id, range, true)
        }

        return true
    }

    override fun HandleMessage(event: MessageEvent, param: MaiBestScoreParam) {
        val scores = getBestScores(param.qq, param.name, param.isMyself, maimaiApiService)
        val songs = maimaiApiService.maimaiSongLibrary
        val charts = implementScore(param.range, scores, songs.toMutableMap())
        val isMultipleScore = charts.deluxe.size + charts.standard.size > 1

        val user = scores.getUser()
        val fit = maimaiApiService.maimaiFitLibrary

        val image =
            if (isMultipleScore) {
                imageService.getPanel(PanelMAParam(user, charts.standard, charts.deluxe).toMap(), "MA")
            } else {
                val score =
                    if (charts.deluxe.size > 0) {
                        charts.deluxe.first()
                    } else {
                        charts.standard.first()
                    }

                val chart = fit.getChartData(score.songID.toString(), score.index)
                val diff = fit.getDiffData(chart)

                val song = songs.get(score.songID.toInt()) ?: MaiSong()

                imageService.getPanel(PanelMEParam(user, score, song, chart, diff).toMap(), "ME")
            }
        event.reply(image)
    }

    companion object {
        val log = KotlinLogging.logger { }

        @JvmStatic
        fun getBestScores(
            qq: Long?,
            name: String?,
            isMyself: Boolean,
            maimaiApiService: MaimaiApiService,
        ): MaiBestScore {
            return if (qq.isNotNull()) {
                try {
                    maimaiApiService.getMaimaiBest50(qq!!)
                } catch (e: WebClientResponseException.BadRequest) {
                    if (isMyself) {
                        throw GeneralTipsException(GeneralTipsException.Type.G_Maimai_YouBadRequest)
                    } else {
                        throw GeneralTipsException(GeneralTipsException.Type.G_Maimai_QQBadRequest)
                    }
                } catch (e: WebClientResponseException.Forbidden) {
                    if (isMyself) {
                        throw GeneralTipsException(GeneralTipsException.Type.G_Maimai_YouForbidden)
                    } else {
                        throw GeneralTipsException(GeneralTipsException.Type.G_Maimai_PlayerForbidden)
                    }
                }
            } else if (name.isNotNull()) {
                try {
                    maimaiApiService.getMaimaiBest50(name!!)
                } catch (e: WebClientResponseException.BadRequest) {
                    throw GeneralTipsException(GeneralTipsException.Type.G_Maimai_NameBadRequest)
                } catch (e: WebClientResponseException.Forbidden) {
                    throw GeneralTipsException(GeneralTipsException.Type.G_Maimai_PlayerForbidden)
                }
            } else {
                throw GeneralTipsException(GeneralTipsException.Type.G_Null_PlayerUnknown)
            }
        }

        @JvmStatic
        fun implementScore(
            range: CmdRange<Int>,
            bp: MaiBestScore,
            song: MutableMap<Int, MaiSong>,
        ): MaiBestScore.Charts {
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

                    return MaiBestScore.Charts(
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

                    return MaiBestScore.Charts(
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

                    return MaiBestScore.Charts(
                        mutableListOf(),
                        c.standard.subList(
                            min(max(offset, 0), c.standard.size - 1),
                            min(offset + limit, c.standard.size),
                        ),
                    )
                } else if (isStandardEmpty) {
                    MaiScore.insertSongData(c.deluxe, song)
                    MaiScore.insertPosition(c.deluxe, false)

                    return MaiBestScore.Charts(
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
                    return MaiBestScore.Charts(
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
