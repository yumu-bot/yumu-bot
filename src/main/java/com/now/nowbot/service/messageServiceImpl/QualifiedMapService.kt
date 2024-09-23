package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.jsonData.BeatMapSetSearch
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.throwable.serviceException.QualifiedMapException
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.command.FLAG_MODE
import java.lang.Exception
import java.util.HashMap
import java.util.Locale
import java.util.regex.Matcher
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

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
            data.setValue(m)
            return true
        } else return false
    }

    @Throws(Throwable::class)
    override fun HandleMessage(event: MessageEvent, matcher: Matcher) {
        // 获取参数
        val modeStr: String? = matcher.group(FLAG_MODE)
        var status = matcher.group("status")
        var sort = matcher.group("sort")
        val rangeStr = matcher.group("range")
        var range: Short
        var mode = OsuMode.DEFAULT.getModeValue()

        if (modeStr != null) mode = OsuMode.getMode(modeStr).getModeValue()
        if (status == null) status = "q"
        if (sort == null) sort = "ranked_asc"

        if (rangeStr == null) {
            range = 12 // 从0开始
        } else {
            try {
                range = rangeStr.toShort()
            } catch (e: Exception) {
                throw QualifiedMapException(QualifiedMapException.Type.Q_Parameter_Error)
            }
        }

        if (range < 1 || range > 999)
            throw QualifiedMapException(QualifiedMapException.Type.Q_Parameter_OutOfRange)

        var page = 1
        val page_aim =
            max(floor((range / 50f).toDouble()) + 1, 10.0)
                .toInt() // 这里需要重复获取，page最多取10页（500个），总之我不知道怎么实现

        val query = HashMap<String?, Any?>()
        status = getStatus(status)
        query.put("m", mode)
        query.put("s", status)
        query.put("sort", getSort(sort))
        query.put("page", page)

        try {
            var data: BeatMapSetSearch? = null
            var resultCount = 0
            do {
                if (data == null) {
                    data = beatmapApiService.searchBeatMapSet(query)
                    resultCount += data.beatmapSets.size
                    continue
                }
                page++
                query.put("page", page)
                val result = beatmapApiService.searchBeatMapSet(query)
                resultCount += result.beatmapSets.size
                data.beatmapSets.addAll(result.beatmapSets)
            } while (resultCount < data.getTotal() && page < page_aim)

            if (resultCount == 0)
                throw QualifiedMapException(QualifiedMapException.Type.Q_Result_NotFound)

            data.resultCount = min(data.getTotal().toDouble(), range.toDouble()).toInt()
            data.setRule(status)
            data.sortBeatmapDiff()
            val img = imageService.getPanelA2(data)
            event.getSubject().sendImage(img)
        } catch (e: Exception) {
            log.error("QuaMap: ", e)
            throw QualifiedMapException(QualifiedMapException.Type.Q_Send_Error)
        }
    }

    companion object {
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
