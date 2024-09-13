package com.now.nowbot.service.MessageServiceImpl

import com.now.nowbot.model.JsonData.Match
import com.now.nowbot.model.multiplayer.MatchCalculate.CalculateParam
import com.now.nowbot.model.multiplayer.MatchCalculate.PlayerData
import com.now.nowbot.model.multiplayer.SeriesCalculate
import com.now.nowbot.model.multiplayer.SeriesCalculate.SeriesData
import com.now.nowbot.qq.contact.Contact
import com.now.nowbot.qq.contact.Group
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.OsuApiService.OsuBeatmapApiService
import com.now.nowbot.service.OsuApiService.OsuMatchApiService
import com.now.nowbot.throwable.ServiceException.MRAException
import com.now.nowbot.util.DataUtil.getMarkdownFile
import com.now.nowbot.util.Instruction
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.regex.Matcher
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.lang.NonNull
import org.springframework.stereotype.Service
import org.springframework.util.CollectionUtils
import org.springframework.util.StringUtils
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.reactive.function.client.WebClientResponseException

@Service("SERIES_RATING")
class SeriesRatingService(
    private val matchApiService: OsuMatchApiService,
    private val beatmapApiService: OsuBeatmapApiService,
    private val imageService: ImageService,
) : MessageService<Matcher> {

    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: DataValue<Matcher>,
    ): Boolean {
        val m = Instruction.SERIES_RATING.matcher(messageText)

        if (m.find()) {
            data.value = m
            return true
        } else return false
    }

    @Throws(Throwable::class)
    override fun HandleMessage(event: MessageEvent, matcher: Matcher) {
        val dataStr = matcher.group("data")
        val nameStr = matcher.group("name")

        if (Objects.isNull(dataStr) || dataStr.isBlank()) {
            try {
                val md = getMarkdownFile("Help/series.md")
                val image = imageService.getPanelA6(md, "help")
                event.reply(image)
                return
            } catch (e: Exception) {
                throw MRAException(MRAException.Type.RATING_Series_Instructions)
            }
        }

        val rematch =
            matcher.group("rematch") == null ||
                !matcher.group("rematch").equals("r", ignoreCase = true)
        val failed =
            matcher.group("failed") == null ||
                !matcher.group("failed").equals("f", ignoreCase = true)
        val easy = getEasyMultiplier(matcher)

        val params = parseDataString(dataStr, easy, failed, rematch)
        val matchIDs = params!!.matchIDs

        if (matcher.group("csv") != null) {
            event.reply(MRAException.Type.RATING_Series_Progressing.message)
        }

        if (matchIDs.size > 50) {
            event.reply(MRAException.Type.RATING_Series_TooManyMatch.message)
        }

        val sc: SeriesCalculate
        try {
            val matches = fetchMatchFromMatchID(matchIDs, event)
            sc = SeriesCalculate(matches, params.params, beatmapApiService)
        } catch (e: MRAException) {
            throw e
        } catch (e: Exception) {
            log.error("系列比赛评分：数据计算失败", e)
            throw MRAException(MRAException.Type.RATING_Rating_CalculatingFailed)
        }

        if (matcher.group("main") != null) {
            val image: ByteArray
            try {
                image = imageService.getPanelC2(sc)
                event.reply(image)
            } catch (e: Exception) {
                log.error("系列比赛评分：数据请求失败", e)
                throw MRAException(MRAException.Type.RATING_Send_SRAFailed)
            }
        } else if (matcher.group("uu") != null) {
            val str = parseUSA(sc)
            try {
                event.reply(str).recallIn(60000)
            } catch (e: Exception) {
                log.error("系列比赛评分文字：发送失败", e)
                throw MRAException(MRAException.Type.RATING_Send_USAFailed)
            }
        } else if (matcher.group("csv") != null) {
            // 必须群聊
            val group = event.subject
            if (group is Group) {
                try {
                    val str = parseCSA(sc.seriesData)
                    group.sendFile(
                        str.toByteArray(StandardCharsets.UTF_8),
                        "${sc.series.matches.first().firstEventID}-results.csv",
                    )
                } catch (e: Exception) {
                    log.error("CSA:", e)
                    throw MRAException(MRAException.Type.RATING_Send_CSAFailed)
                }
            } else {
                throw MRAException(MRAException.Type.RATING_Send_NotGroup)
            }
        }
    }

    private fun parseCSA(data: SeriesData): String {
        val sb = StringBuilder()

        sb.append("#")
            .append(',')
            .append("UID")
            .append(',')
            .append("UserName")
            .append(',')
            .append("MRA")
            .append(',')
            .append("RWS")
            .append(',')
            .append("Win%")
            .append(',')
            .append("Play%")
            .append(',')
            .append('W')
            .append(',')
            .append('L')
            .append(',')
            .append('P')
            .append(',')
            .append("玩家分类")
            .append(',')
            .append("Player Classification")
            .append(',')
            .append("Class Color")
            .append('\n')

        for (p in data.playerDataMap.values.stream().toList()) {
            sb.append(parsePlayer2CSA(p))
        }

        return sb.toString()
    }

    private fun parsePlayer2CSA(data: PlayerData): String {
        val sb = StringBuilder()

        val winRate = 1.0 * data.win / (data.win + data.lose)
        val playRate = 1.0 * data.rrAs.size / data.arc

        try {
            sb.append(data.ranking)
                .append(',')
                .append(data.player.userID)
                .append(',')
                .append(data.player.userName)
                .append(',')
                .append(String.format("%.2f", Math.round(data.mra * 100.0) / 100.0))
                .append(',')
                .append(String.format("%.2f", Math.round(data.rws * 10000.0) / 100.0))
                .append(',')
                .append(String.format("%.0f", Math.round(winRate * 100.0) * 1.0))
                .append('%')
                .append(',')
                .append(String.format("%.0f", Math.round(playRate * 100.0) * 1.0))
                .append('%')
                .append(',')
                .append(data.win)
                .append(',')
                .append(data.lose)
                .append(',')
                .append(data.win + data.lose)
                .append(',')
                .append(data.playerClass.nameCN)
                .append(',')
                .append(data.playerClass.name)
                .append(',')
                .append(data.playerClass.color)
                .append("\n")
        } catch (e: Exception) {
            sb.append("<----User Nullified---->").append(e.message).append('\n')
        }
        return sb.toString()
    }

    private fun parseUSA(sc: SeriesCalculate): String {
        val data = sc.seriesData

        // 结果数据
        val sb = StringBuilder()

        sb.append(sc.series.seriesStat.name)
            .append("\n")
            .append("M")
            .append(data.getMatchCount())
            .append(" R")
            .append(data.getRoundCount())
            .append(" P")
            .append(data.getPlayerCount())
            .append(" S")
            .append(data.getScoreCount())
            .append("\n")

        for (p in data.getPlayerDataMap().values.stream().toList()) {
            sb.append(String.format("#%d [%.2f] %s", p.ranking, p.mra, p.player.userName))
                .append(" ")
                .append(
                    String.format(
                        "%dW-%dL %d%% (%.2fM) [%.2f] [%s | %s]",
                        p.win,
                        p.lose,
                        Math.round(p.win.toDouble() * 100 / (p.win + p.lose)),
                        p.total / 1000000.0,
                        p.rws * 100.0,
                        p.playerClass.name,
                        p.playerClass.nameCN,
                    )
                )
                .append("\n\n")
        }

        return sb.toString()
    }

    enum class Status {
        ID,
        SKIP,
        IGNORE,
        REMOVE_RECEIVED,
        REMOVE_FINISHED,
        OK,
    }

    @JvmRecord data class SRAParam(val matchIDs: List<Int>, val params: List<CalculateParam>)

    @Throws(MRAException::class)
    fun parseDataString(
        dataStr: String,
        easy: Double,
        failed: Boolean,
        rematch: Boolean,
    ): SRAParam? {
        val dataStrArray =
            dataStr
                .trim { it <= ' ' }
                .split("[\\s,，\\-|:]+".toRegex())
                .dropLastWhile { it.isEmpty() }
                .toTypedArray()
        if (dataStr.isBlank() || dataStrArray.size == 0) return null

        val matchIDs: MutableList<Int> = ArrayList()
        val skips: MutableList<Int> = ArrayList()
        val ignores: MutableList<Int> = ArrayList()
        val removes: MutableList<List<Int>> = ArrayList()

        var status =
            Status.ID // 0：收取 matchID 状态，1：收取 skip 状态，2：收取 ignore 状态。3：收取 remove 状态。 4：无需收取，直接输出。
        var matchID = 0
        var skip = 0
        var ignore = 0
        val remove: MutableList<Int> = ArrayList()

        for (i in dataStrArray.indices) {
            var v: Int
            var s = dataStrArray[i]
            if (!StringUtils.hasText(s)) continue

            if (s.contains("[")) {
                status = Status.REMOVE_RECEIVED
                s = s.replace("\\[".toRegex(), "")
            }

            if (s.contains("]") && status == Status.REMOVE_RECEIVED) {
                status = Status.REMOVE_FINISHED
                s = s.replace("]".toRegex(), "")
            }

            try {
                v = s.toInt()
            } catch (e: NumberFormatException) {
                throw MRAException(MRAException.Type.RATING_Parse_ParameterError, s, i.toString())
            }

            if (status == Status.REMOVE_RECEIVED) {
                remove.add(v)
            }

            if (status == Status.REMOVE_FINISHED) {
                remove.add(v)
                status = Status.OK
            }

            // 如果最后一个参数是场比赛，需要重复 parse（结算）
            if (i == dataStrArray.size - 1) {
                if (v < 1000) {
                    when (status) {
                        Status.SKIP -> {
                            matchIDs.add(matchID)
                            skips.add(v)
                            ignores.add(0)

                            removes.addLast(ArrayList<Int>(remove))
                            remove.clear()
                        }
                        Status.IGNORE -> {
                            matchIDs.add(matchID)
                            skips.add(skip)
                            ignores.add(v)
                            removes.addLast(ArrayList<Int>(remove))
                            remove.clear()
                        }
                        Status.REMOVE_RECEIVED ->
                            throw MRAException(
                                MRAException.Type.RATING_Parse_MissingRemove,
                                v.toString(),
                                i.toString(),
                            )
                        Status.OK -> {
                            matchIDs.add(matchID)
                            skips.add(skip)
                            ignores.add(ignore)
                            removes.addLast(ArrayList<Int>(remove))
                            remove.clear()
                        }
                        else ->
                            throw MRAException(
                                MRAException.Type.RATING_Parse_MissingMatch,
                                v.toString(),
                                i.toString(),
                            )
                    }
                    status = Status.OK
                } else {
                    when (status) {
                        Status.SKIP -> {
                            matchIDs.add(matchID)
                            skips.add(0)
                            ignores.add(0)
                        }
                        Status.IGNORE -> {
                            matchIDs.add(matchID)
                            skips.add(skip)
                            ignores.add(0)
                        }
                        Status.OK -> {
                            matchIDs.add(matchID)
                            skips.add(skip)
                            ignores.add(ignore)
                            removes.addLast(ArrayList<Int>(remove))
                            remove.clear()
                        }
                        else -> {}
                    }
                    matchIDs.add(v)
                    skips.add(0)
                    ignores.add(0)

                    status = Status.OK
                }
            } else {
                // 正常 parse
                if (v < 1000) {
                    when (status) {
                        Status.SKIP -> {
                            skip = v
                            status = Status.IGNORE
                        }
                        Status.IGNORE -> {
                            ignore = v
                            status = Status.OK
                        }
                        Status.ID,
                        Status.OK ->
                            throw MRAException(
                                MRAException.Type.RATING_Parse_MissingMatch,
                                v.toString(),
                                i.toString(),
                            )
                        else -> {}
                    }
                } else {
                    when (status) {
                        Status.ID -> {
                            matchID = v
                            status = Status.SKIP
                        }
                        Status.SKIP,
                        Status.IGNORE,
                        Status.OK -> {
                            matchIDs.add(matchID)
                            skips.add(skip)
                            ignores.add(ignore)
                            removes.addLast(ArrayList<Int>(remove))

                            matchID = v
                            skip = 0
                            ignore = 0
                            remove.clear()
                            status = Status.SKIP
                        }
                        else -> {}
                    }
                }
            }
        }

        val params: MutableList<CalculateParam> = ArrayList(matchIDs.size)

        for (i in matchIDs.indices) {
            params.add(CalculateParam(skips[i], ignores[i], removes[i], easy, failed, rematch))
        }

        return SRAParam(matchIDs, params)
    }

    @Throws(MRAException::class)
    private fun fetchMatchFromMatchID(matchIDs: List<Int>, event: MessageEvent): List<Match> {
        if (CollectionUtils.isEmpty(matchIDs)) return ArrayList()

        val matches: MutableList<Match> = ArrayList(matchIDs.size)

        var fetchMapFail = 0
        for (m in matchIDs) {
            try {
                matches.add(matchApiService.getMatchInfo(m.toLong(), 10))
            } catch (e: HttpClientErrorException.TooManyRequests) {
                fetchMapFail++
                if (fetchMapFail > 3) {
                    log.error("SRA 查询次数超限", e)
                    throw MRAException(MRAException.Type.RATING_Series_TooManyRequest, m.toString())
                }

                event.reply(MRAException.Type.RATING_Series_ReachThreshold.message)

                try {
                    Thread.sleep(10000)
                } catch (e1: InterruptedException) {
                    log.error("SRA 休眠意外中断", e1)
                    throw MRAException(
                        MRAException.Type.RATING_Series_SleepingInterrupted,
                        m.toString(),
                    )
                }
            } catch (e: WebClientResponseException.TooManyRequests) {
                fetchMapFail++
                if (fetchMapFail > 3) {
                    log.error("SRA 查询次数超限", e)
                    throw MRAException(MRAException.Type.RATING_Series_TooManyRequest, m.toString())
                }

                event.reply(MRAException.Type.RATING_Series_ReachThreshold.message)

                try {
                    Thread.sleep(10000)
                } catch (e1: InterruptedException) {
                    log.error("SRA 休眠意外中断", e1)
                    throw MRAException(
                        MRAException.Type.RATING_Series_SleepingInterrupted,
                        m.toString(),
                    )
                }
            } catch (e: HttpClientErrorException.NotFound) {
                log.error("SRA 对局找不到", e)

                event.reply(
                    String.format(MRAException.Type.RATING_Series_NotFound.message, m)
                )
            } catch (e: WebClientResponseException.NotFound) {
                log.error("SRA 对局找不到", e)

                event.reply(
                    String.format(MRAException.Type.RATING_Series_NotFound.message, m)
                )
            } catch (e: Exception) {
                log.error("SRA 对局获取失败", e)
                throw MRAException(MRAException.Type.RATING_Series_FetchFailed)
            }
        }

        return matches
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(SeriesRatingService::class.java)

        @NonNull
        @Throws(MRAException::class)
        private fun getEasyMultiplier(matcher: Matcher): Double {
            val easyStr = matcher.group("easy")
            var easy = 1.0

            if (StringUtils.hasText(easyStr)) {
                try {
                    easy = easyStr.toDouble()
                } catch (e: NullPointerException) {
                    throw MRAException(MRAException.Type.RATING_Parameter_EasyError)
                } catch (e: NumberFormatException) {
                    throw MRAException(MRAException.Type.RATING_Parameter_EasyError)
                }
            }

            if (easy > 10.0) throw MRAException(MRAException.Type.RATING_Parameter_EasyTooLarge)
            if (easy < 0.0) throw MRAException(MRAException.Type.RATING_Parameter_EasyTooSmall)
            return easy
        }
    }
}
