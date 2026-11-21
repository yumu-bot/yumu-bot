package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.dao.ServiceCallStatisticsDao
import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.model.match.Match
import com.now.nowbot.model.match.MatchRating
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.qq.message.MessageChain.MessageChainBuilder
import com.now.nowbot.qq.tencent.TencentMessageService
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.service.osuApiService.OsuCalculateApiService
import com.now.nowbot.service.osuApiService.OsuMatchApiService

import com.now.nowbot.throwable.botException.MRAException
import com.now.nowbot.throwable.botRuntimeException.IllegalArgumentException
import com.now.nowbot.throwable.botRuntimeException.IllegalStateException
import com.now.nowbot.throwable.botRuntimeException.NetworkException
import com.now.nowbot.throwable.botRuntimeException.NoSuchElementException
import com.now.nowbot.util.AsyncMessageUtil
import com.now.nowbot.util.AsyncMethodExecutor
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.MatchUtil.toMatch
import com.now.nowbot.util.OfficialInstruction
import com.now.nowbot.util.command.ExtractType
import com.now.nowbot.util.command.FLAG_ANY
import com.now.nowbot.util.command.MatchIDExtractor
import com.now.nowbot.util.command.MatchParamExtractor
import com.now.nowbot.util.command.REGEX_SEPARATOR
import com.now.nowbot.util.command.RoomIDExtractor
import com.now.nowbot.util.command.StripPipeline
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.*
import java.util.regex.Matcher
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds

