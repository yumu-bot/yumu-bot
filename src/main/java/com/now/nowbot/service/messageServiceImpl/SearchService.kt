package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.json.BeatMapSetSearch
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.throwable.GeneralTipsException
import com.now.nowbot.util.DataUtil
import com.now.nowbot.util.Instruction
import org.springframework.stereotype.Service
import java.util.regex.Pattern

@Service("SEARCH") class SearchService(
    private val beatmapApiService: OsuBeatmapApiService, private val imageService: ImageService
) : MessageService<SearchService.SearchParam> {

    data class SearchParam(
        val mode: String?,
        val status: String?,
        val artist: String?,
        val title: String?,
        val creator: String?,
        val difficulty: String?,
        val genre: String?,
        val language: String?,
        val sort: String?,
    )

    override fun isHandle(
        event: MessageEvent, messageText: String, data: MessageService.DataValue<SearchParam>
    ): Boolean {
        val matcher = Instruction.SEARCH.matcher(messageText)

        if (!matcher.find()) return false

        val param = constructParam(matcher.group("text")) ?: return false

        data.value = param
        return true
    }

    override fun HandleMessage(event: MessageEvent, param: SearchParam) {
        val query = constructQuery(param)
        var result: BeatMapSetSearch

        try {
            result = beatmapApiService.searchBeatMapSet(query)
        } catch (e: Exception) {
            try {
                result = beatmapApiService.searchBeatMapSet(constructQueryAlternative(param))
            } catch (e: Exception) {
                throw GeneralTipsException(GeneralTipsException.Type.G_Null_Result)
            }
        }

        if (result.beatmapSets.isEmpty()) {
            try {
                result = beatmapApiService.searchBeatMapSet(constructQueryAlternative(param))
            } catch (e: Exception) {
                throw GeneralTipsException(GeneralTipsException.Type.G_Null_Result)
            }
        }

        event.replyImage(result, imageService)
    }

    companion object {
        private const val REG_ANYTHING = "[^\\-\\[\\]\"“”()（）【】—]+"

        // 拆分查询的歌曲名
        private fun constructParam(text: String?): SearchParam? { // Nishigomi Kakumi - Hyakka Ryouran (SugiuraAyano) [Kantan]
            val str = (text ?: return null).trim()

            val matcher =
                Pattern.compile("\\s*([:：](\")?(?<mode>$REG_ANYTHING)(\")?)?\\s*([#＃](\")?(?<status>$REG_ANYTHING)(\")?)?\\s*((\")?(?<artist>$REG_ANYTHING)(\")?\\s*-\\s*)?\\s*((\")?(?<title>$REG_ANYTHING)(\")?)?\\s*(\\((\")?(?<creator>$REG_ANYTHING)(\")?\\))?\\s*(\\[(\")?(?<difficulty>$REG_ANYTHING)(\")?])?\\s*(&(\")?(?<genre>$REG_ANYTHING)(\")?&?)?\\s*(\\+(\")?(?<language>$REG_ANYTHING)(\")?\\+?)?\\s*(\\*(\")?(?<sort>$REG_ANYTHING)(\")?\\*?)?\\s*")
                    .matcher(str)

            if (!matcher.find()) return null

            val mode = matcher.group("status")
            val status = matcher.group("status")
            val artist = matcher.group("artist")
            val title = matcher.group("title")
            val creator = matcher.group("creator")
            val difficulty = matcher.group("difficulty")
            val genre = matcher.group("genre")
            val language = matcher.group("language")
            val sort = matcher.group("sort")

            return SearchParam(mode, status, artist, title, creator, difficulty, genre, language, sort)
        }

        private fun constructQuery(param: SearchParam): Map<String, Any> {
            val query = mutableMapOf<String, Any>(
                "q" to getParam(
                    mapOf(
                        "artist" to param.artist,
                        "title" to param.title,
                        "creator" to param.creator,
                        "difficulty" to param.difficulty,
                    )
                ),
                "sort" to DataUtil.getSort(param.sort),
                "m" to OsuMode.getMode(param.mode),
                "page" to 1
            )

            DataUtil.getStatus(param.status)?.let { query["s"] = it }
            DataUtil.getGenre(param.genre)?.let { query["g"] = it }
            DataUtil.getLanguage(param.language)?.let { query["l"] = it }

            return query
        }

        // 预先的分割方式可能会导致 Hitogata (TV Size) 这样的搜索被错误分割，此时重构原查询
        private fun constructQueryAlternative(param: SearchParam): Map<String, Any> {
            val sb = StringBuilder()

            sb.apply {
                param.artist?.let {
                    append("$it - ")
                }

                param.title?.let {
                    append("$it ")
                }

                param.creator?.let {
                    append("($it) ")
                }

                param.difficulty?.let {
                    append("[$it]")
                }
            }


            val query = mutableMapOf<String, Any>(
                "q" to sb.toString().trim(),
                "sort" to DataUtil.getSort(param.sort), "page" to 1
            )

            DataUtil.getStatus(param.status)?.let { query["s"] = it }
            DataUtil.getGenre(param.genre)?.let { query["g"] = it } // getLanguage(param.language)?.let { query["l"] = it }

            return query
        }

        /** name="param" */
        private fun getParam(params: Map<String, Any?>): String {
            val map = params.filterNot { it.value == null }

            // 如果只有一个 title 参数，那么不用按格式搜寻
            return if (map.size == 1 && map["title"] != null) {
                map.firstNotNullOfOrNull { it.value.toString() } ?: ""
            } else {
                map.map { it.key + "=\"\"" + it.value + "\"\"" }.joinToString(" ")
            }
        }

        // 临时的输出
        private fun MessageEvent.replyText(result: BeatMapSetSearch) {
            val sb = StringBuilder()

            sb.apply {
                append("可能的结果:")

                result.beatmapSets.subList(0, 10).forEach {
                    append("\n")
                    append(it.previewName)
                }
            }

            this.reply(sb.toString())
        }

        private fun MessageEvent.replyImage(result: BeatMapSetSearch, imageService: ImageService) {
            result.beatmapSets = result.beatmapSets.subList(0, 12)

            val data = imageService.getPanel(result, "A8")

            this.reply(data)
        }
    }
}

