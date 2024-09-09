package com.now.nowbot.service.MessageServiceImpl

import com.now.nowbot.dao.BindDao
import com.now.nowbot.model.JsonData.MicroUser
import com.now.nowbot.model.JsonData.Score
import com.now.nowbot.model.enums.OsuMod
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageServiceImpl.BPQueryService.Companion.isEqual
import com.now.nowbot.service.MessageServiceImpl.BPQueryService.Companion.isGreaterOrEqual
import com.now.nowbot.service.MessageServiceImpl.BPQueryService.Companion.isLessOrEqual
import com.now.nowbot.service.MessageServiceImpl.BPQueryService.Operator.*
import com.now.nowbot.service.MessageServiceImpl.ScorePRService.Companion.getScore4PanelE5
import com.now.nowbot.service.OsuApiService.OsuBeatmapApiService
import com.now.nowbot.service.OsuApiService.OsuScoreApiService
import com.now.nowbot.service.OsuApiService.OsuUserApiService
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.QQMsgUtil
import org.springframework.stereotype.Service
import java.util.regex.Pattern

@Service("BP_QUERY")
class BPQueryService(
    private var beatmapApiService: OsuBeatmapApiService,
    private var userApiService: OsuUserApiService,
    private var bindDao: BindDao,
    private var scoreApiService: OsuScoreApiService,
    private var imageService: ImageService,
) : MessageService<String> {

    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: MessageService.DataValue<String?>
    ): Boolean {
        val matcher = Instruction.BP_QUERY.matcher(messageText)
        return if (matcher.find()) {
            data.value = matcher.group("text")
            true
        } else {
            false
        }
    }


    override fun HandleMessage(event: MessageEvent, data: String) {
        val bindUser = bindDao.getUserFromQQ(event.sender.id)
        val bpList = scoreApiService.getBestPerformance(bindUser, bindUser.osuMode, 0, 100)
        val result = getBP(data, bpList)
        val user = userApiService.getPlayerInfo(bindUser)

        if (result.isEmpty()) {
            event.reply("没有找到符合条件的bp")
            return
        }

        val image = if (result.size == 1) {
            val score = result.first()
            val e5Param = getScore4PanelE5(user, score, beatmapApiService)
            imageService.getPanelE5(e5Param)
        } else {

            val indexMap = bpList.mapIndexed { i, s -> s.scoreID to i }.toMap()
            val ranks = result.map { indexMap[it.scoreID]!! }
            imageService.getPanelA4(user, result, ranks)
        }
        QQMsgUtil.sendImage(event, image)
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
        val filter: (Triple<Operator, String, Score>) -> Boolean,
        vararg val enabledOperator: Operator
    ) {
        Mapper("mapper", { (op, v, s) ->
            // 对字符串的 == / != 操作转为 包含 / 不包含
            if (op == EQ) {
                s.beatMapSet.creator.contains(v, true)
            } else {
                !s.beatMapSet.creator.contains(v, true)
            }
        }, EQ, NE),
        ScoreNumber("score", { (op, v, s) ->
            val score = try {
                v.toInt()
            } catch (e: Exception) {
                throw IllegalArgumentException("'score' invalid value '$v'")
            }
            when (op) {
                EQ -> s.score == score
                NE -> s.score != score
                GT -> s.score > score
                GE -> s.score >= score
                LT -> s.score < score
                LE -> s.score <= score
            }
        }, EQ, NE, GT, GE, LT, LE),
        Rank("rank", { (op, v, s) ->
            val socreRank = getRankNumber(s.rank)
            val rank = getRankNumber(v)
            when (op) {
                EQ -> socreRank == rank
                NE -> socreRank != rank
                GT -> socreRank > rank
                GE -> socreRank >= rank
                LT -> socreRank < rank
                LE -> socreRank <= rank
            }
        }, EQ, NE, GT, GE, LT, LE),
        Accuracy("acc", { (op, v, s) ->
            var acc = v.toDouble()
            if (acc > 1.0) acc /= 100
            when (op) {
                EQ -> s.accuracy.isEqual(acc)
                NE -> !s.accuracy.isEqual(acc)
                GT -> s.accuracy > acc
                GE -> s.accuracy.isGreaterOrEqual(acc)
                LT -> s.accuracy < acc
                LE -> s.accuracy.isLessOrEqual(acc)
            }
        }, EQ, NE, GT, GE, LT, LE),
        Bpm("bpm", { (op, v, s) ->
            val bpm = v.toFloat()
            when (op) {
                EQ -> s.beatMap.bpm.isEqual(bpm)
                NE -> !s.beatMap.bpm.isEqual(bpm)
                GT -> s.beatMap.bpm > bpm
                GE -> s.beatMap.bpm.isGreaterOrEqual(bpm)
                LT -> s.beatMap.bpm < bpm
                LE -> s.beatMap.bpm.isLessOrEqual(bpm)
            }
        }, EQ, NE, GT, GE, LT, LE),
        Combo("combo", { (op, v, s) ->
            val combo = v.toInt()
            when (op) {
                EQ -> s.maxCombo == combo
                NE -> s.maxCombo != combo
                GT -> s.maxCombo > combo
                GE -> s.maxCombo >= combo
                LT -> s.maxCombo < combo
                LE -> s.maxCombo <= combo
            }
        }, EQ, NE, GT, GE, LT, LE),
        Length("length", { (op, v, s) ->
            val length = v.toInt()
            when (op) {
                EQ -> s.beatMap.hitLength == length
                NE -> s.beatMap.hitLength != length
                GT -> s.beatMap.hitLength > length
                GE -> s.beatMap.hitLength >= length
                LT -> s.beatMap.hitLength < length
                LE -> s.beatMap.hitLength <= length
            }
        }, EQ, NE, GT, GE, LT, LE),
        Miss("miss", { (op, v, s) ->
            val misses = v.toInt()
            when (op) {
                EQ -> s.statistics.countMiss == misses
                NE -> s.statistics.countMiss != misses
                GT -> s.statistics.countMiss > misses
                GE -> s.statistics.countMiss >= misses
                LT -> s.statistics.countMiss < misses
                LE -> s.statistics.countMiss <= misses
            }
        }, EQ, NE, GT, GE, LT, LE),
        Mods("mod", { (op, v, s) ->
            // mod 处理是 = 为严格等于, > 为包含mod
            val mods = OsuMod.getModsValue(v)
            val scoreMods = OsuMod.getModsValue(s.mods.toTypedArray())
            when (op) {
                EQ -> mods == scoreMods
                GT -> mods and scoreMods == mods
                else -> throw IllegalArgumentException("'mod' invalid operator '${op.op}'")
            }
        }, EQ, GT)
        ;

        operator fun invoke(operator: Operator, value: String): (Score) -> Boolean {
            if (operator !in enabledOperator) throw IllegalArgumentException("'$key' invalid operator '${operator.op}'")
            return { score -> filter(Triple(operator, value, score)) }
        }
    }

    private fun getBP(text: String, scores: List<Score>): List<Score> {
        val conditions = getAllFilter(text)

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
        return scores.filter { s -> conditions.all { it(s) } }
    }

    companion object {
        val pattern = Pattern.compile("(\\w+)([><]=?|=|[!！]=)(\\w+)")
        val split = Pattern.compile("(\\s+)|[|,，]")

        private fun String.getOperator(): Triple<String, Operator, String> {
            val m = pattern.matcher(this)
            if (!m.find()) throw IllegalArgumentException("Invalid query")
            val operator = when (m.group(2)) {
                EQ.op -> EQ
                NE.op -> NE
                GT.op -> GT
                GE.op -> GE
                LT.op -> LT
                LE.op -> LE
                else -> throw IllegalArgumentException("Invalid operator '${m.group(2)}'")
            }
            return Triple(m.group(1), operator, m.group(3))
        }

        private fun getFilter(cmd: String): (Score) -> Boolean {
            val (key, operator, value) = cmd.getOperator()
            return when (key) {
                Param.Mapper.key -> Param.Mapper(operator, value)
                Param.ScoreNumber.key -> Param.ScoreNumber(operator, value)
                Param.Rank.key -> Param.Rank(operator, value)
                Param.Length.key -> Param.Length(operator, value)
                Param.Bpm.key -> Param.Bpm(operator, value)
                Param.Accuracy.key -> Param.Accuracy(operator, value)
                Param.Combo.key -> Param.Combo(operator, value)
                Param.Miss.key -> Param.Miss(operator, value)
                Param.Mods.key -> Param.Mods(operator, value)
                else -> throw IllegalArgumentException("Invalid key")
            }
        }

        private fun getAllFilter(text: String): List<(Score) -> Boolean> {
            val result = mutableListOf<(Score) -> Boolean>()
            text.split(split).filter { it.isNotBlank() }.forEach {
                val f = try {
                    getFilter(it.trim().lowercase())
                } catch (e: Exception) {
                    throw IllegalArgumentException("err: '$it' ${e.message}")
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
                else -> throw IllegalArgumentException("Invalid rank")
            }
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
    }
}