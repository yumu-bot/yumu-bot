package com.now.nowbot.service.MessageServiceImpl

import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.OsuApiService.OsuBeatmapApiService
import java.util.*
import java.util.regex.Pattern
import org.springframework.stereotype.Service

@Service("SEARCH")
class SearchService(val beatmapApiService: OsuBeatmapApiService) :
        MessageService<SearchService.SearchParam> {

    data class SearchParam(
            val artist: String?,
            val title: String?,
            val creator: String?,
            val difficulty: String?,
            val status: String?,
            val sort: String?,
    )

    override fun isHandle(
            event: MessageEvent,
            messageText: String,
            data: MessageService.DataValue<SearchParam>
    ): Boolean {
        val param = constructParam(messageText)
        return false
    }

    override fun HandleMessage(event: MessageEvent, param: SearchParam) {
        val query = constructQuery(param)
        val result = beatmapApiService.searchBeatmap(query)
        // do nothing
    }

    companion object {
        private fun constructParam(messageText: String): SearchParam {
            // Nishigomi Kakumi - Hyakka Ryouran (SugiuraAyano) [Kantan]
            
            val creator = Pattern.compile("\\s*\\((?<creator>\\w+)\\)\\s*")
            var str = messageText
            return SearchParam(null, null, null, null, null, null)
        }

        private fun constructQuery(param: SearchParam): HashMap<String, Any> {
            val query = HashMap<String, Any>()

            var q = ""
            q.addQuery(param.artist, "artist")
            q.addQuery(param.title, "title")
            q.addQuery(param.creator, "creator")

            query["q"] = q
            query["sort"] = getSort(param.sort)
            query["s"] = getStatus(param.status)
            query["page"] = 1

            return query
        }

        private fun getGenre(genre: String?): String {
            return when (genre?.lowercase()) {
                "u",
                "un",
                "uns",
                "unspecified" -> "unspecified"
                "v",
                "vg",
                "vgm",
                "videogame",
                "video game" -> "video game"
                "a",
                "an",
                "ani",
                "manga",
                "anime" -> "anime"
                "r",
                "rk",
                "rock" -> "rock"
                "p",
                "pp",
                "pop" -> "pop"
                "o",
                "ot",
                "oth",
                "other" -> "other"
                "n",
                "nv",
                "nvt",
                "novel",
                "novelty" -> "novelty"
                "h",
                "hh",
                "hip",
                "hop",
                "hiphop",
                "hip hop" -> "hip hop"
                "e",
                "el",
                "ele",
                "elect",
                "electro",
                "electric",
                "electronic" -> "electronic"
                "m",
                "mt",
                "mtl",
                "metal" -> "metal"
                "c",
                "cl",
                "cls",
                "classic",
                "classical" -> "classical"
                "f",
                "fk",
                "folk" -> "folk"
                "j",
                "jz",
                "jazz" -> "jazz"
                else -> "any"
            }
        }

        private fun getLanguage(language: String?): String {
            return when (language?.lowercase()) {
                "c",
                "cn",
                "chn",
                "china",
                "chinese" -> "chinese"
                "e",
                "en",
                "gb",
                "eng",
                "gbr",
                "england",
                "english" -> "english"
                "f",
                "fr",
                "fra",
                "france",
                "french" -> "french"
                "g",
                "ge",
                "ger",
                "germany",
                "german" -> "german"
                "t",
                "it",
                "ita",
                "italy",
                "italian" -> "italian"
                "j",
                "ja",
                "jpn",
                "japan",
                "japanese" -> "japanese"
                "k",
                "kor",
                "korea",
                "korean" -> "korean"
                "s",
                "sp",
                "esp",
                "spa",
                "spain",
                "spanish" -> "spanish"
                "w",
                "sw",
                "swe",
                "sweden",
                "swedish" -> "swedish"
                "r",
                "ru",
                "rus",
                "russia",
                "russian" -> "russian"
                "p",
                "po",
                "pol",
                "poland",
                "polish" -> "polish"
                "i",
                "in",
                "ins",
                "instrument",
                "instrumental" -> "instrumental"
                "u",
                "un",
                "uns",
                "unspecify",
                "unspecified" -> "unspecified"
                else -> "any"
            }
        }

        private fun getStatus(status: String?): String {
            return when (status?.lowercase()) {
                "0",
                "p",
                "pend",
                "pending" -> "pending"
                "1",
                "r",
                "rnk",
                "rank",
                "ranked" -> "ranked"
                "2",
                "a",
                "app",
                "approved" -> "approved"
                "3",
                "q",
                "qua",
                "qualified" -> "qualified"
                "4",
                "l",
                "lvd",
                "loved" -> "loved"
                "-1",
                "5",
                "w",
                "wip",
                "inprogress",
                "in progress",
                "workinprogress",
                "work in progress" -> "wip"
                "-2",
                "6",
                "g",
                "gra",
                "grave",
                "graveyard" -> "graveyard"
                else -> "any"
            }
        }

        private fun getSort(sort: String?): String {
            return when (sort?.lowercase()) {
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

/** name="param" */
private fun String.addQuery(param: String?, name: String) {
    if ((param ?: "").isNotBlank()) this.plus("${name}=${param} ")
}
