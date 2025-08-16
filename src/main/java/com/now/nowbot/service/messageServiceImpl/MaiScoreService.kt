package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.model.enums.*
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

    /*

    data class MaiScoreParam(
        val id: Int?,
        val title: String?,
        val name: String?,
        val qq: Long?,
        val version: MaiCabinet,
        val difficulty: MaiDifficulty,
        val range: MaiFindService.Range,
    )

    data class MSPanelParam(val user: MaiBestScore.User?, val songs: List<MaiSong>, val scores: List<MaiScore>, val version: MaiCabinet) {
        fun toMap(): Map<String, Any?> {
            return mapOf(
                "user" to user,
                "songs" to songs,
                "scores" to scores,
                "version" to version.name,
                "panel" to "MS"
            )
        }
    }

    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: MessageService.DataValue<MaiScoreParam>,
    ): Boolean {
        val matcher = Instruction.MAI_SCORE.matcher(messageText)

        if (!matcher.find()) {
            return false
        }

        val difficulty = if (matcher.group(FLAG_DIFF).isNullOrBlank().not()) {
            MaiDifficulty.getDifficulty(matcher.group(FLAG_DIFF))
        } else {
            MaiDifficulty.DEFAULT
        }

        val version = MaiCabinet.getCabinet(matcher.group(FLAG_VERSION))

        val nameOrTitleStr = (matcher.group(FLAG_NAME) ?: "").trim()

        val qqStr = (matcher.group(FLAG_QQ_ID) ?: "").trim()

        val qq = if (event.isAt) {
            event.target
        } else {
            qqStr.toLongOrNull() ?: event.sender.id
        }

        if (nameOrTitleStr.isNotBlank()) {
            if (nameOrTitleStr.contains(Regex(REG_SPACE))) {
                val s = nameOrTitleStr.split(Regex(REG_SPACE))

                if (s.size >= 2) {
                    if (s.first().matches(Regex(REG_NUMBER_15))) {
                        data.value = MaiScoreParam(
                            s.first().toInt(),
                            null,
                            s.last().replace(Regex(REG_QUOTATION), ""),
                            null,
                            version,
                            difficulty,
                            null,
                        )
                    } else {
                        data.value = MaiScoreParam(
                            null,
                            nameOrTitleStr.replace(Regex(REG_QUOTATION), ""),
                            null,
                            qq,
                            version,
                            difficulty,
                            null,
                        )
                    }
                } else if (s.size == 1) {
                    if (s.first().matches(Regex(REG_NUMBER_15))) {
                        data.value = MaiScoreParam(s.first().toInt(), null, null, qq, version, difficulty, null)
                    } else if (s.first().contains(Regex(REG_QUOTATION))) {
                        throw IllegalArgumentException.WrongException.Quotation()
                    } else {
                        data.value = MaiScoreParam(null, nameOrTitleStr, null, qq, version, difficulty, null)
                    }
                } else {
                    throw IllegalArgumentException.WrongException.BeatmapID()
                }
            } else {
                if (nameOrTitleStr.matches(Regex(REG_NUMBER_15))) {
                    data.value = MaiScoreParam(nameOrTitleStr.toInt(), null, null, qq, version, difficulty, null)
                } else if (nameOrTitleStr.contains(Regex(REG_QUOTATION))) {
                    throw IllegalArgumentException.WrongException.Quotation()
                } else {
                    data.value = MaiScoreParam(null, nameOrTitleStr, null, qq, version, difficulty, null)
                }
            }
        } else {
            throw IllegalArgumentException.WrongException.BeatmapID()
        }

        return true
    }

    override fun HandleMessage(event: MessageEvent, param: MaiScoreParam) {
        val full = getFullScoreOrNull(param.qq, param.name, maimaiApiService)

        val result: MaiSong = if (param.title != null) {
            val title = DataUtil.getStandardisedString(param.title)

            // 标题搜歌模式
            val possibles = maimaiApiService.getMaimaiPossibleSongs(title)

            val r = if (!possibles.isNullOrEmpty()) {
                possibles.associateBy { it.title.getSimilarity(title) }.filter { it.key > 0.4 }.maxByOrNull { it.key }?.value
            } else {
                null
            }

            if (r != null) {
                r
            } else {
                // 外号模式
                val s = maimaiApiService.getMaimaiAliasSong(title)

                // id 也可能是外号
                val i = if (s != null) {
                    maimaiApiService.getMaimaiAlias(s.songID)?.alias
                } else null

                val sy = s?.title.getSimilarity(title) >= 0.4

                val iy = (i?.maxOfOrNull { it.getSimilarity(title) } ?: 0.0) >= 0.4

                if (s != null && (sy || iy)) {
                    s
                } else {
                    throw NoSuchElementException.ResultNotAccurate()
                }
            }
        } else if (param.id != null) { // ID 搜歌模式
            maimaiApiService.getMaimaiSong(param.id.toLong())
                ?: maimaiApiService.getMaimaiAliasSong(param.id.toString()) // 有的歌曲外号叫 3333
                ?: throw NoSuchElementException.Song(param.id)
        } else {
            throw IllegalStateException.ClassCast("舞萌成绩")
        }


        val image: ByteArray = run {

            // 获取符合的成绩
            val scores: List<MaiScore> = full?.records?.filter {
                if (result.isDeluxe) {
                    return@filter it.songID == result.songID.toLong() || it.songID == (result.songID - 10000).toLong()
                } else {
                    return@filter it.songID == result.songID.toLong() || it.songID == (result.songID + 10000).toLong()
                }
            } ?: listOf()

            maimaiApiService.insertSongData(scores)

            val anotherResult: MaiSong? = if (result.isDeluxe) {
                maimaiApiService.getMaimaiSong(result.songID - 10000L)
            } else {
                maimaiApiService.getMaimaiSong(result.songID + 10000L)
            }

            // 只有一种谱面
            if (anotherResult == null) {
                val version = if (result.isDeluxe) MaiCabinet.DX else MaiCabinet.SD

                return@run imageService.getPanel(
                    MSPanelParam(user = full?.getUser(), songs = listOf(result), scores = scores, version = version).toMap(),
                    "MS")
            } else if (scores.isNotEmpty() && param.version == MaiCabinet.ANY) {
                // 有两种谱面，有成绩，没有规定难度。此时取玩家成绩最好的那个
                val isDX = scores.maxBy { it.rating }.isDeluxe

                val songs = listOf(listOf(result, anotherResult).first { it.isDeluxe == isDX })

                return@run imageService.getPanel(
                    MSPanelParam(user = full?.getUser(), songs = songs,
                        scores = scores.filter { it.isDeluxe == isDX }, version = MaiCabinet.ANY).toMap(),
                    "MS")
            } else {
                // 有两种谱面，但是没有成绩
                val isDX = param.version == MaiCabinet.DX || param.version == MaiCabinet.ANY

                val songs = listOf(listOf(result, anotherResult).first { it.isDeluxe == isDX })

                return@run imageService.getPanel(
                    MSPanelParam(user = full?.getUser(), songs = songs, scores = scores.filter { it.isDeluxe == isDX }, version = MaiCabinet.ANY).toMap(),
                    "MS")
            }
        }

        event.reply(image)
    }

     */

    data class MaiScoreParam(val user: MaiBestScore.User?, val songs: List<MaiSong>, val scores: List<MaiScore>, val cabinet: MaiCabinet, val page: Int = 1, val maxPage: Int = 1) {
        fun toMap(): Map<String, Any?> {
            return if (songs.isEmpty()) {
                mapOf(
                    "user" to user,
                    "scores" to scores,
                    "page" to page,
                    "max_page" to maxPage,
                    "versions" to emptyList<String>(),
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

        val conditions = DataUtil.paramMatcher(any, MaiScoreFilter.entries.map { it.regex }, REG_EQUAL.toRegex())

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

            return MaiScoreParam(user = full.getUser(), songs = listOf(), scores = split.first, cabinet = MaiCabinet.ANY, page = split.second, maxPage = split.third)
        }
    }

    companion object {
        private fun filterInRange(range: String?, scores: List<MaiScore>): List<MaiScore> {
            if (range.isNullOrBlank()) return scores

            val intRange = if (range.contains(REG_HYPHEN.toRegex())) {
                val s = range.split(REG_HYPHEN.toRegex()).map { it.trim() }

                if (s.size == 2) {
                    val f = parseLevel(s.first())
                    val l = parseLevel(s.last())

                    val min = min(min(f.first, f.last), min(l.first, l.last))
                    val max = max(max(f.first, f.last), max(l.first, l.last))

                    IntRange(min, max)
                } else {
                    parseLevel(s.first())
                }
            } else {
                parseLevel(range)
            }

            return scores.filter {
                (it.star * 10).roundToInt() in intRange
            }
        }

        // 返回等级 x 10
        private fun parseLevel(level: String): IntRange {
            if (level.contains(REG_PLUS.toRegex())) {
                val i = level.dropLastWhile { it == '?' || it == '？' }.dropLastWhile { it == '+' || it == '＋' }

                return IntRange(((floor(i.toDouble()) + 0.6) * 10).roundToInt(), ((floor(i.toDouble()) + 0.9) * 10).roundToInt())
            } else if (level.contains('.')) {
                // 精确定级
                val i = level.dropLastWhile { it == '?' || it == '？' }

                return (floor(i.toDouble()) * 10).roundToInt()..(floor(i.toDouble()) * 10).roundToInt()
            } else {
                // 模糊定级
                val i = level.dropLastWhile { it == '?' || it == '？' }

                return (floor(i.toDouble()) * 10).roundToInt()..((floor(i.toDouble()) + 0.5) * 10).roundToInt()
            }
        }




        private fun getFullScoreOrNull(
            qq: Long?,
            name: String?,
            maimaiApiService: MaimaiApiService,
        ): MaiBestScore? {
            return if (qq != null) {
                try {
                    maimaiApiService.getMaimaiFullScores(qq)
                } catch (e: Exception) {
                    return null
                }
            } else if (name != null) {
                try {
                    maimaiApiService.getMaimaiFullScores(name)
                } catch (e: Exception) {
                    return null
                }
            } else {
                throw NoSuchElementException.Player()
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
                val category = MaiVersion.getCategoryAbbreviation(e.value.info.genre)

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
