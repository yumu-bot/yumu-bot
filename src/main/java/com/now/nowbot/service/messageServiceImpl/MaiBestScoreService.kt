package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.model.maimai.MaiBestScore
import com.now.nowbot.model.maimai.MaiFit.ChartData
import com.now.nowbot.model.maimai.MaiFit.DiffData
import com.now.nowbot.model.maimai.MaiScore
import com.now.nowbot.model.maimai.MaiSong
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.divingFishApiService.MaimaiApiService
import com.now.nowbot.throwable.botRuntimeException.NoSuchElementException
import com.now.nowbot.util.InstructionRange
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.command.REG_HYPHEN
import org.springframework.stereotype.Service

@Service("MAI_BP")
class MaiBestScoreService(
    private val maimaiApiService: MaimaiApiService,
    private val imageService: ImageService,
) : MessageService<MaiBestScoreService.MaiBestScoreParam> {

    data class MaiBestScoreParam(val name: String?, val qq: Long?, val range: InstructionRange<Int>, val isMyself: Boolean = false)

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
                            InstructionRange<Int>(null, s.first().toInt(), s.last().toInt())
                        }
                        1 -> {
                            InstructionRange<Int>(null, s.first().toInt(), null)
                        }
                        else -> {
                            InstructionRange<Int>(null, 1, 50)
                        }
                    }
                } else {
                    InstructionRange<Int>(null, rangeStr.toInt(), null)
                }
            } else {
                InstructionRange<Int>(null, 1, 50)
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
                            InstructionRange(null, strs.last().toInt(), null),
                        )
                    return true
                }
            } else if (Regex("\\d{1,3}").matches(name)) {
                data.value =
                    MaiBestScoreParam(null, event.sender.id, InstructionRange(null, name.toInt(), null))
                return true
            }

            data.value = MaiBestScoreParam(matcher.group("name").trim(), null, range)
        } else if (matcher.group("qq").isNullOrBlank().not()) {
            data.value = MaiBestScoreParam(null, matcher.group("qq").toLong(), range)
        } else if (event.hasAt()) {
            data.value = MaiBestScoreParam(null, event.target, range)
        } else {
            data.value = MaiBestScoreParam(null, event.sender.id, range, true)
        }

        return true
    }

    override fun handleMessage(event: MessageEvent, param: MaiBestScoreParam): ServiceCallStatistic? {
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

                val chart = maimaiApiService.getMaimaiChartData(score.songID)
                    .getOrNull(score.index) ?: ChartData()
                val diff = maimaiApiService.getMaimaiDiffData(score.level)

                imageService.getPanel(PanelMEParam(user, score, song, chart, diff).toMap(), "ME")
            }
        event.reply(image)

        return ServiceCallStatistic.building(event) {
            setParam(mapOf(
                "mais" to (scores.charts.deluxe.map { it.songID } + scores.charts.standard.map { it.songID }).toSet()
            ))
        }
    }

    companion object {
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

        fun implementScore(
            range: InstructionRange<Int>,
            best: MaiBestScore,
            maimaiApiService: MaimaiApiService
        ): MaiBestScore.Charts {
            val offset = range.getOffset()
            val limit = range.getLimit()

            val c = getOffsetLimitedScores(best.charts, offset, limit)

            if (c.deluxe.isEmpty() && c.standard.isEmpty()) {
                throw NoSuchElementException.BestScore(best.name)
            }

            maimaiApiService.insert(c)

            return c
        }
        private fun getOffsetLimitedScores(
            charts: MaiBestScore.Charts,
            offset: Int,
            limit: Int
        ): MaiBestScore.Charts {
            val standard = charts.standard
            val deluxe = charts.deluxe

            val filteredStandard = standard.drop(offset).take(limit)
            val remaining = limit - filteredStandard.size
            val newOffset = maxOf(0, offset - standard.size)

            val filteredDeluxe = deluxe.drop(newOffset).take(remaining)

            return MaiBestScore.Charts(filteredDeluxe, filteredStandard)
        }
    }
}
