package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.model.maimai.ChuBestScore
import com.now.nowbot.model.maimai.ChuScore
import com.now.nowbot.model.maimai.ChuSong
import com.now.nowbot.model.maimai.LxChuUser
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.divingFishApiService.ChunithmApiService
import com.now.nowbot.service.lxnsApiService.LxChunithmApiService
import com.now.nowbot.throwable.TipsException
import com.now.nowbot.throwable.botRuntimeException.NetworkException
import com.now.nowbot.throwable.botRuntimeException.NoSuchElementException
import com.now.nowbot.util.AsyncMethodExecutor
import com.now.nowbot.util.CmdRange
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.command.REG_HYPHEN
import org.springframework.stereotype.Service

@Service("CHU_BP")
class ChuBestScoreService(
    private val chunithmApiService: ChunithmApiService,
    private val lxChunithmApiService: LxChunithmApiService,
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
            return mapOf(
                "user" to user,
                "score" to score,
                "song" to song
            )
        }
    }

    @JvmRecord
    data class PanelMA2Param(
        val user: ChuBestScore.User,
        val scores: List<ChuScore>,
        val scoresLatest: List<ChuScore>,
        val scoresSelection: List<ChuScore>,
    ) {
        fun toMap(): Map<String, Any> {
            return mapOf(
                "user" to user,
                "scores" to scores,
                "scores_latest" to scoresLatest,
                "scores_selection" to scoresSelection,
                "panel" to "CB"
            )
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

    override fun handleMessage(event: MessageEvent, param: ChuBestScoreParam) {
        val lxUser = if (param.qq != null) {
            try {
                lxChunithmApiService.getUser(param.qq)
            } catch (e: NetworkException) {
                null
            }
        } else null

        val scores = getBestScores(param.qq, param.name, lxUser, chunithmApiService, lxChunithmApiService)
        val charts = implementScore(param.range, scores, chunithmApiService)
        val isMultipleScore = charts.new20.size + charts.best30.size > 1

        if (charts.best30.isNotEmpty()) {
            checkCover(charts.best30, chunithmApiService)
        }

        if (charts.new20.isNotEmpty()) {
            checkCover(charts.new20, chunithmApiService)
        }

        if (charts.selection10.isNotEmpty()) {
            checkCover(charts.selection10, chunithmApiService)
        }

        val user = scores.getUser()

        val image =
            if (isMultipleScore) {
                imageService.getPanel(PanelMA2Param(user, charts.best30, charts.new20, charts.selection10).toMap(), "MA2")
            } else {
                val score = if (charts.new20.isNotEmpty()) {
                    charts.new20.first()
                } else {
                    charts.best30.first()
                }

                val song = chunithmApiService.getChunithmSong(score.songID) ?: ChuSong()

                imageService.getPanel(PanelME2Param(user, score, song).toMap(), "ME")
            }
        event.reply(image)
    }

    companion object {
        @JvmStatic
        fun getBestScores(
            qq: Long?,
            name: String?,
            user: LxChuUser?,
            chunithmApiService: ChunithmApiService,
            lxChunithmApiService: LxChunithmApiService,
        ): ChuBestScore {
            return if (user != null) {
                lxChunithmApiService.getChunithmBests(user.friendCode).toChuBestScore(user)
            } else if (qq != null) {
                chunithmApiService.getChunithmBest30Recent10(qq)
            } else if (!name.isNullOrBlank()) {
                chunithmApiService.getChunithmBest30Recent10(name)
            } else {
                throw NoSuchElementException.Player()
            }
        }

        fun checkCover(scores: List<ChuScore>, chunithmApiService: ChunithmApiService) {
            val actions = scores.map {
                return@map AsyncMethodExecutor.Runnable {
                    chunithmApiService.downloadChunithmCover(it.songID)
                }
            }

            AsyncMethodExecutor.awaitRunnableExecute(actions)
        }

        @JvmStatic
        fun implementScore(
            range: CmdRange<Int>,
            bp: ChuBestScore,
            chunithmApiService: ChunithmApiService
        ): ChuBestScore.Records {
            val offset = range.getOffset()
            val limit = range.getLimit()

            val c = bp.records

            val isStandardEmpty = c.best30.isEmpty()
            val isDeluxeEmpty = c.new20.isEmpty()

            if (offset >= 35) {
                // dx
                if (isDeluxeEmpty) {
                    throw TipsException("您的新版本成绩是空的！")
                } else {
                    chunithmApiService.insertSongData(c.best30)
                    chunithmApiService.insertPosition(c.best30, true)
                    chunithmApiService.insertChunithmAliasForScore(c.best30)

                    return ChuBestScore.Records(
                        c.best30.drop(offset - 35).take(limit),
                        emptyList(),
                    )
                }
            } else if (offset + limit < 35) {
                // sd
                if (isStandardEmpty) {
                    throw TipsException("您的旧版本成绩是空的！")
                } else {
                    chunithmApiService.insertSongData(c.new20)
                    chunithmApiService.insertPosition(c.new20, true)
                    chunithmApiService.insertChunithmAliasForScore(c.new20)

                    return ChuBestScore.Records(
                        emptyList(),
                        c.new20.drop(offset).take(limit),
                    )
                }
            } else if (offset == 0 && limit == 50) {
                // full

                if (isStandardEmpty && isDeluxeEmpty) {
                    throw NoSuchElementException.BestScore(bp.name)
                } else {
                    chunithmApiService.insertSongData(c.best30 + c.selection10)
                    chunithmApiService.insertPosition(c.best30 + c.selection10, true)
                    chunithmApiService.insertChunithmAliasForScore(c.best30 + c.selection10)

                    chunithmApiService.insertSongData(c.new20)
                    chunithmApiService.insertPosition(c.new20, true)
                    chunithmApiService.insertChunithmAliasForScore(c.new20)

                    return ChuBestScore.Records(
                        c.best30,
                        c.new20,
                        c.selection10
                    )
                }
            } else {
                // sd + dx

                if (isStandardEmpty && isDeluxeEmpty) {
                    throw NoSuchElementException.BestScore(bp.name)
                } else {
                    chunithmApiService.insertSongData(c.best30)
                    chunithmApiService.insertPosition(c.best30, true)
                    chunithmApiService.insertChunithmAliasForScore(c.best30)

                    chunithmApiService.insertSongData(c.new20)
                    chunithmApiService.insertPosition(c.new20, true)
                    chunithmApiService.insertChunithmAliasForScore(c.new20)

                    // offset < 35, offset + limit >= 35

                    return ChuBestScore.Records(
                        c.best30.take(offset + limit - 35),
                        c.new20.drop(offset),
                    )
                }
            }
        }
       
    }
}
