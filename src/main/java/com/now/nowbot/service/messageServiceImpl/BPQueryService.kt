package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.dao.BindDao
import com.now.nowbot.model.LazerMod
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.json.LazerScore
import com.now.nowbot.model.json.MicroUser
import com.now.nowbot.model.json.OsuUser
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.messageServiceImpl.BPQueryService.Operator.*
import com.now.nowbot.service.messageServiceImpl.ScorePRService.PanelE5Param
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.service.osuApiService.OsuCalculateApiService
import com.now.nowbot.service.osuApiService.OsuScoreApiService
import com.now.nowbot.service.osuApiService.OsuUserApiService
import com.now.nowbot.throwable.GeneralTipsException
import com.now.nowbot.throwable.TipsException
import com.now.nowbot.throwable.serviceException.BPQueryException
import com.now.nowbot.throwable.serviceException.BPQueryException.*
import com.now.nowbot.throwable.serviceException.BindException
import com.now.nowbot.util.DataUtil
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.command.REG_QUOTATION
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.roundToLong

// @Service("BP_QUERY")
class BPQueryService(
    private var bindDao: BindDao,
    private var beatmapApiService: OsuBeatmapApiService,
    private var calculateApiService: OsuCalculateApiService,
    private var userApiService: OsuUserApiService,
    private var scoreApiService: OsuScoreApiService,
    private var imageService: ImageService,
) : MessageService<BPQueryService.BPQueryParam> {

    data class BPQueryParam(val filter: String, val mode: OsuMode)

    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: MessageService.DataValue<BPQueryParam>
    ): Boolean {
        val matcher = Instruction.BP_QUERY.matcher(messageText)
        return if (matcher.find()) {
            throw TipsException("""
                BQ 功能已正式下线。
                类似的功能已合并到 B 功能，并已支持多条无序匹配！
                试试 !b acc>98 combo>400 rank=s 吧！
            """.trimIndent())
            /*

            val text: String = matcher.group("text") ?: throw NullInput()

            data.value = BPQueryParam(text, OsuMode.getMode(matcher.group("mode")))
            true

             */
        } else {
            false
        }
    }

    override fun HandleMessage(event: MessageEvent, param: BPQueryParam) {
        val bindUser = try {
            bindDao.getBindFromQQ(event.sender.id)
        } catch (e: BindException) {
            throw BindException(BindException.Type.BIND_Me_NotBind)
        }

        val mode = OsuMode.getMode(param.mode, bindUser.osuMode, bindDao.getGroupModeConfig(event))

        val (order, text) = getOrder(param.filter)
        val filters = getAllFilter(text)
        val bests = scoreApiService.getBestScores(bindUser, mode)
        var result = getBP(filters, bests)
        val user = userApiService.getPlayerInfo(bindUser)

        if (result.isEmpty()) {
            event.reply(GeneralTipsException(GeneralTipsException.Type.G_Null_FilterBP, user.username))
            return
        }

        order?.let {
            result = if (it.asc) {
                result.sortedBy(order::sort)
            } else {
                result.sortedByDescending(order::sort)
            }
        }

        // ContextUtil.setContext("breakApplySR", true)
        val image = if (result.size == 1) {
            val e5Param = getScore4PanelE5(user, result.first(), beatmapApiService, calculateApiService)

            imageService.getPanel(e5Param.toMap(), "E5")
        } else {
            val indexMap = bests.mapIndexed { i, s -> s.scoreID to i }.toMap()
            val ranks = result.map { indexMap[it.scoreID]!! + 1 }
            val body = mapOf("user" to user, "scores" to result, "rank" to ranks, "panel" to "BQ")

            imageService.getPanel(body, "A4")
        }
        event.reply(image)
    }

    data class Order(val key: String, val asc: Boolean) {
        val p: Param = support.firstOrNull { it.key == key } ?: throw UnsupportedOrderKey(key)
        fun sort(score: LazerScore): Int {
            return when (p) {
                Param.Star -> score.beatMap.starRating * 100
                Param.Bpm -> 100 * (score.beatMap.BPM ?: 0f)
                Param.Length -> score.beatMap.hitLength ?: 0
                Param.AR -> 100 * (score.beatMap.AR ?: 0f)
                Param.OD -> 100 * (score.beatMap.OD ?: 0f)
                Param.CS -> 100 * (score.beatMap.CS ?: 0f)
                Param.HP -> 100 * (score.beatMap.HP ?: 0f)
                Param.PerformancePoint -> score.getPP()
                Param.Combo -> score.totalCombo
                Param.Accuracy -> 10000 * score.accuracy
                Param.Perfect -> score.statistics.perfect
                Param.Great -> score.statistics.great
                Param.Good -> score.statistics.good
                Param.Ok -> score.statistics.ok
                Param.Meh -> score.statistics.meh
                Param.Miss -> score.statistics.miss
                Param.Index -> score.weight!!.index
                else -> 0
            }.toInt()
        }

        companion object {
            val support = listOf(
                Param.Star,
                Param.Bpm,
                Param.Length,
                Param.AR,
                Param.OD,
                Param.CS,
                Param.HP,
                Param.PerformancePoint,
                Param.Combo,
                Param.Accuracy,
                Param.Perfect,
                Param.Great,
                Param.Good,
                Param.Ok,
                Param.Meh,
                Param.Miss,
                Param.Index,
            )
        }
    }

    enum class ClientVersion {
        STABLE, LAZER
    }

    enum class Operator(val op: String) {
        // 等于
        EQ("="),

        // 不等于
        NE("!="),

        // 大于
        GT(">"),

        // 大于等于
        GE(">="),

        // 小于
        LT("<"),

        // 小于等于
        LE("<="),
    }

    enum class Param(
        val key: String,
        val filter: (Triple<Operator, String, LazerScore>) -> Boolean,
        private vararg val enabledOperator: Operator
    ) {
        Mapper("mapper", { (op, v, s) ->
            // 对字符串的 == / != 操作转为 包含 / 不包含
            val name = v.replace("$", " ")
            if (op == EQ) {
                s.beatMapSet.creator.contains(name, true)
            } else {
                !s.beatMapSet.creator.contains(name, true)
            }
        }, EQ, NE),
        Name("name", { (op, v, s) ->
            val name = v.replace("$", " ")
            val hasName = s.beatMapSet.title.contains(name, true) || s.beatMapSet.titleUnicode.contains(name, true)
            if (op == EQ) {
                hasName
            } else {
                hasName.not()
            }
        }, EQ, NE),
        Artist("artist", { (op, v, s) ->
            val artist = v.replace("$", " ")
            val hasArtist =
                s.beatMapSet.artist.contains(artist, true) || s.beatMapSet.artistUnicode.contains(artist, true)
            if (op == EQ) {
                hasArtist
            } else {
                hasArtist.not()
            }
        }, EQ, NE),
        Star("star", { (op, v, s) ->
            compare(s.beatMap.starRating, v, op)
        }, EQ, NE, GT, GE, LT, LE),
        ScoreID("score", { (op, v, s) ->
            val score = try {
                v.filter { it.isDigitOrDot() }.toDouble().roundToLong()
            } catch (_: Exception) {
                throw UnsupportedScoreValue(v)
            }

            compare(s.score, score, op)
        }, EQ, NE, GT, GE, LT, LE),
        Index("index", { (op, v, s) ->
            val index = try {
                v.filter { it.isDigitOrDot() }.toDouble().roundToInt() - 1 //自然数是 1-100，不是计算机需要的0-99
            } catch (_: Exception) {
                throw UnsupportedIndexValue(v)
            }

            compare(s.weight!!.index, index, op)
        }, EQ, NE, GT, GE, LT, LE),
        AR("ar", { (op, v, s) ->
            compare(s.beatMap.AR!!, v, op)
        }, EQ, NE, GT, GE, LT, LE),
        OD("od", { (op, v, s) ->
            compare(s.beatMap.OD!!, v, op)
        }, EQ, NE, GT, GE, LT, LE),
        CS("cs", { (op, v, s) ->
            compare(s.beatMap.CS!!, v, op)
        }, EQ, NE, GT, GE, LT, LE),
        HP("hp", { (op, v, s) ->
            compare(s.beatMap.HP!!, v, op)
        }, EQ, NE, GT, GE, LT, LE),
        PerformancePoint("pp", { (op, v, s) ->
            compare(s.getPP(), v, op)
        }, EQ, NE, GT, GE, LT, LE),
        Rank("rank", { (op, v, s) ->
            val scoreRank = getRankNumber(s.rank)
            val rank = getRankNumber(v)

            compare(scoreRank, rank, op)
        }, EQ, NE, GT, GE, LT, LE),
        Accuracy("acc", { (op, v, s) ->
            var acc = v.filter { it.isDigitOrDot() }.toDouble()
            if (acc > 100.0) acc /= 10000
            else if (acc > 1.0) acc /= 100

            compare(s.accuracy, acc, op)
        }, EQ, NE, GT, GE, LT, LE),
        Bpm("bpm", { (op, v, s) ->
            compare(s.beatMap.BPM!!, v, op)
        }, EQ, NE, GT, GE, LT, LE),
        Combo("combo", { (op, v, s) ->
            compare(s.maxCombo, v, op)
        }, EQ, NE, GT, GE, LT, LE),
        Length("length", { (op, v, s) ->
            compare(s.beatMap.hitLength!!, v, op)
        }, EQ, NE, GT, GE, LT, LE),
        Perfect("perfect", { (op, v, s) ->
            val perfect = s.statistics.perfect
            compare(perfect, v, op)
        }, EQ, NE, GT, GE, LT, LE),
        Great("great", { (op, v, s) ->
            val great = s.statistics.great
            compare(great, v, op)
        }, EQ, NE, GT, GE, LT, LE),
        Good("good", { (op, v, s) ->
            val good = s.statistics.good
            compare(good, v, op)
        }, EQ, NE, GT, GE, LT, LE),
        Ok("ok", { (op, v, s) ->
            val ok = s.statistics.ok
            compare(ok, v, op)
        }, EQ, NE, GT, GE, LT, LE),
        Meh("meh", { (op, v, s) ->
            val meh = s.statistics.meh
            compare(meh, v, op)
        }, EQ, NE, GT, GE, LT, LE),
        Miss("miss", { (op, v, s) ->
            val misses = s.statistics.miss
            compare(misses, v, op)
        }, EQ, NE, GT, GE, LT, LE),
        Mods("mod", { (op, v, s) ->
            // mod 处理是 = 为严格等于, > 为包含mod
            val mods = LazerMod.getModsValue(v)

            val scoreMods = LazerMod.getModsValue(s.mods)
            when (op) {
                EQ -> mods == scoreMods
                NE -> mods and scoreMods == 0
                GT -> mods and scoreMods == mods
                else -> throw UnsupportedModOperator(op.op)
            }
        }, EQ, NE, GT),
        Rate("rate", { (op, v, s) ->
            val value = v.filter { it.isDigitOrDot() }.toDouble()
            val rate = if (s.statistics.great == 0) {
                Double.MAX_VALUE
            } else {
                1.0 * s.statistics.perfect / s.statistics.great
            }

            compare(rate, value, op)
        }, EQ, NE, GT, GE, LT, LE),
        Client("client", { (op, v, s) ->
            val it = when (v) {
                "stable", "s" -> ClientVersion.STABLE
                else -> ClientVersion.LAZER
            }
            val that = when (s.isLazer) {
                true -> ClientVersion.LAZER
                else -> ClientVersion.STABLE
            }

            when (op) {
                EQ -> it == that
                else -> it !== that
            }
        }, EQ, NE),
        ;

        operator fun invoke(operator: Operator, value: String): (LazerScore) -> Boolean {
            if (operator !in enabledOperator) throw UnsupportedOperator(key, operator.op)
            return { score -> filter(Triple(operator, value, score)) }
        }
    }

    private fun getBP(filters: List<(LazerScore) -> Boolean>, scores: List<LazerScore>): List<LazerScore> {
        // bp 有 pp，所以只需要查星数
        calculateApiService.applyBeatMapChanges(scores)
        calculateApiService.applyStarToScores(scores)

        // 处理麻婆, 与 set 主不一致
        val mapperMap = mutableMapOf<Int, Long>()
        scores.forEachIndexed { index, score ->
            if (score.beatMap.mapperID != score.beatMapSet.creatorID) {
                mapperMap[index] = score.beatMap.mapperID
            }
        }
        if (mapperMap.isNotEmpty()) {
            val rawMapperMap = mutableMapOf<Long, MicroUser>()
            mapperMap.entries.chunked(50)
                .map { mappers -> mappers.map { it.value } }
                .forEach { ids ->
                    val users = userApiService.getUsers(ids)
                    users.forEach { rawMapperMap[it.id] = it }
                }
            mapperMap.forEach { (index, mapperID) ->
                val score = scores[index]
                val mapper = rawMapperMap[mapperID] ?: return@forEach
                score.beatMapSet.creator = mapper.userName
                score.beatMapSet.creatorID = mapperID
            }
        }
        return scores.filter { s -> filters.all { it(s) } }
    }

    companion object {
        private val operate: Regex = "(\\S+?)([<>＜＞][=＝]?|[=＝]|[!！][=＝])(\\S+(\\.\\d+)?)".toRegex()

        private val order: Regex = "\\s*(?i)order by (?<key>\\w+) (asc|(?<desc>desc))?\\s*$".toRegex()

        private val split: Regex = "(\\s+)|[|,，]".toRegex()

        // 最好还是支持一下全角符号，总是有用户输入
        private fun String.standardised(): String {
            return this
                .replace('＜', '<')
                .replace('＞', '>')
                .replace("≤", "<=")
                .replace("≥", ">=")
                .replace('＝', '=')
                .replace('！', '!')
                .replace("≠", "!=")
                .replace(REG_QUOTATION, "\"")
        }

        private fun String.getOperator(): Triple<String, Operator, String> {
            val m = operate.matchEntire(this) ?: throw ParsingBlockException()
            val operator = when (m.groupValues[2].standardised()) {
                EQ.op -> EQ
                NE.op -> NE
                GT.op -> GT
                GE.op -> GE
                LT.op -> LT
                LE.op -> LE
                else -> throw UnsupportedOperator(m.groupValues[0], m.groupValues[2])
            }
            return Triple(m.groupValues[1], operator, m.groupValues[3])
        }

        private fun getFilter(cmd: String): (LazerScore) -> Boolean {
            val (key, operator, value) = cmd.getOperator()
            return when (key) {
                Param.Mapper.key, "u", "creator" -> Param.Mapper(operator, value)
                Param.ScoreID.key, "s" -> Param.ScoreID(operator, value)
                Param.Name.key, "t", "title", "song" -> Param.Name(operator, value)
                Param.Artist.key, "f" -> Param.Artist(operator, value)
                Param.Star.key, "sr" -> Param.Star(operator, value)
                Param.Index.key, "i" -> Param.Index(operator, value)
                Param.AR.key -> Param.AR(operator, value)
                Param.OD.key -> Param.OD(operator, value)
                Param.CS.key, "key", "keys", "k" -> Param.CS(operator, value)
                Param.HP.key -> Param.HP(operator, value)
                Param.PerformancePoint.key, "p" -> Param.PerformancePoint(operator, value)
                Param.Rank.key, "r" -> Param.Rank(operator, value)
                Param.Length.key, "l" -> Param.Length(operator, value)
                Param.Bpm.key, "b" -> Param.Bpm(operator, value)
                Param.Accuracy.key, "acc", "a" -> Param.Accuracy(operator, value)
                Param.Combo.key, "c" -> Param.Combo(operator, value)
                Param.Perfect.key, "320", "305", "pf" -> Param.Perfect(operator, value)
                Param.Great.key, "300", "良", "gr" -> Param.Great(operator, value)
                Param.Good.key, "200", "gd" -> Param.Good(operator, value)
                Param.Ok.key, "100", "150", "可", "ok", "bd", "bad" -> Param.Ok(operator, value)
                Param.Meh.key, "50", "pr", "poor" -> Param.Meh(operator, value)
                Param.Miss.key, "0", "ms", "x" -> Param.Miss(operator, value)
                Param.Mods.key, "m", "mod" -> Param.Mods(operator, value)
                Param.Rate.key, "e", "pm" -> Param.Rate(operator, value)
                Param.Client.key, "z", "v", "version" -> Param.Client(operator, value)
                else -> throw UnsupportedKey(key)
            }
        }

        private fun getAllFilter(text: String): List<(LazerScore) -> Boolean> {
            val result = mutableListOf<(LazerScore) -> Boolean>()

            text.process()
                .split(split)
                .filter { it.isNotBlank() }
                .forEach {
                    val f = try {
                        getFilter(it.trim().lowercase())
                    } catch (e: BPQueryException) {
                        e.expression = it
                        throw e
                    }
                    result.add(f)
                }
            return result
        }

        fun getOrder(text: String): Pair<Order?, String> {
            val matcher = order.find(text) ?: return null to text
            val key = matcher.groups["key"]!!.value
            val asc = matcher.groups["desc"] == null

            return Order(key, asc) to text.substring(0, matcher.range.first)
        }

        @Throws(BPQueryException::class)
        private fun getRankNumber(text: String): Int {
            val rankArray = arrayOf("F", "D", "C", "B", "A", "S", "SH", "X", "XH")
            val rank = text.uppercase().replace("SS", "X", ignoreCase = true)

            for (i in rankArray.indices) {
                if (rankArray[i] == rank) return i
            }

            throw UnsupportedRankValue(rank)
        }

        /**
         * 将双引号内的空格替换为 $ 符号
         */
        private fun String.process(): String {
            val quoteCount = this.count { it == '"' }
            when {
                quoteCount == 0 -> return this
                quoteCount % 2 != 0 -> {
                    val end = this.substring(min(4, this.length))
                    throw ParsingQuoteException(end)
                }
            }
            val result = StringBuilder()
            var insideQuotes = false
            val currentContent = StringBuilder()
            this.forEach {
                if (it == '"') {
                    if (insideQuotes) {
                        result.append(currentContent.toString().replace(" ", "$"))
                        currentContent.clear()
                    }
                    insideQuotes = !insideQuotes
                } else {
                    if (insideQuotes) {
                        // 如果在双引号内，收集内容
                        currentContent.append(it)
                    } else {
                        // 如果不在双引号内，正常添加字符到结果
                        result.append(it)
                    }
                }
            }
            return result.toString()
        }

        private fun Double.isEqual(other: Double): Boolean {
            return this - other in -1E-7..1E-7
        }

        private fun Double.isGreaterOrEqual(other: Double): Boolean {
            return this - other > -1E-7
        }

        private fun Double.isLessOrEqual(other: Double): Boolean {
            return this - other < 1E-7
        }

        private fun compare(it: Number, that: String, operator: Operator): Boolean {
            return compare(it, that.toDouble(), operator)
        }

        private fun compare(it: Number, that: Number, operator: Operator): Boolean {
            val i = it.toDouble()
            val t = that.toDouble()

            return when (operator) {
                EQ -> i.isEqual(t)
                NE -> !i.isEqual(t)
                GT -> i > t
                GE -> i.isGreaterOrEqual(t)
                LT -> i < t
                LE -> i.isLessOrEqual(t)
            }
        }

        private fun compare(it: Int, that: String, operator: Operator): Boolean {
            return compare(it.toLong(), that, operator)
        }

        private fun compare(it: Long, that: String, operator: Operator): Boolean {
            val t = that.filter { it.isDigitOrDot() }.toDouble().roundToLong()

            return when (operator) {
                EQ -> it == t
                NE -> it != t
                GT -> it > t
                GE -> it >= t
                LT -> it < t
                LE -> it <= t
            }
        }

        private fun Char.isDigitOrDot(): Boolean {
            return this.isDigit() || this == '.'
        }


        // 这个是不带 applyBeatMapChanges 的版本
        private fun getScore4PanelE5(
            user: OsuUser,
            score: LazerScore,
            beatmapApiService: OsuBeatmapApiService,
            calculateApiService: OsuCalculateApiService,
        ): PanelE5Param {
            beatmapApiService.applyBeatMapExtend(score)

            val beatmap = score.beatMap
            val original = DataUtil.getOriginal(beatmap)

            calculateApiService.applyPPToScore(score)
            // calculateApiService.applyBeatMapChanges(score)
            calculateApiService.applyStarToScore(score)

            val attributes = calculateApiService.getScoreStatisticsWithFullAndPerfectPP(score)

            val density = beatmapApiService.getBeatmapObjectGrouping26(beatmap)
            val progress = beatmapApiService.getPlayPercentage(score)

            return PanelE5Param(user, score, null, density, progress, original, attributes, "BQ")
        }
    }
}
