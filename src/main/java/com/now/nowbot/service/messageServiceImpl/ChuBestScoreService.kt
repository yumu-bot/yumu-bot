package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.model.json.ChuBestScore
import com.now.nowbot.model.json.ChuScore
import com.now.nowbot.model.json.ChuSong
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.divingFishApiService.ChunithmApiService
import com.now.nowbot.throwable.GeneralTipsException
import com.now.nowbot.throwable.TipsException
import com.now.nowbot.util.AsyncMethodExecutor
import com.now.nowbot.util.CmdRange
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.command.REG_HYPHEN
import com.yumu.core.extensions.isNotNull
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClientResponseException
import kotlin.math.max
import kotlin.math.min

@Service("CHU_BP")
class ChuBestScoreService(
    private val chunithmApiService: ChunithmApiService,
    private val imageService: ImageService,
) : MessageService<ChuBestScoreService.ChuBestScoreParam> {

    data class ChuBestScoreParam(val name: String?, val qq: Long?, val range: CmdRange<Int>, val isMyself: Boolean = false)

    @JvmRecord
    data class PanelME2Param(
        val user: ChuBestScore.User,
        val score: ChuScore,
        val song: ChuSong,
    ) {
        fun toMap(): Map<String, Any> {
            val out = mutableMapOf<String, Any>()

            out["user"] = user
            out["score"] = score
            out["song"] = song
            return out
        }
    }

    @JvmRecord
    data class PanelMA2Param(
        val user: ChuBestScore.User,
        val scores: List<ChuScore>,
        val scoresLatest: List<ChuScore>,
    ) {
        fun toMap(): Map<String, Any> {
            val out = mutableMapOf<String, Any>()

            out["user"] = user
            out["scores"] = scores
            out["scores_latest"] = scoresLatest
            out["panel"] = "CB"

            return out
        }
    }

    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: MessageService.DataValue<ChuBestScoreParam>,
    ): Boolean {
        val matcher = Instruction.CHU_BP.matcher(messageText)

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
                        ChuBestScoreParam(
                            strs.first().trim(),
                            null,
                            CmdRange(null, strs.last().toInt(), null),
                        )
                    return true
                }
            } else if (Regex("\\d{1,3}").matches(name)) {
                data.value =
                    ChuBestScoreParam(null, event.sender.id, CmdRange(null, name.toInt(), null))
                return true
            }

            data.value = ChuBestScoreParam(matcher.group("name").trim(), null, range)
        } else if (matcher.group("qq").isNullOrBlank().not()) {
            data.value = ChuBestScoreParam(null, matcher.group("qq").toLong(), range)
        } else if (event.isAt) {
            data.value = ChuBestScoreParam(null, event.target, range)
        } else {
            data.value = ChuBestScoreParam(null, event.sender.id, range, true)
        }

        return true
    }

    override fun HandleMessage(event: MessageEvent, param: ChuBestScoreParam) {
        val scores = getBestScores(param.qq, param.name, param.isMyself, chunithmApiService)
        val songs = chunithmApiService.getChunithmSongLibrary()
        val charts = implementScore(param.range, scores, songs.toMutableMap())
        val isMultipleScore = charts.recent10.size + charts.best30.size > 1

        if (charts.recent10.isNotEmpty()) {
            checkCover(charts.recent10, chunithmApiService)
        }

        if (charts.best30.isNotEmpty()) {
            checkCover(charts.best30, chunithmApiService)
        }

        val user = scores.getUser()

        val image =
            if (isMultipleScore) {
                imageService.getPanel(PanelMA2Param(user, charts.best30, charts.recent10).toMap(), "MA2")
            } else {
                val score =
                    if (charts.recent10.size > 0) {
                        charts.recent10.first()
                    } else {
                        charts.best30.first()
                    }

                val song = songs[score.songID.toInt()] ?: ChuSong()

                imageService.getPanel(PanelME2Param(user, score, song).toMap(), "ME")
            }
        event.reply(image)
    }

    companion object {
        @JvmStatic
        fun getBestScores(
            qq: Long?,
            name: String?,
            isMyself: Boolean,
            chunithmApiService: ChunithmApiService,
        ): ChuBestScore {
            return if (qq.isNotNull()) {
                try {
                    chunithmApiService.getChunithmBest30Recent10(qq!!)
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
                    chunithmApiService.getChunithmBest30Recent10(name!!)
                } catch (e: WebClientResponseException.BadRequest) {
                    throw GeneralTipsException(GeneralTipsException.Type.G_Maimai_NameBadRequest)
                } catch (e: WebClientResponseException.Forbidden) {
                    throw GeneralTipsException(GeneralTipsException.Type.G_Maimai_PlayerForbidden)
                }
            } else {
                throw GeneralTipsException(GeneralTipsException.Type.G_Null_PlayerUnknown)
            }
        }

        fun checkCover(scores: List<ChuScore>, chunithmApiService: ChunithmApiService) {
            val actions = scores.map {
                AsyncMethodExecutor.Runnable {
                    chunithmApiService.downloadChunithmCover(it.songID)
                }
            }

            AsyncMethodExecutor.AsyncRunnable(actions)
        }

        @JvmStatic
        fun implementScore(
            range: CmdRange<Int>,
            bp: ChuBestScore,
            song: MutableMap<Int, ChuSong>,
        ): ChuBestScore.Records {
            val offset = range.getOffset()
            val limit = range.getLimit()

            val c = bp.records

            val isStandardEmpty = c.best30.isEmpty()
            val isDeluxeEmpty = c.recent10.isEmpty()

            if (offset > 35) {
                // dx
                if (isDeluxeEmpty) {
                    throw TipsException("您的新版本成绩是空的！")
                } else {
                    ChuScore.insertSongData(c.best30, song)
                    ChuScore.insertPosition(c.recent10, false)

                    return ChuBestScore.Records(
                        c.recent10.subList(
                            min(max(offset - 35, 0), c.recent10.size - 1),
                            min(offset + limit - 35, c.recent10.size),
                        ),
                        mutableListOf(),
                    )
                }
            } else if (offset + limit < 35) {
                // sd
                if (isStandardEmpty) {
                    throw TipsException("您的旧版本成绩是空的！")
                } else {
                    ChuScore.insertSongData(c.best30, song)
                    ChuScore.insertPosition(c.best30, true)

                    return ChuBestScore.Records(
                        mutableListOf(),
                        c.best30.subList(
                            min(max(offset, 0), c.best30.size - 1),
                            min(offset + limit, c.best30.size),
                        ),
                    )
                }
            } else {
                // sd + dx

                if (isStandardEmpty && isDeluxeEmpty) {
                    throw GeneralTipsException(GeneralTipsException.Type.G_Empty_Score)
                } else if (isDeluxeEmpty) {
                    ChuScore.insertSongData(c.best30, song)
                    ChuScore.insertPosition(c.best30, true)

                    return ChuBestScore.Records(
                        mutableListOf(),
                        c.best30.subList(
                            min(max(offset, 0), c.best30.size - 1),
                            min(offset + limit, c.best30.size),
                        ),
                    )
                } else if (isStandardEmpty) {
                    ChuScore.insertSongData(c.recent10, song)
                    ChuScore.insertPosition(c.recent10, false)

                    return ChuBestScore.Records(
                        c.recent10.subList(
                            min(max(offset - 35, 0), c.recent10.size - 1),
                            min(offset + limit - 35, c.recent10.size),
                        ),
                        mutableListOf(),
                    )
                } else {
                    ChuScore.insertSongData(c.best30, song)
                    ChuScore.insertSongData(c.recent10, song)

                    ChuScore.insertPosition(c.best30, true)
                    ChuScore.insertPosition(c.recent10, false)
                    return ChuBestScore.Records(
                        c.recent10.subList(
                            min(max(offset - 35, 0), c.recent10.size - 1),
                            min(offset + limit - 35, c.recent10.size),
                        ),
                        c.best30.subList(
                            min(max(offset, 0), c.best30.size - 1),
                            min(offset + limit, c.best30.size),
                        ),
                    )
                }
            }
        }
    }
}
