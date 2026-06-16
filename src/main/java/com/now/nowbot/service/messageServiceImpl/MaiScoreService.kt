package com.now.nowbot.service.messageServiceImpl

import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
import com.now.nowbot.dao.MaiDao
import com.now.nowbot.entity.ServiceCallStatistic
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
import com.now.nowbot.service.lxnsApiService.LxMaiApiService
import com.now.nowbot.throwable.TipsException
import com.now.nowbot.throwable.botRuntimeException.NoSuchElementException
import com.now.nowbot.util.ASyncMessageUtil
import com.now.nowbot.util.DataUtil
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.command.*
import org.springframework.stereotype.Service
import java.util.regex.Matcher

@Service("MAI_SCORE") class MaiScoreService(
    private val maimaiApiService: MaimaiApiService,
    private val lxMaiApiService: LxMaiApiService,
    private val imageService: ImageService,
    private val maiDao: MaiDao
) : MessageService<MaiScoreService.MaiScoreParam> {

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
    data class MaiScoreStatistics(
        val count: Int,
        val pageRating: Int,
        val totalRating: Int,
        val averageRating: Double,
        val averageAchievement: Double,
        val averageStar: Double,
    ) {
        companion object {
            fun getStatistics(scores: List<MaiScore>, thisPageScores: List<MaiScore> = scores): MaiScoreStatistics {
                return MaiScoreStatistics(
                    count = scores.size,
                    pageRating = thisPageScores.sumOf { it.rating },
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

    override fun handleMessage(event: MessageEvent, param: MaiScoreParam): ServiceCallStatistic? {
        val image: ByteArray = if (param.songs.isEmpty()) {
            imageService.getPanel(param.toMap(), "MA")
        } else {
            imageService.getPanel(param.toMap(), "MS")
        }

        event.replyAsync(image)

        return ServiceCallStatistic.building(event) {
            val mais = if (param.songs.isNotEmpty()) {
                param.songs.map { it.songID }.distinct()
            } else {
                param.scores.map { it.songID }.distinct()
            }

            setParam(mapOf(
                "mais" to mais
            ))
        }
    }

    private fun getParam(event: MessageEvent, matcher: Matcher): MaiScoreParam {
        val titleStr: String = (matcher.group(FLAG_NAME) ?: "").trim()
        val name: String? = matcher.group(FLAG_DATA)?.trim()

        val difficulties: String? = matcher.group(FLAG_DIFF)

        val full: MaiBestScore

        val conditions = DataUtil.getConditions(titleStr, MaiScoreFilter.entries.map { it.regex },
            endPattern = MaiScoreFilter.RANGE.regex.pattern)

        val rangeInConditions = conditions.lastOrNull().orEmpty()
        val hasCondition = conditions.dropLast(1).any { it.isNotEmpty() }

        val qqStr = (matcher.group(FLAG_QQ_ID) ?: "").trim()

        val qq = if (event.hasAt()) {
            event.target
        } else {
            qqStr.toLongOrNull() ?: event.sender.contactID
        }

        var cabinet = MaiCabinet.getCabinet(matcher.group(FLAG_VERSION))

        val isRange = titleStr.matches(REG_MAI_RANGE.toRegex())

        // 输入的可能是外号或者歌曲编号
        if (!hasCondition && titleStr.isNotEmpty() && !isRange) {
            val id: Long?
            val title: String?

            if (titleStr.matches(REG_NUMBER_15.toRegex())) {
                id = titleStr.toLong()
                title = null
            } else {
                id = null
                title = titleStr
            }

            full = if (name.isNullOrBlank()) {
                maimaiApiService.getMaimaiFullScores(qq)
            } else {
                maimaiApiService.getMaimaiFullScores(name)
            }

            val song: MaiSong

            if (id != null) {
                song = lxMaiApiService.getMaiSong(id.toInt())
                    ?: maimaiApiService.getMaimaiAliasSong(id.toString()) // 有的歌曲外号叫 333
                    ?: throw NoSuchElementException.Song(id)
            } else {
                val possibles = maimaiApiService
                    .getMaimaiPossibleSongs(DataUtil.getStandardisedString(title))
                    .associateBy { it.title.getSimilarity(title) }

                val selected = selectSong(event, cabinet, possibles)

                song = if (selected != null) {
                    cabinet = MaiCabinet.getCabinet(selected)
                    selected
                } else {
                    // 外号模式

                    val possibles2 = maimaiApiService.getMaimaiAliasSongs((title ?: ""))

                    if (cabinet == MaiCabinet.UTAGE) {
                        val possibles3 = possibles2.flatMap { pos -> maimaiApiService.getUtage(pos.songID) }

                        val selected3 = selectSong(event, cabinet, possibles3)
                        cabinet = MaiCabinet.getCabinet(selected3)

                        selected3 ?: throw NoSuchElementException.ResultNotAccurate()
                    } else {

                        val selected2 = selectSong(event, cabinet, possibles2)
                        cabinet = MaiCabinet.getCabinet(selected2)

                        selected2 ?: throw NoSuchElementException.ResultNotAccurate()
                    }
                }
            }

            // 获取符合的成绩
            val scores: List<MaiScore> = full.records.filter {
                if (song.isUtage) {
                    return@filter it.songID == song.songID.toLong()
                } else if (song.isDeluxe) {
                    return@filter it.songID == song.songID.toLong() || it.songID == (song.songID - 10000).toLong()
                } else {
                    return@filter it.songID == song.songID.toLong() || it.songID == (song.songID + 10000).toLong()
                }
            }

            maimaiApiService.insertSongData(scores)

            val anotherResult: MaiSong? = if (song.isUtage) {
                null
            } else if (song.isDeluxe) {
                lxMaiApiService.getMaiSong(song.songID - 10000)
            } else {
                lxMaiApiService.getMaiSong(song.songID + 10000)
            }

            // 只有一种谱面
            if (anotherResult == null) {
                return MaiScoreParam(
                    user = full.getUser(maiDao), songs = listOf(song), scores = scores, cabinet = cabinet
                )
            } else if (scores.isNotEmpty() && cabinet == MaiCabinet.ANY) {
                // 有两种谱面，有成绩，没有规定难度。此时取玩家成绩最好的那个
                val isDX = scores.maxBy { it.rating }.isDeluxe

                val songs = listOf(song, anotherResult).filter { it.isDeluxe == isDX }

                return MaiScoreParam(user = full.getUser(maiDao), songs = songs, scores = scores.filter { it.isDeluxe == isDX }, cabinet = cabinet
                )
            } else {
                // 有两种谱面，但是没有成绩
                val isDX = cabinet == MaiCabinet.DX || cabinet == MaiCabinet.ANY

                val songs = listOf(song, anotherResult).filter { it.isDeluxe == isDX }

                return MaiScoreParam(user = full.getUser(maiDao), songs = songs, scores = scores.filter { it.isDeluxe == isDX }, cabinet = MaiCabinet.ANY)
            }

        } else {
            // 条件筛选模式
            val page = matcher.group(FLAG_PAGE)?.toIntOrNull() ?: 1

            full = maimaiApiService.getMaimaiFullScores(qq)

            val sorted = full.records
                .sortedByDescending { it.rating }

            val scores = if (conditions.size >= 11 && conditions[11].isNotEmpty()) {
                sorted.sortedByDescending {
                    it.achievements
                }
            } else {
                sorted
            }.mapIndexed { i: Int, score: MaiScore ->
                score.position = i + 1
                score
            }

            maimaiApiService.insertSongData(scores)
            maimaiApiService.insertMaimaiAliasForScore(scores)

            val filteredScores = fitScoreInDifficulties(
                difficulties,
                fitScoreInRange(
                    rangeInConditions,
                    MaiScoreFilter.filterScores(
                        scores, conditions
                    )
                )
            )

            if (filteredScores.isEmpty()) {
                throw NoSuchElementException.BestScoreFiltered(full.getUser(maiDao).name ?: qq.toString())
            }

            val split = DataUtil.splitPage(filteredScores, page, 50)

            return MaiScoreParam(
                user = full.getUser(maiDao),
                songs = listOf(),
                scores = split.first,
                cabinet = MaiCabinet.ANY,
                statistics = MaiScoreStatistics.getStatistics(filteredScores, split.first),
                page = split.second,
                maxPage = split.third
            )
        }
    }

    private fun Collection<MaiSong>.addDifferentCabinet(): List<MaiSong> {
        val result = ArrayList<MaiSong>(this.size * 2)
        val seenIDs = mutableSetOf<Int>()

        for (song in this) {
            if (!seenIDs.add(song.songID)) continue

            if (song.songID in 0L..20000L) {
                result.add(song)

                // 计算互补 ID（SD ↔ DX）
                val anotherID = if (song.isDeluxe) song.songID - 10000 else song.songID + 10000
                val extraSong = lxMaiApiService.getMaiSong(anotherID)

                if (extraSong != null && extraSong.title == song.title) {
                    if (seenIDs.add(extraSong.songID)) {
                        result.add(extraSong)
                    }
                }
            } else {
                result.add(song)
            }
        }
        return result
    }

    private fun Collection<MaiSong>.filterByCabinet(cabinet: MaiCabinet?): List<MaiSong> {
        // 处理无须过滤的情况
        if (cabinet == null || cabinet == MaiCabinet.ANY) return this.toList()

        return this.filter { song ->
            when (cabinet) {
                MaiCabinet.UTAGE -> song.isUtage
                MaiCabinet.DX -> song.isDeluxe
                MaiCabinet.SD -> !song.isDeluxe
            }
        }
    }

    // 当有多个结果时，询问，并返回默认的那个
    private fun selectSong(event: MessageEvent, cabinet: MaiCabinet, candidate: Map<Double, MaiSong> = emptyMap()): MaiSong? {
        val filtered = candidate.filter { it.key >= 0.4 }

        return selectSong(event, cabinet, filtered.values)
    }

    // 当有多个结果时，询问，并返回默认的那个
    private fun selectSong(event: MessageEvent, cabinet: MaiCabinet, candidate: Collection<MaiSong> = emptyList()): MaiSong? {
        if (candidate.size <= 1) return candidate.firstOrNull()

        val sb = StringBuilder("当前有多个匹配结果，请输入您想要展示的结果：\n")

        val filtered = candidate
            .addDifferentCabinet()
            .filterByCabinet(cabinet)

        if (filtered.size <= 1) return filtered.firstOrNull()

        val tips = filtered.mapIndexed { index, song ->
            "${index + 1}: ${song.getSongPreviewInfo()}" }
            .joinToString("\n")

        sb.append(tips)

        val receipt = event.reply(sb)

        val lock = ASyncMessageUtil.getLock(event, 30 * 1000L)

        val ev = lock.get()
        receipt.recall()

        if (ev != null) {
            val index = ev.rawMessage.trim().toIntOrNull()

            if (index != null) {
                val i = index.coerceIn(1, filtered.size)

                return filtered.toList()[i - 1]
            } else {
                throw TipsException("""
                        请输入正确的索引！
                        操作已中止。
                    """.trimIndent())
            }
        } else {
            return filtered.firstOrNull()
        }
    }

    companion object {
        private fun fitScoreInRange(ranges: List<String>?, scores: List<MaiScore>): List<MaiScore> {
            if (ranges.isNullOrEmpty()) return scores
            return scores.filter { score ->
                ranges.any { range ->
                    MaiScoreFilter.fitRange(Operator.EQ, score.star, range)
                }
            }
        }

        private fun fitScoreInDifficulties(difficulty: String?, scores: List<MaiScore>): List<MaiScore> {
            val difficulties = MaiDifficulty.getDifficulties(difficulty)
                .map { MaiDifficulty.getIndex(it) }.toSet()

            if (difficulties.isEmpty()) return scores

            val filterUtage = difficulties.contains(5)

            return scores.filter { (it.index in difficulties) || (it.isUtage && filterUtage) }
        }


        // UUMS
        fun getSearchResult(text: String?, maimaiApiService: MaimaiApiService): MessageChain {
            val songs = maimaiApiService.getMaimaiSongLibrary()
            val result = mutableMapOf<Double, MaiSong>()

            for (s in songs) {
                val similarity = s.title.getSimilarity(text)

                if (similarity >= 0.4) {
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
