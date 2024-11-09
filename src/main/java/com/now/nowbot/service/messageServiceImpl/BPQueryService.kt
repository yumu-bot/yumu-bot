package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.dao.BindDao
import com.now.nowbot.model.LazerMod
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.json.LazerScore
import com.now.nowbot.model.json.MicroUser
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.messageServiceImpl.BPQueryService.Operator.*
import com.now.nowbot.service.messageServiceImpl.ScorePRService.Companion.getScore4PanelE5
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.service.osuApiService.OsuScoreApiService
import com.now.nowbot.service.osuApiService.OsuUserApiService
import com.now.nowbot.throwable.GeneralTipsException
import com.now.nowbot.throwable.serviceException.BQQueryException
import com.now.nowbot.throwable.serviceException.BQQueryException.*
import com.now.nowbot.util.ContextUtil
import com.now.nowbot.util.Instruction
import org.springframework.stereotype.Service
import kotlin.math.min

@Service("BP_QUERY")
class BPQueryService(
    private var beatmapApiService: OsuBeatmapApiService,
    private var userApiService: OsuUserApiService,
    private var bindDao: BindDao,
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
            val text: String = matcher.group("text") ?: throw NullInput()

            data.value = BPQueryParam(text, OsuMode.getMode(matcher.group("mode")))
            true
        } else {
            false
        }
    }


    override fun HandleMessage(event: MessageEvent, param: BPQueryParam) {
        val bindUser = bindDao.getUserFromQQ(event.sender.id)
        val mode = if (OsuMode.isDefaultOrNull(param.mode)) {
            bindUser.osuMode
        } else {
            param.mode
        }
        val filters = getAllFilter(param.filter)
        val bpList = scoreApiService.getBestScores(bindUser, mode, 0, 100)
        val result = getBP(filters, bpList)
        val user = userApiService.getPlayerInfo(bindUser)

        if (result.isEmpty()) {
            event.reply(GeneralTipsException(GeneralTipsException.Type.G_Null_FilterBP, user.username))
            return
        }

        ContextUtil.setContext("breakApplySR", true)
        val image = if (result.size == 1) {
            val score = result.first()
            val e5Param = getScore4PanelE5(user, score, "BQ", beatmapApiService)
            imageService.getPanel(e5Param.toMap(), "E5")
        } else {
            val indexMap = bpList.mapIndexed { i, s -> s.scoreID to i }.toMap()
            val ranks = result.map { indexMap[it.scoreID]!! + 1 }
            imageService.getPanelA4(user, result, ranks, "BQ")
        }
        event.reply(image)
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
                v.toLong()
            } catch (_: Exception) {
                throw UnsupportedScoreValue(v)
            }

            compare(s.score, score, op)
        }, EQ, NE, GT, GE, LT, LE),
        Index("index", { (op, v, s) ->
            val index = try {
                v.toInt() - 1 //自然数是 1-100，不是计算机需要的0-99
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
        Rank("rank", { (op, v, s) ->
            val scoreRank = getRankNumber(s.rank)
            val rank = getRankNumber(v)

            compare(scoreRank, rank, op)
        }, EQ, NE, GT, GE, LT, LE),
        Accuracy("acc", { (op, v, s) ->
            var acc = v.toDouble()
            if (acc > 1.0) acc /= 100

            compare(s.accuracy, acc, op)
        }, EQ, NE, GT, GE, LT, LE),
        Bpm("bpm", { (op, v, s) ->
            compare(s.beatMap.BPM!!, v, op)
        }, EQ, NE, GT, GE, LT, LE),
        Combo("combo", { (op, v, s) ->
            compare(s.beatMap.maxCombo!!, v, op)
        }, EQ, NE, GT, GE, LT, LE),
        Length("length", { (op, v, s) ->
            compare(s.beatMap.hitLength!!, v, op)
        }, EQ, NE, GT, GE, LT, LE),
        Miss("miss", { (op, v, s) ->
            val misses = (s.statistics.miss ?: 0)
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
            val value = v.toDouble()
            val rate = if (s.statistics.great == 0) {
                Double.MAX_VALUE
            } else {
                1.0 * (s.statistics.perfect ?: 0) / (s.statistics.great ?: 0)
            }

            compare(rate, value, op)
        }, EQ, NE, GT, GE, LT, LE),
        ;

        operator fun invoke(operator: Operator, value: String): (LazerScore) -> Boolean {
            if (operator !in enabledOperator) throw UnsupportedOperator(key, operator.op)
            return { score -> filter(Triple(operator, value, score)) }
        }
    }

    private fun getBP(filters: List<(LazerScore) -> Boolean>, scores: List<LazerScore>): List<LazerScore> {
        // 处理带 mod 的
        beatmapApiService.applySRAndPP(scores)

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
        val pattern: Regex = "(\\S+?)([<>＜＞][=＝]?|[=＝]|[!！][=＝])(\\S+(\\.\\d+)?)".toRegex()

        val split: Regex = "(\\s+)|[|,，]".toRegex()

        // 最好还是支持一下全角符号，总是有用户输入
        private fun String.standardised(): String {
            return this.replace('＜', '<').replace('＞', '>').replace('＝', '=').replace('！', '!')
        }

        private fun String.getOperator(): Triple<String, Operator, String> {
            val m = pattern.matchEntire(this) ?: throw ParsingBlockException()
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
                Param.Mapper.key, "creator" -> Param.Mapper(operator, value)
                Param.ScoreID.key -> Param.ScoreID(operator, value)
                Param.Name.key, "title" -> Param.Name(operator, value)
                Param.Artist.key -> Param.Artist(operator, value)
                Param.Star.key, "sr" -> Param.Star(operator, value)
                Param.Index.key -> Param.Index(operator, value)
                Param.AR.key -> Param.AR(operator, value)
                Param.OD.key -> Param.OD(operator, value)
                Param.CS.key -> Param.CS(operator, value)
                Param.HP.key -> Param.HP(operator, value)
                Param.Rank.key -> Param.Rank(operator, value)
                Param.Length.key -> Param.Length(operator, value)
                Param.Bpm.key -> Param.Bpm(operator, value)
                Param.Accuracy.key, "acc", "a" -> Param.Accuracy(operator, value)
                Param.Combo.key, "c" -> Param.Combo(operator, value)
                Param.Miss.key, "x" -> Param.Miss(operator, value)
                Param.Mods.key, "m" -> Param.Mods(operator, value)
                Param.Rate.key, "p" -> Param.Rate(operator, value)
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
                    } catch (e: BQQueryException) {
                        e.expression = it
                        throw e
                    }
                    result.add(f)
                }
            return result
        }

        private fun getRankNumber(rank: String): Int {
            return when (rank.uppercase()) {
                "F" -> 0
                "D" -> 1
                "C" -> 2
                "B" -> 3
                "A" -> 4
                "S" -> 5
                "SH" -> 6
                "X" -> 7
                "XH" -> 8
                else -> throw UnsupportedRankValue(rank)
            }
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

        fun Double.isEqual(other: Double): Boolean {
            return this - other in -1E-7..1E-7
        }

        fun Double.isGreaterOrEqual(other: Double): Boolean {
            return this - other > -1E-7
        }

        fun Double.isLessOrEqual(other: Double): Boolean {
            return this - other < 1E-7
        }

        fun Float.isEqual(other: Float): Boolean {
            return this - other in -1E-7..1E-7
        }

        fun Float.isGreaterOrEqual(other: Float): Boolean {
            return this - other > -1E-7
        }

        fun Float.isLessOrEqual(other: Float): Boolean {
            return this - other < 1E-7
        }

        fun compare(it: Number, that: String, operator: Operator): Boolean {
            return compare(it, that.toDouble(), operator)
        }

        fun compare(it: Number, that: Number, operator: Operator): Boolean {
            val it = it.toDouble()
            val that = that.toDouble()

            return when (operator) {
                EQ -> it.isEqual(that)
                NE -> ! it.isEqual(that)
                GT -> it > that
                GE -> it.isGreaterOrEqual(that)
                LT -> it < that
                LE -> it.isLessOrEqual(that)
            }
        }

        fun compare(it: Int, that: String, operator: Operator): Boolean {
            return compare(it.toLong(), that, operator)
        }

        fun compare(it: Long, that: String, operator: Operator): Boolean {
            val that = that.toLong()

            return when (operator) {
                EQ -> it == that
                NE -> it != that
                GT -> it > that
                GE -> it >= that
                LT -> it < that
                LE -> it <= that
            }
        }
    }
}