package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.json.BeatMapSetSearch
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.throwable.serviceException.QualifiedMapException
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.command.FLAG_MODE
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.*
import java.util.regex.Matcher
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

@Service("QUALIFIED_MAP")
class QualifiedMapService(
    private val beatmapApiService: OsuBeatmapApiService,
    private val imageService: ImageService,
) : MessageService<Matcher> {

    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: DataValue<Matcher>,
    ): Boolean {
        val m = Instruction.QUALIFIED_MAP.matcher(messageText)
        if (m.find()) {
            data.value = m
            return true
        } else return false
    }

    @Throws(Throwable::class)
    override fun HandleMessage(event: MessageEvent, matcher: Matcher) {
        // 获取参数
        val statusStr = matcher.group("status") ?: "q"
        val sort = matcher.group("sort") ?: "ranked_asc"
        val rangeStr = matcher.group("range") ?: "12"
        val mode = OsuMode.getMode(matcher.group(FLAG_MODE)).modeValue

        val range = try {
            rangeStr.toInt()
        } catch (e: Exception) {
            throw QualifiedMapException(QualifiedMapException.Type.Q_Parameter_Error)
        }

        if (range < 1 || range > 999) {
            throw QualifiedMapException(QualifiedMapException.Type.Q_Parameter_OutOfRange)
        }

        var page = 1
        val pageAim =
            max(floor((range / 50.0)) + 1.0, 10.0)
                .toInt() // 这里需要重复获取，page最多取10页（500个），总之我不知道怎么实现

        val query = hashMapOf<String, Any>()
        val status = getStatus(statusStr)

        query["m"] = mode
        query["s"] = status
        query["sort"] = getSort(sort)
        query["page"] = page

        try {
            var search: BeatMapSetSearch? = null
            var resultCount = 0
            do {
                if (search == null) {
                    search = beatmapApiService.searchBeatMapSet(query)
                    resultCount += search.beatmapSets.size
                    continue
                }
                page++
                query["page"] = page

                val result = beatmapApiService.searchBeatMapSet(query)

                resultCount += result.beatmapSets.size

                search.beatmapSets.addAll(result.beatmapSets)

                if (result.cursor != null) {
                    search.cursor = result.cursor
                }

                if (result.cursorString != null) {
                    search.cursorString = result.cursorString
                }

            } while (resultCount < (search?.total ?: 0) && page < pageAim)

            if (resultCount == 0 || search == null) {
                throw QualifiedMapException(QualifiedMapException.Type.Q_Result_NotFound)
            }

            search.resultCount = min(search.total, range)
            search.rule = status
            search.sortBeatmapDiff()

            val img = imageService.getPanelA2(search)
            event.getSubject().sendImage(img)
        } catch (e: Exception) {
            log.error("过审谱面：", e)
            throw QualifiedMapException(QualifiedMapException.Type.Q_Send_Error)
        }
    }

    companion object {
        private fun BeatMapSetSearch.sortBeatmapDiff() = BeatMapSetSearch.sortBeatmapDiff(this)

        private val log: Logger = LoggerFactory.getLogger(QualifiedMapService::class.java)

        private fun getStatus(status: String): String {
            return when (status.lowercase(Locale.getDefault())) {
                "0",
                "p" -> "pending"
                "1",
                "r" -> "ranked"
                "2",
                "a" -> "approved"
                "4",
                "l" -> "loved"
                "-1",
                "5",
                "w" -> "wip"
                "-2",
                "6",
                "g" -> "graveyard"
                else -> "qualified"
            }
        }

        private fun getSort(sort: String): String {
            return when (sort.lowercase(Locale.getDefault())) {
                "t",
                "t+",
                "ta",
                "title",
                "title asc",
                "title_asc" -> "title_asc"
                "t-",
                "td",
                "title desc",
                "title_desc" -> "title_desc"
                "a",
                "a+",
                "aa",
                "artist",
                "artist asc",
                "artist_asc" -> "artist_asc"
                "a-",
                "ad",
                "artist desc",
                "artist_desc" -> "artist_desc"
                "d",
                "d+",
                "da",
                "difficulty",
                "difficulty asc",
                "difficulty_asc",
                "s",
                "s+",
                "sa",
                "star",
                "star asc",
                "star_asc" -> "difficulty_asc"
                "d-",
                "dd",
                "difficulty desc",
                "difficulty_desc",
                "s-",
                "sd",
                "star desc",
                "star_desc" -> "difficulty_desc"
                "m",
                "m+",
                "ma",
                "map",
                "rating",
                "rating asc",
                "rating_asc" -> "rating_asc"
                "m-",
                "md",
                "map desc",
                "rating desc",
                "rating_desc" -> "rating_desc"
                "p",
                "p+",
                "pa",
                "plays",
                "pc asc",
                "plays asc",
                "plays_asc" -> "plays_asc"
                "p-",
                "pd",
                "pc desc",
                "plays desc",
                "plays_desc" -> "plays_desc"
                "r",
                "r+",
                "ra",
                "ranked",
                "time asc",
                "ranked asc",
                "ranked_asc" -> "ranked_asc"
                "r-",
                "rd",
                "time desc",
                "ranked desc",
                "ranked_desc" -> "ranked_desc"
                else -> "relevance_desc"
            }
        }
    }
}
