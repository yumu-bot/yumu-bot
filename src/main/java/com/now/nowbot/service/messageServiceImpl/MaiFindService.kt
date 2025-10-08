package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.model.enums.MaiDifficulty
import com.now.nowbot.model.enums.Operator
import com.now.nowbot.model.filter.MaiScoreFilter
import com.now.nowbot.model.filter.MaiSongFilter
import com.now.nowbot.model.maimai.MaiSong
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.divingFishApiService.MaimaiApiService
import com.now.nowbot.service.messageServiceImpl.MaiFindService.MaiFindParam
import com.now.nowbot.throwable.botRuntimeException.NoSuchElementException
import com.now.nowbot.util.DataUtil
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.command.*
import org.springframework.stereotype.Service
import java.util.regex.Matcher

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

    override fun handleMessage(event: MessageEvent, param: MaiFindParam) {
        val image = imageService.getPanel(param.toMap(), "MF")

        event.reply(image)
    }

    private fun getParam(matcher: Matcher): MaiFindParam {
        val any: String = (matcher.group(FLAG_NAME) ?: "").trim()

        val conditions = DataUtil.paramMatcher(any, MaiSongFilter.entries.map { it.regex }, MaiSongFilter.RANGE.regex)

        val rangeInConditions = conditions.lastOrNull()?.firstOrNull()
        val hasRangeInConditions = (rangeInConditions.isNullOrEmpty().not())
        val hasCondition = conditions.dropLast(1).sumOf { it.size } > 0

        val songs: List<MaiSong>

        val isRange = any.matches(REG_MAI_RANGE.toRegex())

        if (!hasRangeInConditions && !hasCondition && !isRange) {
            val title = any.trim()

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

            val filteredSongs = if (hasCondition) {
                MaiSongFilter.filterSongs(all, conditions, difficulties)
            } else {
                all
            }

            songs = if (hasRangeInConditions) {
                fitSongInRange(rangeInConditions, filteredSongs, difficulties)
            } else {
                filteredSongs
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

        /**
         * @param difficulties 通过冒号输入的难度
         */
        private fun fitSongInRange(
            range: String?,
            songs: List<MaiSong>,
            difficulties: List<MaiDifficulty>
        ): List<MaiSong> {
            val diffs = difficulties.map { MaiDifficulty.getIndex(it) }

            return songs.filter { s ->
                val result = s.star.mapIndexed { i: Int, sr: Double ->
                    val isLevel = diffs.isEmpty() || diffs.contains(i) || (s.isUtage && diffs.contains(5))
                    val inRange = MaiScoreFilter.fitRange(Operator.EQ, range, sr)

                    i to (isLevel && inRange)
                }.filter {
                    it.second
                }.map {
                    it.first
                }

                val boolean = result.isNotEmpty()

                if (boolean) {
                    s.updateHighlight(result)
                }

                boolean
            }
        }
    }
}