@Service("MU_RATING") class MuRatingService(
    private val matchApiService: OsuMatchApiService,
    private val beatmapApiService: OsuBeatmapApiService,
    private val calculateApiService: OsuCalculateApiService,
    private val imageService: ImageService,
    private val dao: ServiceCallStatisticsDao
) : MessageService<MuRatingService.MuRatingPanelParam>, TencentMessageService<MuRatingService.MuRatingPanelParam> {

    data class MuRatingPanelParam(
        val match: Match,
        val param: MatchRating.RatingParam,
        val isUU: Boolean = false
    )

    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: DataValue<MuRatingPanelParam>,
    ): Boolean {
        val matcher = Instruction.MU_RATING.matcher(messageText)

        if (!matcher.find()) {
            return false
        }

        data.value = getMuRatingParam(event, matcher, matchApiService, dao)
        return true
    }

    @Throws(Throwable::class) override fun handleMessage(event: MessageEvent, param: MuRatingPanelParam): ServiceCallStatistic? {
        val mr : MatchRating

        try {
            mr = calculate(param, beatmapApiService, calculateApiService)
        } catch (e: MRAException) {
            throw e
        } catch (e: Exception) {
            log.error("木斗力：数据计算失败", e)
            throw IllegalStateException.Calculate("木斗力")
        }

        if (mr.rounds.isEmpty()) {
            throw NoSuchElementException.MatchRound()
        }

        if (param.isUU) {
            mr.insertMicroUserToScores()
            val str = parseCSA(mr)

            try {
                event.replyAndRecallAsync(str, 60.seconds)
            } catch (e: Exception) {
                log.error("木斗力文字版：发送失败", e)
                throw IllegalStateException.Send("木斗力")
            }
        } else {
            val image: ByteArray = imageService.getPanel(mr, "C")

            try {
                event.replyAsync(image)
            } catch (e: Exception) {
                log.error("木斗力：发送失败", e)
                throw IllegalStateException.Send("木斗力")
            }
        }
        return ServiceCallStatistic.building(event) {
            setParam(mapOf(
                "mids" to listOf(mr.match.id),
                "uids" to mr.players.keys.distinct(),
                "bids" to mr.rounds.map { it.beatmapID }.distinct(),
                "sids" to mr.rounds.mapNotNull { it.beatmap?.beatmapsetID }.distinct(),
                "modes" to mr.rounds.map { it.mode.modeValue }.distinct()
            ))
        }
    }

    override fun accept(event: MessageEvent, messageText: String): MuRatingPanelParam? {
        val matcher = OfficialInstruction.MU_RATING.matcher(messageText)

        if (!matcher.find()) {
            return null
        }

        // 官方 bot 的消息没有一问一答的通道，编号有歧义时直接按房间处理
        return getMuRatingParam(event, matcher, matchApiService, dao, allowAsk = false)
    }

    override fun reply(event: MessageEvent, param: MuRatingPanelParam): MessageChain? {
        val mr: MatchRating = calculate(param, beatmapApiService, calculateApiService)
        return MessageChainBuilder().addImage(imageService.getPanel(mr, "C")).build()
    }

    private fun parseCSA(mr: MatchRating): String {
        val data = mr.playerDataList

        // 结果数据
        val sb = StringBuilder()
        sb.append(mr.match.statistics.name).append("\n").append(mr.teamPointMap["red"]).append(" : ")
            .append(mr.teamPointMap["blue"]).append("\n").append("mp").append(mr.match.statistics.matchID).append(" ")
            .append(mr.isTeamVs).append("\n")

        for (p in data) {
            sb.append(
                String.format(
                    "#%d [%.2f] %s (%s)",
                    p.ranking,
                    p.mra,
                    p.player.username,
                    p.team!!.uppercase(Locale.getDefault()),
                )
            ).append(" ").append(
                    String.format(
                        "%dW-%dL %d%% (%.2fM) [%.2f] [%s | %s]",
                        p.win,
                        p.lose,
                        (p.win.toDouble() * 100 / (p.win + p.lose)).roundToInt(),
                        p.total / 1000000.0,
                        p.rws * 100.0,
                        p.playerClass!!.english,
                        p.playerClass!!.chinese,
                    )
                ).append("\n\n")
        }
        return sb.toString()
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(MuRatingService::class.java)

        /** 纯数字编号：没有 m / r 前缀时，同一个数字既可能是比赛也可能是房间。最多 18 位，超出 long 范围的当成无法识别的参数。 */
        private val NUMBER_REGEX = Regex("""(?<!\d)-?\d{1,18}(?!\d)""")

        /**
         * 小于这个数的纯数字不当编号，而是 skip / ignore。
         *
         * 和旧版一致：`!ra 1 0` 是「上一场比赛，热手 1 场、表演赛 0 场」。
         * 编号本身很小（老比赛确实有两位数的 MID）时用 `m42` / `r42` 明确指定即可。
         */
        private const val MIN_MATCH_ID = 100L

        /** 编号的归属：旧版比赛，还是新版房间。 */
        internal enum class MatchTarget { MATCH, ROOM }

        /** 解析出来、还没去查比赛的比赛参数。 */
        internal data class ParsedMuRating(
            /** 用户自己写的编号；没有写时为 null，由群记录兜底。 */
            val matchID: Long?,
            /** 编号的归属；只有纯数字时为 null，表示要两个接口都查。 */
            val target: MatchTarget?,
            val skip: Int,
            val ignore: Int,
            val remove: List<Int>,
            val easy: Double,
            val delete: Boolean,
            val rematch: Boolean,
        )

        /**
         * 提取通用方法：将消息匹配变成 MRA 参数。
         *
         * 指令只把 `!ra` 之后的整段文本收进 `any`，具体参数交给剥离器：
         * - 编号：`m123` / `mid=123` / `match=123` 是旧版比赛，`r123` / `rid=123` / `room=123` 是新版房间，
         *   纯数字则两个接口都查一次；
         * - 其它参数：`skip=1`、`ignore=0`、`remove=1,2`、`e1.5`，开关固定写成 `:r` / `:f`，顺序随意；
         * - 剩下的纯数字按位置解释：没有显式编号时，第一个是编号，之后是 skip / ignore；
         *   小于 [MIN_MATCH_ID] 的数字不算编号（见该常量的说明），直接从 skip 开始。
         *
         * 纯数字两个接口都能查到（编号本身有歧义）时，会问一次：
         * 回复 `1` 查旧版比赛、`2` 查新版房间，30 秒不回复按房间处理。
         *
         * @param allowAsk 是否允许在一问一答里询问编号的归属。官方 bot 的消息没有回复通道，传 `false` 默认按房间处理。
         * @throws MRAException 参数不合法（Easy 倍率越界等）
         */
        @Throws(MRAException::class)
        fun getMuRatingParam(
            event: MessageEvent,
            matcher: Matcher,
            matchApiService: OsuMatchApiService,
            dao: ServiceCallStatisticsDao? = null,
            allowAsk: Boolean = true,
        ): MuRatingPanelParam {
            val parsed = parseMuRating(matcher.group(FLAG_ANY).orEmpty())

            val matchID = parsed.matchID ?: dao?.getLastMatchID(
                groupID = event.subject.contactID,
                from = LocalDateTime.now().minusHours(24L)
            ) ?: throw IllegalArgumentException.WrongException.MatchID()

            // 只有用户自己输入的编号才值得为歧义问一次；从群记录里翻出来的编号不打断用户
            val match: Match = fetchMatch(
                event = event,
                id = matchID,
                target = parsed.target,
                matchApiService = matchApiService,
                askWhenAmbiguous = allowAsk && parsed.matchID != null,
            )

            val isUU = matcher.namedGroups().containsKey("uu") && matcher.group("uu") != null

            return MuRatingPanelParam(
                match,
                MatchRating.RatingParam(
                    parsed.skip, parsed.ignore, parsed.remove, parsed.easy,
                    delete = parsed.delete,
                    rematch = parsed.rematch,
                ),
                isUU,
            )
        }

        /**
         * 只解析 `!ra` 之后的文本，不访问网络，方便单独验证参数语法。
         *
         * @throws IllegalArgumentException 写了不认识的参数，或者同时写了比赛和房间编号
         * @throws MRAException Easy 倍率越界
         */
        @Throws(MRAException::class)
        internal fun parseMuRating(any: String): ParsedMuRating {
            val context = StripPipeline(any)
                .apply(MatchIDExtractor)
                .apply(RoomIDExtractor)
                .apply(MatchParamExtractor)
                .trim()
                .context

            // 编号：显式的 m / r 只能出现一种，同时写两个分不清要查哪个
            val explicitMatchID = context[ExtractType.MATCH_ID].lastOrNull()
            val explicitRoomID = context[ExtractType.ROOM_ID].lastOrNull()

            if (explicitMatchID != null && explicitRoomID != null) {
                throw IllegalArgumentException.WrongException.MatchAndRoom()
            }

            // 剥完参数之后只应该剩纯数字，其它字符说明写了一个不认识的参数
            val leftover = NUMBER_REGEX.replace(context.text, " ").trim()

            if (leftover.isNotEmpty()) {
                throw IllegalArgumentException.WrongException.MatchParam(leftover)
            }

            val target = when {
                explicitMatchID != null -> MatchTarget.MATCH
                explicitRoomID != null -> MatchTarget.ROOM
                else -> null
            }

            // 纯数字按位置解释：编号、skip、ignore
            val positional = NUMBER_REGEX.findAll(context.text)
                .mapNotNull { it.value.toLongOrNull() }
                .toList()

            val explicitID = explicitMatchID ?: explicitRoomID

            var matchID = explicitID?.toLongOrNull()

            if (explicitID != null && matchID == null) {
                throw IllegalArgumentException.WrongException.MatchParam(explicitID)
            }

            var index = 0

            // 没有显式编号时，只有不小于 [MIN_MATCH_ID] 的第一个纯数字才是编号；
            // 更小的数字按旧约定直接当 skip / ignore，编号走群记录兜底。
            if (matchID == null && index < positional.size && positional[index] >= MIN_MATCH_ID) {
                matchID = positional[index++]
            }

            var skip = if (index < positional.size) positional[index++].toInt() else 0
            var ignore = if (index < positional.size) positional[index++].toInt() else 0

            if (index < positional.size) {
                throw IllegalArgumentException.WrongException.MatchParam(positional.drop(index).joinToString(" "))
            }

            // 显式写了参数名时以参数名为准
            context[ExtractType.SKIP].lastOrNull()?.toIntOrNull()?.let { skip = it }
            context[ExtractType.IGNORE].lastOrNull()?.toIntOrNull()?.let { ignore = it }

            return ParsedMuRating(
                matchID = matchID,
                target = target,
                skip = skip,
                ignore = ignore,
                remove = context[ExtractType.REMOVE]
                    .flatMap { it.split(REGEX_SEPARATOR) }
                    .mapNotNull { it.trim().toIntOrNull() },
                easy = parseEasy(context[ExtractType.EASY].lastOrNull()),
                // 写了 :r / :f 才剔除，和旧版一致
                delete = !context.exists(ExtractType.FAILED),
                rematch = !context.exists(ExtractType.REMATCH),
            )
        }

        /**
         * Easy 倍率。没写就是 1.0，写了就必须落在 0 ~ 10 之间。
         */
        private fun parseEasy(raw: String?): Double {
            if (raw.isNullOrBlank()) return 1.0

            val easy = raw.toDoubleOrNull() ?: 1.0

            if (easy > 10.0) throw MRAException(MRAException.Type.RATING_Parameter_EasyTooLarge)
            if (easy < 0.0) throw MRAException(MRAException.Type.RATING_Parameter_EasyTooSmall)

            return easy
        }

        /**
         * 按编号取比赛。
         *
         * - [target] 为 [MatchTarget.MATCH] / [MatchTarget.ROOM] 时只查对应的一个接口；
         * - 为 null（用户只写了纯数字）时先确认两边是否存在：只有一边存在就用那一边，
         *   两边都存在且 [askWhenAmbiguous] 为真时问一次，都没有则抛 [NoSuchElementException.MatchOrRoom]。
         *
         * 存在性检查用轻量的接口（比赛只取第一页、房间只取房间信息），问清楚之后再拉完整数据，
         * 免得编号有歧义时白拉一整场比赛。只有 404 才算「不存在」，限流、超时之类的网络错误照常往上抛。
         */
        private fun fetchMatch(
            event: MessageEvent,
            id: Long,
            target: MatchTarget?,
            matchApiService: OsuMatchApiService,
            askWhenAmbiguous: Boolean,
        ): Match {
            when (target) {
                MatchTarget.MATCH -> return try {
                    matchApiService.getMatch(id, 10)
                } catch (_: NetworkException.MatchException.NotFound) {
                    throw NoSuchElementException.Match(id)
                }

                MatchTarget.ROOM -> return try {
                    matchApiService.getRoom(id).toMatch()
                } catch (_: NetworkException.MatchException.NotFound) {
                    throw NoSuchElementException.Room(id)
                }

                null -> {}
            }

            val (hasMatch, hasRoom) = AsyncMethodExecutor.awaitPair(
                { matchExists(id, matchApiService) },
                { roomExists(id, matchApiService) },
            )

            val chosen = when {
                hasMatch && hasRoom ->
                    // 两个编号都存在：问一次。没人回答就按房间算。
                    if (askWhenAmbiguous) askMatchTarget(event, id) else MatchTarget.MATCH

                hasMatch -> MatchTarget.MATCH
                hasRoom -> MatchTarget.ROOM

                else -> throw NoSuchElementException.MatchOrRoom(id)
            }

            return try {
                when (chosen) {
                    MatchTarget.MATCH -> matchApiService.getMatch(id, 10)
                    MatchTarget.ROOM -> matchApiService.getRoom(id).toMatch()
                }
            } catch (_: NetworkException.MatchException.NotFound) {
                // 探测和取数据之间比赛刚好过期
                throw NoSuchElementException.MatchOrRoom(id)
            }
        }

        /** 旧版比赛是否存在：只取第一页，比拉完整场比赛便宜得多。 */
        private fun matchExists(id: Long, matchApiService: OsuMatchApiService): Boolean = try {
            matchApiService.getMatch(id, 0)
            true
        } catch (_: NetworkException.MatchException.NotFound) {
            false
        }

        /** 新版房间是否存在：只取房间信息，不用拉 events。 */
        private fun roomExists(id: Long, matchApiService: OsuMatchApiService): Boolean = try {
            matchApiService.getRoomInfo(id)
            true
        } catch (_: NetworkException.MatchException.NotFound) {
            false
        }

        /**
         * 同一个数字既是旧版比赛又是新版房间时问一次，返回用户选择的归属。
         *
         * 回复 `1` 查旧版比赛，回复 `2` 查新版房间；30 秒内没有回复按房间处理。
         * 回复了别的文字、或者被新指令打断，同样按房间处理。
         */
        private fun askMatchTarget(event: MessageEvent, id: Long): MatchTarget {
            var target = MatchTarget.ROOM

            AsyncMessageUtil.doubleCheck(
                event = event,
                keywords = listOf("1", "2"),
                onCheck = {
                    event.reply("""
                        编号 $id 既是旧版比赛，也是新版房间。
                        回复 1 查询旧版比赛，回复 2 查询新版房间。
                        30 秒内不回复，默认查询新版房间。
                    """.trimIndent())
                },
                onOverTime = {},
                onFailure = {},
                onSuccess = { reply ->
                    target = if (reply.rawMessage.trim().startsWith("1")) MatchTarget.MATCH else MatchTarget.ROOM
                },
                timeout = 30.seconds,
            )

            return target
        }

        @Throws(MRAException::class) fun calculate(
            matchID: Long,
            skip: Int,
            ignore: Int,
            remove: List<Int>?,
            easy: Double,
            failed: Boolean,
            rematch: Boolean,
            matchApiService: OsuMatchApiService,
            beatmapApiService: OsuBeatmapApiService,
            calculateApiService: OsuCalculateApiService,
        ): MatchRating {
            val match: Match = matchApiService.getMatch(matchID, 10)

            val param = MuRatingPanelParam(match, MatchRating.RatingParam(skip, ignore, remove, easy, failed, rematch), false)

            return calculate(param, beatmapApiService, calculateApiService)
        }

        @JvmStatic @Throws(MRAException::class)
        fun calculate(
            panel: MuRatingPanelParam,
            beatmapApiService: OsuBeatmapApiService,
            calculateApiService: OsuCalculateApiService,
        ): MatchRating {
            if (panel.param.skip < 0) throw MRAException(MRAException.Type.RATING_Parameter_SkipError)
            if (panel.param.ignore < 0) throw MRAException(MRAException.Type.RATING_Parameter_SkipEndError)

            val mr = MatchRating(
                panel.match, panel.param, beatmapApiService, calculateApiService
            )

            mr.calculate()

            return mr
        }
    }
}
