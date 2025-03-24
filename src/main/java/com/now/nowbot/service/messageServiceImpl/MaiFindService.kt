package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.model.enums.MaiDifficulty
import com.now.nowbot.model.enums.MaiVersion
import com.now.nowbot.model.json.MaiBestScore
import com.now.nowbot.model.json.MaiSong
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.divingFishApiService.MaimaiApiService
import com.now.nowbot.service.messageServiceImpl.MaiFindService.MaiFindParam
import com.now.nowbot.throwable.GeneralTipsException
import com.now.nowbot.util.DataUtil
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.command.*
import org.intellij.lang.annotations.Language
import org.springframework.stereotype.Service
import kotlin.math.abs
import kotlin.math.floor

@Service("MAI_FIND") class MaiFindService(
    private val maimaiApiService: MaimaiApiService,
    private val imageService: ImageService
) : MessageService<MaiFindParam> {

    data class MaiFindParam(val ranges: List<Range>?, val difficulty: MaiDifficulty, val version: MaiVersion?, val dxScore: Int?)

    // 默认包含开头，包含结尾
    data class Range(val from: Float, val to: Float, val includeFrom: Boolean = true, val includeTo: Boolean = true)

    enum class Operator(@Language("RegExp") val regex: Regex) {
        // 这么写真的是好事？
        RANGE("(?<from>$REG_NUMBER_DECIMAL$REG_PLUS?)$REG_HYPHEN(?<to>$REG_NUMBER_DECIMAL$REG_PLUS?)".toRegex()),
        GREATER_OR_EQUAL("($REG_GREATER$REG_EQUAL$REG_NUMBER_DECIMAL$REG_PLUS?|$REG_NUMBER_DECIMAL$REG_PLUS?$REG_LESS$REG_EQUAL)".toRegex()),
        GREATER("($REG_GREATER$REG_NUMBER_DECIMAL$REG_PLUS?|$REG_NUMBER_DECIMAL$REG_PLUS?$REG_LESS)".toRegex()),
        SMALLER_OR_EQUAL("($REG_LESS$REG_EQUAL$REG_NUMBER_DECIMAL$REG_PLUS?|$REG_NUMBER_DECIMAL$REG_PLUS?$REG_GREATER$REG_EQUAL)".toRegex()),
        SMALLER("($REG_LESS$REG_NUMBER_DECIMAL$REG_PLUS?|$REG_NUMBER_DECIMAL$REG_PLUS?$REG_GREATER)".toRegex()),
        SINGLE("$REG_NUMBER_DECIMAL$REG_PLUS?".toRegex()),
    }

    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: MessageService.DataValue<MaiFindParam>,
    ): Boolean {
        val matcher = Instruction.MAI_FIND.matcher(messageText)

        if (!matcher.find()) {
            return false
        }

        val result = DataUtil.paramMatcher(matcher.group("any"), Operator.entries.map { it.regex })
        val range = getRange(result)

        /*
        val type = when (matcher.group("type")?.lowercase()) {
            "dx", "deluxe", "d", "x" -> "dx"
            "sd", "standard", "s", "std" -> "sd"

            null -> null
            else -> null
        }

         */

        val version = MaiVersion.getVersion(matcher.group("version"))

        val difficulty = MaiDifficulty.getDifficulty(matcher.group("diff"))

        val dxScore = matcher.group("score")?.toIntOrNull()

        data.value = MaiFindParam(range, difficulty, version, dxScore)
        return true
    }

    override fun HandleMessage(event: MessageEvent, param: MaiFindParam) {
        val library = maimaiApiService.getMaimaiSongLibrary()
        val songs = mutableListOf<MaiSong>()

        song@
        for (s in library.entries) {
            if (param.version != MaiVersion.DEFAULT && param.version != MaiVersion.getVersion(s.value.info.version)) {
                continue@song
            }

            var meetCount = 0

            diff@
            for (i in s.value.star.indices) {
                if (param.dxScore != null) {
                    val dxScore = s.value.charts[i].dxScore
                    if (dxScore >= param.dxScore + 100 || dxScore < param.dxScore) {
                        continue@diff
                    }
                }

                if (param.difficulty != MaiDifficulty.DEFAULT) {
                    val diff = if (s.value.level[i].contains('?')) {
                        MaiDifficulty.UTAGE
                    } else {
                        MaiDifficulty.getIndex(i)
                    }

                    if (param.difficulty != diff) {
                        continue@diff
                    }
                }

                val sr = s.value.star[i]

                if (param.ranges.isNullOrEmpty()) {
                    songs.add(s.value)
                    continue@song
                } else {
                    for (range in param.ranges) {
                        if (isInRange(sr, range)) {
                            meetCount++
                            continue@diff
                        }
                    }
                }
            }

            if (meetCount > 0 && meetCount == param.ranges?.size) {
                songs.add(s.value)
                continue@song
            }
        }

        if (songs.isEmpty()) throw GeneralTipsException(GeneralTipsException.Type.G_Null_Result)
        if (songs.size > 200) throw GeneralTipsException(GeneralTipsException.Type.G_Exceed_Score_Count, songs.size)

        val user = try {
            maimaiApiService.getMaimaiBest50(event.sender.id).getUser()
        } catch (e: Exception) {
            MaiBestScore.User("YumuBot", "", null, null, 0, 0, 0, null)
        }

        val image = imageService.getPanel(mapOf("user" to user, "songs" to songs), "MF")
        event.reply(image)
    }

    companion object {
        @JvmStatic fun isInRange(number: Double, range: Range): Boolean {
            return if (abs(range.from - range.to) < 1e-4) {
                abs(number - range.to) < 1e-4
            } else if (range.includeFrom) {
                if (range.includeTo) {
                    range.from - 1e-4 < number && number < range.to + 1e-4
                } else {
                    range.from - 1e-4 < number && number < range.to - 1e-4
                }
            } else {
                if (range.includeTo) {
                    range.from + 1e-4 < number && number < range.to + 1e-4
                } else {
                    range.from + 1e-4 < number && number < range.to - 1e-4
                }
            }
        }


        @JvmStatic fun getRange(ranges: List<List<String>>): List<Range> {
            if (ranges.isEmpty()) return emptyList()

            val result = mutableListOf<Range>()

            if (ranges.first().isNotEmpty()) {
                ranges.first().forEach {
                    val m = Operator.RANGE.regex.toPattern().matcher(it)

                    if (m.find()) {
                        var from = m.group("from").toFloat()
                        var to = m.group("to").toFloat()

                        if (from > to) to = from.also { from = to }

                        result.add(Range(from, to))
                    }
                }
            }

            if (ranges[1].isNotEmpty()) {
                ranges[1].forEach {
                    val r = parseDifficulty(it)

                    result.add(Range(r.from, 15f))
                }
            }

            if (ranges[2].isNotEmpty()) {
                ranges[2].forEach {
                    val r = parseDifficulty(it)

                    result.add(Range(r.from, 15f, includeFrom = false, includeTo = true))
                }
            }

            if (ranges[3].isNotEmpty()) {
                ranges[3].forEach {
                    val r = parseDifficulty(it)

                    result.add(Range(0f, r.to))
                }
            }

            if (ranges[4].isNotEmpty()) {
                ranges[4].forEach {
                    val r = parseDifficulty(it)

                    result.add(Range(0f, r.to, includeFrom = true, includeTo = false))
                }
            }

            if (ranges[5].isNotEmpty()) {
                ranges[5].forEach {
                    result.add(parseDifficulty(it))
                }
            }

            return result
        }

        @JvmStatic fun parseDifficulty(diff: String?): Range {
            if (diff == null) return Range(0f, 15f)

            val level = diff.removeSuffix("+").removeSuffix("＋").trim().toFloat()

            return if (diff.contains(".")) {
                Range(level, level)
            } else {
                val levelBase = floor(level)

                // TODO PRiSM 更新后，会变成 .6
                if (diff.contains("+") || diff.contains("＋")) {
                    Range(levelBase + 0.7f, levelBase + 1f, includeFrom = true, includeTo = false)
                } else {
                    Range(levelBase, levelBase + 0.7f, includeFrom = true, includeTo = false)
                }
            }
        }
    }
}
