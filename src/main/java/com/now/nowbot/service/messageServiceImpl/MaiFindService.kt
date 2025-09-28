package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.model.enums.MaiDifficulty
import com.now.nowbot.model.filter.MaiSongFilter
import com.now.nowbot.model.maimai.MaiSong
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.divingFishApiService.MaimaiApiService
import com.now.nowbot.service.messageServiceImpl.MaiFindService.MaiFindParam
import com.now.nowbot.service.messageServiceImpl.MaiScoreService.Companion.parseLevel
import com.now.nowbot.throwable.botRuntimeException.NoSuchElementException
import com.now.nowbot.util.DataUtil
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.command.*
import org.springframework.stereotype.Service
import java.util.regex.Matcher
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Service("MAI_FIND") class MaiFindService(
    private val maimaiApiService: MaimaiApiService,
    private val imageService: ImageService
) : MessageService<MaiFindParam> {
    data class MaiFindParam(
        val songs: List<MaiSong>,
        val page: Int = 1,
        val maxPage: Int = 1,
        val count: Int = 0,
    ) {
        fun toMap(): Map<String, Any> {
            return mapOf(
                "songs" to songs,
                "count" to count,
                "page" to page,
                "max_page" to maxPage,
                // "user" to MaiBestScore.User("YumuBot", "", null, null, 0, 0, 0, null)
            )
        }
    }

    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: MessageService.DataValue<MaiFindParam>
    ): Boolean {
        val matcher = Instruction.MAI_FIND.matcher(messageText)

        if (!matcher.find()) {
            return false
        }

        data.value = getParam(matcher)
        return true
    }

    override fun HandleMessage(event: MessageEvent, param: MaiFindParam) {
        val image = imageService.getPanel(param.toMap(), "MF")

        event.reply(image)
    }

    private fun getParam(matcher: Matcher): MaiFindParam {
        val any: String? = matcher.group(FLAG_NAME)

        val conditions = DataUtil.paramMatcher(any, MaiSongFilter.entries.map { it.regex })

        val rangeInConditions = conditions.lastOrNull()?.firstOrNull()
        val hasRangeInConditions = (rangeInConditions.isNullOrEmpty().not())
        val hasCondition = conditions.dropLast(1).sumOf { it.size } > 0

        val songs: List<MaiSong>

        if (hasRangeInConditions.not() && hasCondition.not() && any.isNullOrBlank().not()) {
            val title = (any ?: "").trim()

            val id4Song = if (title.matches(REG_NUMBER_15.toRegex())) {
                maimaiApiService.getMaimaiSong(title.toLongOrNull() ?: -1L)
            } else null

            // 编号搜歌模式
            if (id4Song != null) {
                songs = listOf(id4Song)
            } else {
                // 标题搜歌模式
                val possibles = (
                        maimaiApiService.getMaimaiPossibleSongs(
                            DataUtil.getStandardisedString(title)
                        ) ?: listOf())
                    .associateBy { it.title.getSimilarity(title) }
                    .filter { it.key > 0.4 }
                    .map { it.value }

                songs = possibles.ifEmpty {
                    // 外号模式
                    maimaiApiService.getMaimaiAliasSongs(title) ?: throw NoSuchElementException.ResultNotAccurate()
                }
            }
        } else {
            // 常规模式
            val all = maimaiApiService.getMaimaiSongLibrary()
                .sortedByDescending { it.songID % 10000 }

            val difficulties = MaiDifficulty.getDifficulties(matcher.group(FLAG_DIFF))

            songs = if (hasCondition) {
                MaiSongFilter.filterSongs(all, conditions, difficulties)
            } else {
                filterInRange(rangeInConditions, all, difficulties)
            }

            if (songs.isEmpty()) {
                throw NoSuchElementException.ResultNotAccurate()
            }
        }

        val page = matcher.group(FLAG_PAGE)?.toIntOrNull() ?: 1
        val pages = DataUtil.splitPage(songs, page, maxPerPage = 48)

        return MaiFindParam(pages.first, pages.second, pages.third, songs.size)
    }

    companion object {

        private fun String?.getSimilarity(other: String?): Double {
            return DataUtil.getStringSimilarity(other, this)
        }

        private fun filterInRange(range: String?, songs: List<MaiSong>, difficulties: List<MaiDifficulty>): List<MaiSong> {

            val intRange = if (range.isNullOrBlank()) {
                10..150
            } else if (range.contains(REG_HYPHEN.toRegex())) {
                val s = range.split(REG_HYPHEN.toRegex()).map { it.trim() }

                if (s.size == 2) {
                    val f = parseLevel(s.first(), isAccurate = true)
                    val l = parseLevel(s.last(), isAccurate = true)

                    val min = min(min(f.first, f.last), min(l.first, l.last))
                    val max = max(max(f.first, f.last), max(l.first, l.last))

                    IntRange(min, max)
                } else {
                    parseLevel(s.first(), isAccurate = false)
                }
            } else {
                parseLevel(range)
            }

            val levels = difficulties.map { diff -> MaiDifficulty.getIndex(diff) }

            return songs.filter { song ->
                val lvs = mutableListOf<Int>()

                val result = song.star.mapIndexed { i, sr ->
                    val isLevel = levels.isEmpty() || levels.contains(i) || (song.isUtage && levels.contains(5))
                    val inRange = (sr * 10).roundToInt() in intRange

                    if (isLevel && inRange) {
                        lvs.add(i)

                        true
                    } else {
                        false
                    }
                }

                val boolean = result.contains(true)

                if (boolean) {
                    song.updateHighlight(lvs)
                }

                boolean
            }
        }
    }

    /*

    data class MaiFindParam(val ranges: List<Range>?, val difficulty: MaiDifficulty, val page: Int, val version: MaiVersion?, val dxScore: Int?)

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

        val page = if (version == MaiVersion.DEFAULT &&
            matcher.group("version")?.matches("\\s*$REG_NUMBER_1_100\\s*".toRegex()) == true) {
            matcher.group("version")?.toIntOrNull() ?: 1
        } else {
            1
        }

        val difficulty = MaiDifficulty.getDifficulty(matcher.group("diff"))

        val dxScore = matcher.group("score")?.toIntOrNull()

        data.value = MaiFindParam(range, difficulty, page, version, dxScore)
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

            val requirement: MutableSet<Int> = mutableSetOf()
            val rangeRequirement: MutableSet<Range> = mutableSetOf()

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
                            // meetCount++
                            requirement.add(i)
                            rangeRequirement.add(range)
                            continue@diff
                        }
                    }
                }
            }

            if (requirement.size > 0 && rangeRequirement.size > 0 && requirement.size == param.ranges?.size && rangeRequirement.size == param.ranges.size) {
                songs.add(s.value)
                continue@song
            }
        }

        if (songs.isEmpty()) throw NoSuchElementException.Result()
        if (songs.size > 200) throw IllegalArgumentException.ExceedException.FilteredScore(songs.size)

        val user = try {
            maimaiApiService.getMaimaiBest50(event.sender.id).getUser()
        } catch (e: Exception) {
            MaiBestScore.User("YumuBot", "", null, null, 0, 0, 0, null)
        }

        val pages = DataUtil.splitPage(songs.reversed(), param.page, 50)

        val image = imageService.getPanel(
            mapOf(
                "user" to user,
                "songs" to pages.first,
                )
            , "MF")
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

                if (diff.contains("+") || diff.contains("＋")) {
                    Range(levelBase + 0.6f, levelBase + 1f, includeFrom = true, includeTo = false)
                } else {
                    Range(levelBase, levelBase + 0.6f, includeFrom = true, includeTo = false)
                }
            }
        }
    }

     */
}
