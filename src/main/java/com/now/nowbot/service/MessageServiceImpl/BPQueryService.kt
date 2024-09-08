package com.now.nowbot.service.MessageServiceImpl

import com.now.nowbot.model.JsonData.MicroUser
import com.now.nowbot.model.JsonData.Score
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageServiceImpl.BPQueryService.Operator.*
import com.now.nowbot.service.OsuApiService.OsuBeatmapApiService
import com.now.nowbot.service.OsuApiService.OsuScoreApiService
import com.now.nowbot.service.OsuApiService.OsuUserApiService
import com.now.nowbot.util.Instruction
import org.springframework.stereotype.Service
import java.util.regex.Pattern

@Service("BP_QUERY")
class BPQueryService(
    private var beatmapApiService: OsuBeatmapApiService,
    private var userApiService: OsuUserApiService,
    private var scoreApiService: OsuScoreApiService,
    private var imageService: ImageService,
) : MessageService<String> {

    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: MessageService.DataValue<String?>
    ): Boolean {
        return if (Instruction.BP_QUERY.matcher(messageText).find()) {
            data.value = messageText
            true
        } else {
            false
        }
    }


    override fun HandleMessage(event: MessageEvent?, data: String?) {
        //todo: 没处理解析谁的bp
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
            val acc = v.toDouble()
            when (op) {
                EQ -> s.accuracy.isEqual(acc)
                NE -> !s.accuracy.isEqual(acc)
                GT -> s.accuracy > acc
                GE -> s.accuracy.isGreaterOrEqual(acc)
                LT -> s.accuracy < acc
                LE -> s.accuracy.isLessOrEqual(acc)
            }
        }, EQ, NE, GT, GE, LT, LE),
        Combo("combo", { false }),
        Miss("miss", { false }),
        Mode("mode", { false }),
        Mods("mod", { false }),
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
            mapperMap.forEach { index, mapperID ->
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
                Param.Accuracy.key -> Param.Accuracy(operator, value)
                Param.Combo.key -> Param.Combo(operator, value)
                Param.Miss.key -> Param.Miss(operator, value)
                Param.Mode.key -> Param.Mode(operator, value)
                Param.Mods.key -> Param.Mods(operator, value)
                else -> throw IllegalArgumentException("Invalid key")
            }
        }

        private fun getAllFilter(text: String): List<(Score) -> Boolean> {
            val result = mutableListOf<(Score) -> Boolean>()
            text.split(" ").filter { it.isBlank() }.forEach {
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
    }
}