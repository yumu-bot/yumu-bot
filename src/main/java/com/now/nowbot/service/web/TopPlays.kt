package com.now.nowbot.service.web

import com.now.nowbot.model.osu.LazerScore
import com.now.nowbot.util.DataUtil
import com.now.nowbot.util.JacksonUtil
import java.util.regex.Pattern

data class TopPlays(
    val firstScoreRank: Int,
    val scores: List<LazerScore>
)

private val topPlaysDataPattern: Pattern =
    Pattern.compile("(?s)data-props=\"(?<json>[^\"]+)\"")

fun parseTopPlays(html: String): TopPlays {
    val htmlString = html
        .substringAfter("<div class=\"ranking-page\">")
        .substringBefore("data-react=\"ranking-top-plays\"")
        .trim()

    val topPlaysMatcher = topPlaysDataPattern.matcher(htmlString)

    val scores: List<LazerScore>
    val firstScoreRank: Int

    if (topPlaysMatcher.find()) {
        val json = DataUtil.unescapeHTML(topPlaysMatcher.group("json"))

        val node = JacksonUtil.toNode(json)

        firstScoreRank = node.get("first_score_rank")?.asInt(0) ?: 0

        scores = node.get("scores").mapNotNull { JacksonUtil.parseObject<LazerScore>(it) }
    } else {
        firstScoreRank = -1
        scores = listOf()
    }

    return TopPlays(
        firstScoreRank, scores
    )
}