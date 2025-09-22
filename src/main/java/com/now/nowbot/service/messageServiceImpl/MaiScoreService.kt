package com.now.nowbot.service.messageServiceImpl

import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
import com.now.nowbot.model.enums.*
import com.now.nowbot.model.filter.MaiScoreFilter
import com.now.nowbot.model.maimai.MaiBestScore
import com.now.nowbot.model.maimai.MaiScore
import com.now.nowbot.model.maimai.MaiSong
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.divingFishApiService.MaimaiApiService
import com.now.nowbot.throwable.botRuntimeException.NoSuchElementException
import com.now.nowbot.util.DataUtil
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.command.*
import org.springframework.stereotype.Service
import java.util.regex.Matcher
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Service("MAI_SCORE") class MaiScoreService(
    private val maimaiApiService: MaimaiApiService,
    private val imageService: ImageService,
) : MessageService<MaiScoreService.MaiScoreParam> {

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
    data class MaiScoreStatistics(
        val count: Int,
        val totalRating: Int,
        val averageRating: Double,
        val averageAchievement: Double,
        val averageStar: Double,
    ) {
        companion object {
            fun getStatistics(scores: List<MaiScore>): MaiScoreStatistics {
                return MaiScoreStatistics(
                    count = scores.size,
                    totalRating = scores.sumOf { it.rating },
                    averageRating = scores.map { it.rating }.average(),
                    averageAchievement = scores.map { it.achievements }.average(),
                    averageStar = scores.map { it.star }.average()
                )
            }
        }
    }

    data class MaiScoreParam(
        val user: MaiBestScore.User?,
        val songs: List<MaiSong>,
        val scores: List<MaiScore>,
        val cabinet: MaiCabinet,
        val statistics: MaiScoreStatistics? = null,
        val page: Int = 1,
        val maxPage: Int = 1
    ) {
        fun toMap(): Map<String, Any?> {
            return if (songs.isEmpty()) {
                mapOf(
                    "user" to user,
                    "scores" to scores,
                    "page" to page,
                    "max_page" to maxPage,
                    "versions" to emptyList<String>(),
                    "statistics" to statistics,
                    "panel" to "MS",
                )
            } else {
                mapOf(
                    "user" to user,
                    "songs" to songs,
                    "scores" to scores,
                    "version" to cabinet.name,
                    "panel" to "MS",
                )
            }
        }
    }

    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: MessageService.DataValue<MaiScoreParam>
    ): Boolean {
        val matcher = Instruction.MAI_SCORE.matcher(messageText)

        if (!matcher.find()) {
            return false
        }

        data.value = getParam(event, matcher)

        return true
    }

    override fun HandleMessage(event: MessageEvent, param: MaiScoreParam) {
        val image: ByteArray = if (param.songs.isEmpty()) {
            imageService.getPanel(param.toMap(), "MA")
        } else {
            imageService.getPanel(param.toMap(), "MS")
        }

        event.reply(image)
    }

    private fun getParam(event: MessageEvent, matcher: Matcher): MaiScoreParam {
        val any: String? = matcher.group("name")

        val full: MaiBestScore

        val conditions = DataUtil.paramMatcher(any, MaiScoreFilter.entries.map { it.regex }, REG_MAI_RANGE.toRegex())

        val rangeInConditions = conditions.lastOrNull()?.firstOrNull()
        val hasCondition = conditions.sumOf { it.size } > 0

        val qqStr = (matcher.group(FLAG_QQ_ID) ?: "").trim()

        val qq = if (event.isAt) {
            event.target
        } else {
            qqStr.toLongOrNull() ?: event.sender.id
        }

        val cabinet = MaiCabinet.getCabinet(matcher.group(FLAG_VERSION))

        //输入的可能是外号或者歌曲编号
        if (!hasCondition && !any.isNullOrBlank()) {

            val id: Long?
            val title: String?
            val name: String?

            if (any.contains(Regex(REG_SPACE))) {
                val s = any.trim().split(Regex("\\s*"))

                if (s.size == 2) {
                    if (s.first().matches(REG_NUMBER_15.toRegex())) {
                        id = s.first().toLong()
                        title = null
                        name = s.last()
                    } else if (s.last().matches(REG_NUMBER_15.toRegex())) {
                        id = s.last().toLong()
                        title = null
                        name = s.first()
                    } else {
                        id = null
                        title = s.first()
                        name = s.last()
                    }
                } else {
                    id = null
                    title = any
                    name = null
                }
            } else if (any.matches(REG_NUMBER_15.toRegex())) {
                id = any.toLong()
                title = null
                name = null
            } else {
                id = null
                title = any
                name = null
            }

            full = if (name.isNullOrBlank()) {
                maimaiApiService.getMaimaiFullScores(qq)
            } else {
                maimaiApiService.getMaimaiFullScores(name)
            }

            val song: MaiSong

            if (id != null) {
                song = maimaiApiService.getMaimaiSong(id)
                    ?: maimaiApiService.getMaimaiAliasSong(id.toString()) // 有的歌曲外号叫 333
                    ?: throw NoSuchElementException.Song(id)
            } else {
                // 标题搜歌模式

                val possibles = maimaiApiService
                    .getMaimaiPossibleSongs(DataUtil.getStandardisedString(title))
                    ?.associateBy { it.title.getSimilarity(title) }
                    ?.filter { it.key > 0.4 }
                    ?.maxByOrNull { it.key }?.value

                if (possibles != null) {
                    song = possibles
                } else {
                    // 外号模式

                    val s = maimaiApiService.getMaimaiAliasSong(title ?: "")

                    // id 也可能是外号
                    val i = if (s != null) {
                        maimaiApiService.getMaimaiAlias(s.songID)?.alias
                    } else null

                    val sy = s?.title.getSimilarity(title) >= 0.4

                    val iy = (i?.maxOfOrNull { it.getSimilarity(title) } ?: 0.0) >= 0.4

                    if (s != null && (sy || iy)) {
                        song = s
                    } else {
                        throw NoSuchElementException.ResultNotAccurate()
                    }
                }
            }

            // 获取符合的成绩
            val scores: List<MaiScore> = full.records.filter {
                if (song.isDeluxe) {
                    return@filter it.songID == song.songID.toLong() || it.songID == (song.songID - 10000).toLong()
                } else {
                    return@filter it.songID == song.songID.toLong() || it.songID == (song.songID + 10000).toLong()
                }
            }

            maimaiApiService.insertSongData(scores)

            val anotherResult: MaiSong? = if (song.isDeluxe) {
                maimaiApiService.getMaimaiSong(song.songID - 10000L)
            } else {
                maimaiApiService.getMaimaiSong(song.songID + 10000L)
            }

            // 只有一种谱面
            if (anotherResult == null) {
                val version = if (song.isDeluxe) MaiCabinet.DX else MaiCabinet.SD

                return MaiScoreParam(
                    user = full.getUser(), songs = listOf(song), scores = scores, cabinet = version
                )
            } else if (scores.isNotEmpty() && cabinet == MaiCabinet.ANY) {
                // 有两种谱面，有成绩，没有规定难度。此时取玩家成绩最好的那个
                val isDX = scores.maxBy { it.rating }.isDeluxe

                val songs = listOf(song, anotherResult).filter { it.isDeluxe == isDX }

                return MaiScoreParam(user = full.getUser(), songs = songs, scores = scores.filter { it.isDeluxe == isDX }, cabinet = MaiCabinet.ANY
                )
            } else {
                // 有两种谱面，但是没有成绩
                val isDX = cabinet == MaiCabinet.DX || cabinet == MaiCabinet.ANY

                val songs = listOf(song, anotherResult).filter { it.isDeluxe == isDX }

                return MaiScoreParam(user = full.getUser(), songs = songs, scores = scores.filter { it.isDeluxe == isDX }, cabinet = MaiCabinet.ANY)
            }

        } else {
            // 条件筛选模式
            val page = matcher.group(FLAG_PAGE)?.toIntOrNull() ?: 1

            full = maimaiApiService.getMaimaiFullScores(qq)

            val scores = full.records
                .sortedByDescending { it.rating }
                .mapIndexed { i: Int, score: MaiScore ->
                    score.position = i + 1
                    score
                }

            maimaiApiService.insertSongData(scores)
            maimaiApiService.insertMaimaiAliasForScore(scores)

            val filteredScores = filterInRange(rangeInConditions, MaiScoreFilter.filterScores(scores, conditions))

            if (filteredScores.isEmpty()) {
                throw NoSuchElementException.BestScoreFiltered(full.getUser().name ?: qq.toString())
            }

            val split = DataUtil.splitPage(filteredScores, page, 50)

            return MaiScoreParam(
                user = full.getUser(),
                songs = listOf(),
                scores = split.first,
                cabinet = MaiCabinet.ANY,
                statistics = MaiScoreStatistics.getStatistics(filteredScores),
                page = split.second,
                maxPage = split.third
            )
        }
    }

    companion object {
        private fun filterInRange(range: String?, scores: List<MaiScore>): List<MaiScore> {
            if (range.isNullOrBlank()) return scores

            val intRange = if (range.contains(REG_HYPHEN.toRegex())) {
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

            return scores.filter {
                (it.star * 10).roundToInt() in intRange
            }
        }

        // 返回等级 x 10
        /**
         * @param isAccurate 如果为真，则 13 会匹配成 13.0。否则只会匹配成 13.0-13.5。
         */
        private fun parseLevel(level: String, isAccurate: Boolean = false): IntRange {
            if (level.contains(REG_PLUS.toRegex())) {
                val i10 = level.dropLastWhile { it == '?' || it == '？' }.dropLastWhile { it == '+' || it == '＋' }.toDouble() * 10.0

                return IntRange((floor(i10) + 6).roundToInt(), (floor(i10) + 9).roundToInt())
            } else if (level.contains('.') || isAccurate) {
                // 精确定级
                val i10 = level.dropLastWhile { it == '?' || it == '？' }.toDouble() * 10.0

                return (floor(i10)).roundToInt()..(floor(i10)).roundToInt()
            } else {
                // 模糊定级
                val i10 = level.dropLastWhile { it == '?' || it == '？' }.toDouble() * 10.0

                return (floor(i10)).roundToInt()..(floor(i10) + 5).roundToInt()
            }
        }


        // UUMS
        fun getSearchResult(text: String?, maimaiApiService: MaimaiApiService): MessageChain {
            val songs = maimaiApiService.getMaimaiSongLibrary()
            val result = mutableMapOf<Double, MaiSong>()

            for (s in songs.values) {
                val similarity = s.title.getSimilarity(text)

                if (similarity >= 0.4) {
                    maimaiApiService.insertMaimaiAlias(s)
                    result[similarity] = s
                }
            }

            if (result.isEmpty()) {
                throw NoSuchElementException.Result()
            }

            val sort = result.toSortedMap().reversed()

            val sb = StringBuilder("\n")

            var i = 1
            for (e in sort) {
                val code = MaiVersion.getCodeList(MaiVersion.getVersionList(e.value.info.version)).first()
                val category = MaiCategory.getCategory(e.value.info.genre).english

                sb.append("#${i}:").append(" ").append(String.format("%.0f", e.key * 100)).append("%").append(" ")
                    .append("[${e.value.songID}]").append(" ").append(e.value.title).append(" ").append("[${code}]")
                    .append(" / ").append("[${category}]").append("\n")

                i++

                if (i >= 6) break
            }

            val img = maimaiApiService.getMaimaiCover((sort[sort.firstKey()]?.songID ?: 0).toLong())

            sb.removeSuffix("\n")

            return MessageChain.MessageChainBuilder().addText("搜索结果：\n").addImage(img).addText(sb.toString())
                .build()
        }

        // uum
        fun getScoreMessage(score: MaiScore, image: ByteArray): MessageChain {
            val sb = MessageChain.MessageChainBuilder()

            sb.addImage(image)
            sb.addText("\n")
            sb.addText(
                "[${score.type}] ${score.title} [${score.difficulty} ${score.level}] (${score.star})\n"
            )
            sb.addText(
                "${String.format("%.4f", score.achievements)}% ${getRank(score.rank)} // ${score.rating} ra\n"
            )
            sb.addText("[${getCombo(score.combo)}] [${getSync(score.sync)}] // id ${score.songID}")

            return sb.build()
        }

        private fun getRank(rate: String?): String {
            return (rate ?: "?").uppercase().replace('P', '+')
        }

        private fun getCombo(combo: String?): String {
            return when (combo?.lowercase()) {
                "" -> "C"
                "fc" -> "FC"
                "fcp" -> "FC+"
                "ap" -> "AP"
                "app" -> "AP+"
                else -> "?"
            }
        }

        private fun getSync(sync: String?): String {
            return when (sync?.lowercase()) {
                "" -> "1P"
                "sync" -> "SY"
                "fs" -> "FS"
                "fsp" -> "FS+"
                "fsd" -> "FDX"
                "fsdp" -> "FDX+"
                else -> "?"
            }
        }

        private fun String?.getSimilarity(other: String?): Double {
            return DataUtil.getStringSimilarity(other, this)
        }
    }
}
