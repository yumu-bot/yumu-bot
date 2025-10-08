package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.model.maimai.MaiBestScore
import com.now.nowbot.model.maimai.MaiFit.ChartData
import com.now.nowbot.model.maimai.MaiFit.DiffData
import com.now.nowbot.model.maimai.MaiScore
import com.now.nowbot.model.maimai.MaiSong
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.divingFishApiService.MaimaiApiService

import com.now.nowbot.throwable.TipsException
import com.now.nowbot.throwable.botRuntimeException.NoSuchElementException
import com.now.nowbot.util.CmdRange
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.command.REG_HYPHEN
import org.springframework.stereotype.Service

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
            if (rangeStr.isNullOrBlank().not()) {
                if (rangeStr.contains(Regex(REG_HYPHEN))) {
                    val s = rangeStr.split(Regex(REG_HYPHEN))

                    when (s.size) {
                        2 -> {
                            CmdRange<Int>(null, s.first().toInt(), s.last().toInt())
                        }
                        1 -> {
                            CmdRange<Int>(null, s.first().toInt(), null)
                        }
                        else -> {
                            CmdRange<Int>(null, 1, 50)
                        }
                    }
                } else {
                    CmdRange<Int>(null, rangeStr.toInt(), null)
                }
            } else {
                CmdRange<Int>(null, 1, 50)
            }

        if (matcher.group("name").isNullOrBlank().not()) {
            val name = matcher.group("name").trim()
            if (name.contains(Regex("\\s+"))) {
                val strs = name.split(Regex("\\s+"))

                if (strs.size == 2 && Regex("\\d{1,3}").matches(strs.last())) {
                    data.value =
                        MaiBestScoreParam(
                            strs.first().trim(),
                            null,
                            CmdRange(null, strs.last().toInt(), null),
                        )
                    return true
                }
            } else if (Regex("\\d{1,3}").matches(name)) {
                data.value =
                    MaiBestScoreParam(null, event.sender.id, CmdRange(null, name.toInt(), null))
                return true
            }

            data.value = MaiBestScoreParam(matcher.group("name").trim(), null, range)
        } else if (matcher.group("qq").isNullOrBlank().not()) {
            data.value = MaiBestScoreParam(null, matcher.group("qq").toLong(), range)
        } else if (event.isAt) {
            data.value = MaiBestScoreParam(null, event.target, range)
        } else {
            data.value = MaiBestScoreParam(null, event.sender.id, range, true)
        }

        return true
    }

    override fun handleMessage(event: MessageEvent, param: MaiBestScoreParam) {
        val scores = getBestScores(param.qq, param.name, maimaiApiService)
        val charts = implementScore(param.range, scores, maimaiApiService = maimaiApiService)
        val isMultipleScore = charts.deluxe.size + charts.standard.size > 1

        val user = scores.getUser()

        val image =
            if (isMultipleScore) {
                imageService.getPanel(PanelMAParam(user, charts.standard, charts.deluxe).toMap(), "MA")
            } else {
                val score =
                    if (charts.deluxe.isNotEmpty()) {
                        charts.deluxe.first()
                    } else {
                        charts.standard.first()
                    }

                val song = maimaiApiService.getMaimaiSong(score.songID)
                    ?: throw NoSuchElementException.Song(score.songID)

                val chart = maimaiApiService.getMaimaiChartData(score.songID).getOrNull(score.index) ?: ChartData()
                val diff = maimaiApiService.getMaimaiDiffData(score.difficulty)

                imageService.getPanel(PanelMEParam(user, score, song, chart, diff).toMap(), "ME")
            }
        event.reply(image)
    }

    companion object {
        @JvmStatic
        fun getBestScores(
            qq: Long?,
            name: String?,
            maimaiApiService: MaimaiApiService,
        ): MaiBestScore {
            return if (qq != null) {
                maimaiApiService.getMaimaiBest50(qq)
            } else if (!name.isNullOrBlank()) {
                maimaiApiService.getMaimaiBest50(name)
            } else {
                throw NoSuchElementException.Player()
            }
        }

        @JvmStatic
        fun implementScore(
            range: CmdRange<Int>,
            bp: MaiBestScore,
            maimaiApiService: MaimaiApiService
        ): MaiBestScore.Charts {
            val offset = range.getOffset()
            val limit = range.getLimit()

            val c = bp.charts

            val isStandardEmpty = c.standard.isEmpty()
            val isDeluxeEmpty = c.deluxe.isEmpty()

            if (offset >= 35) {
                // dx
                if (isDeluxeEmpty) {
                    throw TipsException("您的新版本成绩是空的！")
                } else {
                    maimaiApiService.insertSongData(c.deluxe)
                    maimaiApiService.insertPosition(c.deluxe, false)
                    maimaiApiService.insertMaimaiAliasForScore(c.deluxe)

                    return MaiBestScore.Charts(
                        c.deluxe.drop(offset - 35).take(limit),
                        emptyList(),
                    )
                }
            } else if (offset + limit < 35) {
                // sd
                if (isStandardEmpty) {
                    throw TipsException("您的旧版本成绩是空的！")
                } else {
                    maimaiApiService.insertSongData(c.standard)
                    maimaiApiService.insertPosition(c.standard, true)
                    maimaiApiService.insertMaimaiAliasForScore(c.standard)

                    return MaiBestScore.Charts(
                        emptyList(),
                        c.standard.drop(offset).take(limit),
                    )
                }
            } else {
                // sd + dx

                if (isStandardEmpty && isDeluxeEmpty) {
                    throw NoSuchElementException.BestScore(bp.name)
                } else {
                    maimaiApiService.insertSongData(c.standard)
                    maimaiApiService.insertPosition(c.standard, true)
                    maimaiApiService.insertMaimaiAliasForScore(c.standard)

                    maimaiApiService.insertSongData(c.deluxe)
                    maimaiApiService.insertPosition(c.deluxe, false)
                    maimaiApiService.insertMaimaiAliasForScore(c.deluxe)

                    // offset < 35, offset + limit >= 35

                    return MaiBestScore.Charts(
                        c.deluxe.take(offset + limit - 35),
                        c.standard.drop(offset),
                    )
                }
            }
        }
    }
}
