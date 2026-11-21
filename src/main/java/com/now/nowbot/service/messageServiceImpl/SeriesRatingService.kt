package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.model.match.Match
import com.now.nowbot.model.match.MatchRating
import com.now.nowbot.model.match.SeriesRating
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.service.osuApiService.OsuCalculateApiService
import com.now.nowbot.service.osuApiService.OsuMatchApiService
import com.now.nowbot.throwable.botException.MRAException
import com.now.nowbot.throwable.botRuntimeException.IllegalStateException
import com.now.nowbot.throwable.botRuntimeException.NoSuchElementException
import com.now.nowbot.util.DataUtil.getMarkdownFile
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.command.ExtractType
import com.now.nowbot.util.command.FLAG_ANY
import com.now.nowbot.util.command.REGEX_SEPARATOR
import com.now.nowbot.util.command.SeriesRatingExtractor
import com.now.nowbot.util.command.StripPipeline
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException
import java.nio.charset.StandardCharsets
import java.util.regex.Matcher
import kotlin.math.round
import kotlin.time.Duration.Companion.seconds

@Service("SERIES_RATING")
class SeriesRatingService(
    private val matchApiService: OsuMatchApiService,
    private val calculateApiService: OsuCalculateApiService,
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
    override fun handleMessage(event: MessageEvent, param: Matcher): ServiceCallStatistic? {
        // 指令只把 `!sa` 之后的整段文本收进 any：`#系列名#`、easy、`:r`、`:f` 交给剥离器，
        // 剩下的就是数据串，仍然由 parseDataString 按位置解释（编号、热手、表演赛、[剔除]）。
        val context = StripPipeline(param.group(FLAG_ANY).orEmpty())
            .apply(SeriesRatingExtractor)
            .trim()
            .context

        val nameStr = context[ExtractType.SERIES_NAME].lastOrNull()?.takeIf { it.isNotBlank() }
        val dataStr = context.text.trim()

        if (dataStr.isBlank()) {
            replyHelp(event)
            return null
        }

        val rematch = !context.exists(ExtractType.REMATCH)
        val failed = !context.exists(ExtractType.FAILED)
        val easy = getEasyMultiplier(context[ExtractType.EASY].lastOrNull())

        val params = parseDataString(dataStr, easy, failed, rematch)

        // 只剩分隔符（例如 `!sa ,`）时 parseDataString 会返回 null，这时也回用法
        if (params == null) {
            replyHelp(event)
            return null
        }

        val matchIDs = params.matchIDs

        if (param.group("csv") != null) {
            event.replyAsync(MRAException.Type.RATING_Series_Progressing.message)
        }

        if (matchIDs.size > 50) {
            event.replyAsync(MRAException.Type.RATING_Series_TooManyMatch.message)
        }

        val sr: SeriesRating

        try {
            val matches = fetchMatchFromMatchID(matchIDs, event)
            sr = SeriesRating(
                matches,
                params.params,
                beatmapApiService,
                calculateApiService
            )

            if (nameStr != null) {
                sr.name = nameStr
            }

            sr.calculate()
        } catch (e: MRAException) {
            throw e
        } catch (e: Exception) {
            log.error("系列斗力：数据计算失败", e)
            throw IllegalStateException.Calculate("系列斗力")
        }

        if (sr.rounds.isEmpty()) {
            throw NoSuchElementException.SeriesRound()
        }

        if (param.group("main") != null) {
            val image: ByteArray
            try {
                image = imageService.getPanel(sr, "C2")
                event.replyAsync(image)
            } catch (e: Exception) {
                log.error("系列斗力：数据请求失败", e)
                throw IllegalStateException.Send("系列斗力")
            }
        } else if (param.group("uu") != null) {
            val str = parseUSA(sr)
            try {
                event.replyAndRecallAsync(str, 60.seconds)
            } catch (e: Exception) {
                log.error("系列斗力文字：发送失败", e)
                throw IllegalStateException.Send("系列斗力文字版")
            }
        } else if (param.group("csv") != null) {
            // 必须群聊
            event.replyFileInGroupAsync(parseCSA(sr).toByteArray(StandardCharsets.UTF_8), "${sr.statistics.matchID}-results.csv")
        }

        return ServiceCallStatistic.building(event) {
            setParam(mapOf(
                "mids" to sr.matches.map { it.id },
                "uids" to sr.players.keys.distinct(),
                "bids" to sr.rounds.map { it.beatmapID }.distinct(),
                "sids" to sr.rounds.mapNotNull { it.beatmap?.beatmapsetID }.distinct(),
                "modes" to sr.rounds.map { it.mode.modeValue }.distinct()
            ))
        }
    }

    /**
     * 没有参数时回用法：优先发 `Help/series.md` 的图片，取不到就回文字说明。
     */
    private fun replyHelp(event: MessageEvent) {
        try {
            val md = getMarkdownFile("Help/series.md")
            val image = imageService.getPanelA6(md, "help")
            event.replyAsync(image)
        } catch (_: Exception) {
            throw MRAException(MRAException.Type.RATING_Series_Instructions)
        }
    }

    private fun parseCSA(sr: SeriesRating): String {
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

        for (p in sr.playerDataList) {
            sb.append(parsePlayer2CSA(p))
        }

        return sb.toString()
    }

    private fun parsePlayer2CSA(data: MatchRating.PlayerData): String {
        val sb = StringBuilder()

        val winRate = 1.0 * data.win / (data.win + data.lose)
        val playRate = 1.0 * data.rawRatings.size / data.associatedRoundCount

        try {
            sb.append(data.ranking)
                .append(',')
                .append(data.player.userID)
                .append(',')
                .append(data.player.username)
                .append(',')
                .append(String.format("%.2f", round(data.mra * 100.0) / 100.0))
                .append(',')
                .append(String.format("%.2f", round(data.rws * 10000.0) / 100.0))
                .append(',')
                .append(String.format("%.0f", round(winRate * 100.0)))
                .append('%')
                .append(',')
                .append(String.format("%.0f", round(playRate * 100.0)))
                .append('%')
                .append(',')
                .append(data.win)
                .append(',')
                .append(data.lose)
                .append(',')
                .append(data.win + data.lose)
                .append(',')
                .append(data.playerClass!!.chinese)
                .append(',')
                .append(data.playerClass!!.english)
                .append(',')
                .append(data.playerClass!!.color)
                .append("\n")
        } catch (e: Exception) {
            sb.append("<----User Nullified---->").append(e.message).append('\n')
        }
        return sb.toString()
    }

    private fun parseUSA(sr: SeriesRating): String {
        // 结果数据
        val sb = StringBuilder()

        sb.append(sr.statistics.name)
            .append("...\n")
            .append("M")
            .append(sr.matches.size)
            .append(" R")
            .append(sr.rounds.size)
            .append(" P")
            .append(sr.players.size)
            .append(" S")
            .append(sr.scores.size)
            .append("\n")

        for (p in sr.playerDataList) {
            sb.append(String.format("#%d [%.2f] %s", p.ranking, p.mra, p.player.username))
                .append(" ")
                .append(
                    String.format(
                        "%dW-%dL %d%% (%.2fM) [%.2f] [%s | %s]",
                        p.win,
                        p.lose,
                        round((p.win * 100.0) / (p.win + p.lose)),
                        p.total / 1000000.0,
                        p.rws * 100.0,
                        p.playerClass!!.english,
                        p.playerClass!!.chinese,
                    )
                )
                .append("\n\n")
        }

        return sb.toString()
    }

    @JvmRecord data class SRAPanelParam(val matchIDs: List<Long>, val params: List<MatchRating.RatingParam>)

    /**
     * 解析系列赛的数据串。
     *
     * 每场比赛一组，按位置写 `编号 热手 表演赛 [剔除场次]`，可以连写多组：
     * `12345 1 0 [2,4] 23456 2 1`。编号不小于 1000，热手 / 表演赛 / 剔除场次都是更小的数字。
     *
     * 方括号里的剔除场次可以用逗号或空格分开（`[2,4]`、`[ 2, 4 ]`），每一组后面都可以写；
     * 旧实现只认最后一组的方括号，中间的方括号会把解析状态搞乱，抛出「漏了一场比赛」。
     *
     * @return 没有任何数据时返回 null
     * @throws MRAException 数字无法解析、漏了编号、方括号没有闭合
     */
    @Throws(MRAException::class)
    fun parseDataString(
        dataStr: String,
        easy: Double,
        failed: Boolean,
        rematch: Boolean,
    ): SRAPanelParam? {
        // 先把方括号和数字拆开，`12345[2,4]` 和 `12345 [2,4]` 等价
        val tokens = dataStr
            .replace("[", " [ ")
            .replace("]", " ] ")
            .trim()
            .split(REGEX_SEPARATOR)
            .filter { it.isNotBlank() }

        if (tokens.isEmpty()) return null

        val matchIDs = ArrayList<Int>()
        val skips = ArrayList<Int>()
        val ignores = ArrayList<Int>()
        val removes = ArrayList<List<Int>>()

        var matchID: Int? = null
        var skip = 0
        var ignore = 0
        var skipSeen = false
        var ignoreSeen = false

        // 当前组的剔除场次；null 表示这一组还没写过方括号
        var remove: MutableList<Int>? = null

        // 是否正在方括号里，以及最近一次剔除场次，用来报「没闭合」的位置
        var inRemove = false
        var removeToken = "["
        var removeTokenIndex = 0

        /** 结算当前这一组；还没有编号（空数据）时什么都不做。 */
        fun commit() {
            val id = matchID ?: return

            matchIDs.add(id)
            skips.add(skip)
            ignores.add(ignore)
            removes.add(remove?.let { ArrayList(it) } ?: emptyList())

            matchID = null
            skip = 0
            ignore = 0
            skipSeen = false
            ignoreSeen = false
            remove = null
        }

        for ((index, token) in tokens.withIndex()) {
            when (token) {
                "[" -> {
                    if (inRemove) {
                        throw MRAException(MRAException.Type.RATING_Parse_MissingRemove, token, index.toString())
                    }

                    remove = remove ?: ArrayList()
                    inRemove = true
                }

                "]" -> {
                    if (!inRemove) {
                        throw MRAException(MRAException.Type.RATING_Parse_ParameterError, token, index.toString())
                    }

                    inRemove = false
                }

                else -> {
                    val v = token.toIntOrNull()
                        ?: throw MRAException(MRAException.Type.RATING_Parse_ParameterError, token, index.toString())

                    if (inRemove) {
                        // 方括号属于前一场比赛：没有编号就不知道要剔除哪一场
                        if (matchID == null) {
                            throw MRAException(MRAException.Type.RATING_Parse_MissingMatch, token, index.toString())
                        }

                        remove?.add(v)
                        removeToken = token
                        removeTokenIndex = index
                    } else if (v >= 1000) {
                        // 新的一场：先把上一场结算掉
                        commit()
                        matchID = v
                    } else {
                        // 小数字：热手 / 表演赛
                        if (matchID == null) {
                            throw MRAException(MRAException.Type.RATING_Parse_MissingMatch, token, index.toString())
                        }

                        when {
                            !skipSeen -> {
                                skip = v
                                skipSeen = true
                            }

                            !ignoreSeen -> {
                                ignore = v
                                ignoreSeen = true
                            }

                            // 一组只有「编号 热手 表演赛」三个位置，多出来的数字没有含义
                            else -> throw MRAException(
                                MRAException.Type.RATING_Parse_MissingMatch,
                                token,
                                index.toString(),
                            )
                        }
                    }
                }
            }
        }

        if (inRemove) {
            throw MRAException(
                MRAException.Type.RATING_Parse_MissingRemove,
                removeToken,
                removeTokenIndex.toString(),
            )
        }

        commit()

        val params: List<MatchRating.RatingParam> =
            List(matchIDs.size) { i ->
                MatchRating.RatingParam(skips[i], ignores[i], removes[i], easy, failed, rematch)
            }

        return SRAPanelParam(matchIDs.map { it.toLong() }, params)
    }

    @Throws(MRAException::class)
    private fun fetchMatchFromMatchID(matchIDs: List<Long>, event: MessageEvent): List<Match> {
        if (matchIDs.isEmpty()) return ArrayList()

        val matches: MutableList<Match> = ArrayList(matchIDs.size)

        var failTimes = 0
        for (m in matchIDs) {
            try {
                matches.add(matchApiService.getMatch(m, 10))
            } catch (e: HttpClientErrorException.TooManyRequests) {
                failTimes++
                if (failTimes > 3) {
                    log.error("SRA 查询次数超限", e)
                    throw MRAException(MRAException.Type.RATING_Series_TooManyRequest, m.toString())
                }

                event.replyAsync(MRAException.Type.RATING_Series_ReachThreshold.message)

                try {
                    Thread.sleep(10000)
                } catch (e1: InterruptedException) {
                    log.error("SRA 休眠意外中断", e1)
                    throw MRAException(
                        MRAException.Type.RATING_Series_SleepingInterrupted,
                        m.toString(),
                    )
                }
            } catch (e: HttpClientErrorException) {
                failTimes++
                if (failTimes > 3) {
                    log.error("SRA 查询次数超限", e)
                    throw MRAException(MRAException.Type.RATING_Series_TooManyRequest, m.toString())
                }

                event.replyAsync(MRAException.Type.RATING_Series_ReachThreshold.message)

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

                event.replyAsync(
                    m.toString().format(MRAException.Type.RATING_Series_NotFound.message)
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

        /**
         * Easy 倍率。没写就是 1.0，写了就必须落在 0 ~ 10 之间。
         */
        @Throws(MRAException::class)
        private fun getEasyMultiplier(raw: String?): Double {
            if (raw.isNullOrBlank()) return 1.0

            val easy = raw.toDoubleOrNull()
                ?: throw MRAException(MRAException.Type.RATING_Parameter_EasyError)

            if (easy > 10.0) throw MRAException(MRAException.Type.RATING_Parameter_EasyTooLarge)
            if (easy < 0.0) throw MRAException(MRAException.Type.RATING_Parameter_EasyTooSmall)

            return easy
        }
    }
}
