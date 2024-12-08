package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.model.json.BeatMapSetSearch
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.throwable.GeneralTipsException
import com.now.nowbot.util.Instruction
import org.springframework.stereotype.Service
import java.util.regex.Pattern

@Service("SEARCH") class SearchService(
    private val beatmapApiService: OsuBeatmapApiService, private val imageService: ImageService
) : MessageService<SearchService.SearchParam> {

    data class SearchParam(
        val artist: String?,
        val title: String?,
        val creator: String?,
        val difficulty: String?,
        val status: String?,
        val sort: String?,
        val genre: String?,
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
                Pattern.compile("([:：](\")?(?<genre>$REG_ANYTHING)(\")?)?\\s*((\")?(?<artist>$REG_ANYTHING)(\")?\\s*-\\s*)?\\s*((\")?(?<title>$REG_ANYTHING)(\")?)?\\s*(\\((\")?(?<creator>$REG_ANYTHING)(\")?\\))?\\s*(\\[(\")?(?<difficulty>$REG_ANYTHING)(\")?])?\\s*(#(\")?(?<status>$REG_ANYTHING)(\")?#?)?\\s*(\\*(\")?(?<sort>$REG_ANYTHING)(\")?\\*?)?\\s*")
                    .matcher(str)

            if (!matcher.find()) return null

            val artist = matcher.group("artist")
            val title = matcher.group("title")
            val creator = matcher.group("creator")
            val difficulty = matcher.group("difficulty")
            val status = matcher.group("status")
            val sort = matcher.group("sort")
            val genre = matcher.group("genre")

            return SearchParam(artist, title, creator, difficulty, status, sort, genre)
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
                ), "sort" to getSort(param.sort), "page" to 1
            )

            getStatus(param.status)?.let { query["s"] = it }
            getGenre(param.genre)?.let { query["g"] = it } // getLanguage(param.language)?.let { query["l"] = it }

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
                "sort" to getSort(param.sort), "page" to 1
            )

            getStatus(param.status)?.let { query["s"] = it }
            getGenre(param.genre)?.let { query["g"] = it } // getLanguage(param.language)?.let { query["l"] = it }

            return query
        }

        private fun getGenre(genre: String?): Byte? {
            return when (genre?.lowercase()) {
                "u", "un", "uns", "unspecified" -> 1
                "v", "vg", "vgm", "videogame", "video game" -> 2
                "a", "an", "ani", "manga", "anime" -> 3
                "r", "rk", "rock" -> 4
                "p", "pp", "pop" -> 5
                "o", "ot", "oth", "other" -> 6
                "n", "nv", "nvt", "novel", "novelty" -> 7
                "h", "hh", "hip", "hop", "hiphop", "hip hop" -> 9
                "e", "el", "ele", "elect", "electro", "electric", "electronic" -> 10
                "m", "mt", "mtl", "metal" -> 11
                "c", "cl", "cls", "classic", "classical" -> 12
                "f", "fk", "folk" -> 13
                "j", "jz", "jazz" -> 14
                else -> null
            }
        }

        private fun getLanguage(language: String?): Byte? {
            return when (language?.lowercase()) {
                "c", "cn", "chn", "china", "chinese" -> 4
                "e", "en", "gb", "eng", "gbr", "england", "english" -> 2
                "f", "fr", "fra", "france", "french" -> 7
                "g", "ge", "ger", "germany", "german" -> 8
                "t", "it", "ita", "italy", "italian" -> 11
                "j", "ja", "jpn", "japan", "japanese" -> 3
                "k", "kr", "kor", "korea", "korean" -> 6
                "s", "sp", "esp", "spa", "spain", "spanish" -> 10
                "w", "sw", "swe", "sweden", "swedish" -> 9
                "r", "ru", "rus", "russia", "russian" -> 12
                "p", "po", "pol", "poland", "polish" -> 13
                "i", "in", "ins", "instrument", "instrumental" -> 5
                "u", "un", "uns", "unspecified" -> 1
                "o", "ot", "oth", "any", "other", "others" -> 14
                else -> null
            }
        }

        private fun getStatus(status: String?): String? {
            return when (status?.lowercase()) {
                "0", "p", "pend", "pending" -> "pending"
                "1", "r", "rnk", "rank", "ranked" -> "ranked"
                "2", "a", "app", "approved" -> "approved"
                "3", "q", "qua", "qualified" -> "qualified"
                "4", "l", "lvd", "loved" -> "loved"
                "-1", "5", "w", "wip", "inprogress", "in progress", "workinprogress", "work in progress" -> "wip"
                "-2", "6", "g", "gra", "grave", "graveyard" -> "graveyard"
                "f", "fav", "favorite", "favorites" -> "favorites"
                "h", "has", "leader", "leaderboard", "has leaderboard" -> null
                else -> "any"
            }
        }

        private fun getSort(sort: String?): String {
            return when (sort?.lowercase()) {
                "t", "t+", "ta", "title", "title asc", "title_asc" -> "title_asc"
                "t-", "td", "title desc", "title_desc" -> "title_desc"
                "a", "a+", "aa", "artist", "artist asc", "artist_asc" -> "artist_asc"
                "a-", "ad", "artist desc", "artist_desc" -> "artist_desc"
                "d", "d+", "da", "difficulty", "difficulty asc", "difficulty_asc", "s", "s+", "sa", "star", "star asc", "star_asc" -> "difficulty_asc"
                "d-", "dd", "difficulty desc", "difficulty_desc", "s-", "sd", "star desc", "star_desc" -> "difficulty_desc"
                "m", "m+", "ma", "map", "rating", "rating asc", "rating_asc" -> "rating_asc"
                "m-", "md", "map desc", "rating desc", "rating_desc" -> "rating_desc"
                "p", "p+", "pa", "plays", "pc asc", "plays asc", "plays_asc" -> "plays_asc"
                "p-", "pd", "pc desc", "plays desc", "plays_desc" -> "plays_desc"
                "r", "r+", "ra", "ranked", "time asc", "ranked asc", "ranked_asc" -> "ranked_asc"
                "r-", "rd", "time desc", "ranked desc", "ranked_desc" -> "ranked_desc"
                else -> "relevance_desc"
            }
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

