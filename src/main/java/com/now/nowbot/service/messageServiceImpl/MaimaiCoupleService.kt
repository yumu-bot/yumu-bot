package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.model.enums.MaiDifficulty
import com.now.nowbot.model.enums.MaiVersion
import com.now.nowbot.model.json.MaiBestScore
import com.now.nowbot.model.json.MaiSong
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.divingFishApiService.MaimaiApiService
import com.now.nowbot.service.messageServiceImpl.MaimaiCoupleService.MaiCoupleParam
import com.now.nowbot.throwable.GeneralTipsException
import com.now.nowbot.util.DataUtil
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.command.*
import org.intellij.lang.annotations.Language
import org.springframework.stereotype.Service

@Service("MAIMAI_COUPLE") class MaimaiCoupleService(
    private val maimaiApiService: MaimaiApiService,
    private val imageService: ImageService
) : MessageService<MaiCoupleParam> {

    data class MaiCoupleParam(val ranges: List<Range>?, val difficulty: MaiDifficulty, val version: MaiVersion?, val dxScore: Int?)

    // 默认包含开头，不包含结尾
    data class Range(val from: Float, val to: Float, val includeFrom: Boolean = true, val includeTo: Boolean = false)

    enum class Type(@Language("RegExp") val regex: Regex) {
        // 这么写真的是好事？
        RANGE("(?<from>$REG_NUMBER_DECIMAL$REG_PLUS?)$REG_HYPHEN(?<to>$REG_NUMBER_DECIMAL$REG_PLUS?)".toRegex()),
        GREATER_OR_EQUAL("($REG_GREATER$REG_EQUAL$REG_NUMBER_DECIMAL$REG_PLUS?|$REG_NUMBER_DECIMAL$REG_PLUS?$REG_SMALLER$REG_EQUAL)".toRegex()),
        GREATER("($REG_GREATER$REG_NUMBER_DECIMAL$REG_PLUS?|$REG_NUMBER_DECIMAL$REG_PLUS?$REG_SMALLER)".toRegex()),
        SMALLER_OR_EQUAL("($REG_SMALLER$REG_EQUAL$REG_NUMBER_DECIMAL$REG_PLUS?|$REG_NUMBER_DECIMAL$REG_PLUS?$REG_GREATER$REG_EQUAL)".toRegex()),
        SMALLER("($REG_SMALLER$REG_NUMBER_DECIMAL$REG_PLUS?|$REG_NUMBER_DECIMAL$REG_PLUS?$REG_GREATER)".toRegex()),
        SINGLE("$REG_NUMBER_DECIMAL$REG_PLUS?".toRegex()),
    }

    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: MessageService.DataValue<MaiCoupleParam>,
    ): Boolean {
        val matcher = Instruction.MAI_COUPLE.matcher(messageText)

        if (!matcher.find()) {
            return false
        }

        val result = DataUtil.paramMatcher(matcher.group("any"), Type.entries.map { it.regex })
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

        data.value = MaiCoupleParam(range, difficulty, version, dxScore)
        return true
    }

    override fun HandleMessage(event: MessageEvent, param: MaiCoupleParam) {
        val library = maimaiApiService.getMaimaiSongLibrary()
        val songs = mutableListOf<MaiSong>()

        song@ for (s in library.entries) {
            if (param.version != null && param.version == MaiVersion.getVersion(s.value.info.version)) continue@song

            var meetCount = 0

            diff@ for (i in s.value.star.indices) {
                if (param.dxScore != null) {
                    val dxScore = s.value.charts[i].dxScore
                    if (dxScore < param.dxScore || dxScore > param.dxScore + 1000) continue
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

                if (param.ranges == null) {
                    songs.add(s.value)
                    continue@song
                }

                val sr = s.value.star[i]

                for (range in param.ranges) {
                    if (inRange(sr, range)) {
                        meetCount++
                        continue@diff
                    }
                }
            }

            if (param.ranges != null && meetCount == param.ranges.size) {
                songs.add(s.value)
                continue@song
            }
        }

        if (songs.isEmpty()) throw GeneralTipsException(GeneralTipsException.Type.G_Empty_Score)
        if (songs.size > 200) throw GeneralTipsException(GeneralTipsException.Type.G_Exceed_Score)

        val user = try {
            maimaiApiService.getMaimaiBest50(event.sender.id).getUser()
        } catch (e: Exception) {
            MaiBestScore.User("YumuBot", "", null, null, 0, 0, 0, null)
        }

        val image = imageService.getPanel(mapOf("user" to user, "songs" to songs), "MC")
        event.reply(image)
    }

    companion object {
        @JvmStatic fun inRange(number: Double, range: Range): Boolean {
            return if (range.includeFrom) {
                if (range.includeTo) {
                    range.from <= number && number <= range.to
                } else {
                    range.from <= number && number < range.to
                }
            } else {
                if (range.includeTo) {
                    range.from < number && number <= range.to
                } else {
                    range.from < number && number < range.to
                }
            }
        }


        @JvmStatic fun getRange(ranges: List<String?>): List<Range> {
            if (ranges.isEmpty()) return emptyList()

            val result = mutableListOf<Range>()

            if (ranges.first() != null) {
                val m = Type.RANGE.regex.toPattern().matcher(ranges.first()!!)

                if (m.find()) {
                    var from = m.group("from").toFloat()
                    var to = m.group("to").toFloat()

                    if (from > to) to = from.also { from = to }

                    result.add(Range(from, to, includeFrom = true, includeTo = true))
                }
            }

            if (ranges[1] != null) {
                val r = parseDifficulty(ranges[1]!!)

                result.add(Range(r.from, 15f, includeFrom = true, includeTo = true))
            }

            if (ranges[2] != null) {
                val r = parseDifficulty(ranges[2]!!)

                result.add(Range(r.from, 15f, includeFrom = false, includeTo = true))
            }

            if (ranges[3] != null) {
                val r = parseDifficulty(ranges[3]!!)

                result.add(Range(0f, r.to, includeFrom = true, includeTo = true))

            }

            if (ranges[4] != null) {
                val r = parseDifficulty(ranges[4]!!)

                result.add(Range(0f, r.to, includeFrom = true, includeTo = false))

            }

            if (ranges[5] != null) {
                result.add(parseDifficulty(ranges[5]!!))
            }

            return result
        }

        @JvmStatic fun parseDifficulty(diff: String?): Range {
            if (diff == null) return Range(0f, 15f, includeTo = true, includeFrom = true)

            val level = diff.removeSuffix("+").removeSuffix("＋").trim().toFloat()

            // TODO PRiSM 更新后，会变成 .6
            if (diff.contains("+") || diff.contains("＋")) {
                return Range(level + 0.7f, level + 1f)
            }

            return Range(level, level + 0.7f)
        }
    }
}
