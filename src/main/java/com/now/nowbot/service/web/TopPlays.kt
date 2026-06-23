package com.now.nowbot.service.web

import com.now.nowbot.model.osu.LazerScore
import com.now.nowbot.util.JacksonUtil
import org.jsoup.Jsoup

data class TopPlays(
    val firstScoreRank: Int,
    val scores: List<LazerScore>
)

fun parseTopPlays(html: String): TopPlays {
    // 1. 使用 Jsoup 解析 HTML 文本
    val document = Jsoup.parse(html)

    // 2. 使用 CSS 选择器定位元素：寻找拥有 data-props 属性且 class 包含 ranking-page 的元素
    // 如果该属性就在 .ranking-page 本身，可以用 ".ranking-page[data-props]"
    // 如果它在子元素，可以用 ".ranking-page [data-props]"
    val element = document.selectFirst(".ranking-page[data-props]")
        ?: document.selectFirst("[data-props]")

    // 3. 如果找到了节点，提取属性并用 Jackson 解析
    if (element != null) {
        val json = element.attr("data-props")

        if (json.isNotBlank()) {
            val node = JacksonUtil.toNode(json)
            val firstScoreRank = node.get("first_score_rank")?.asInt(0) ?: 0
            val scores = node.get("scores")?.mapNotNull { JacksonUtil.parseObject<LazerScore>(it) } ?: listOf()

            return TopPlays(firstScoreRank, scores)
        }
    }

    // 4. 未找到或解析失败时的默认返回值
    return TopPlays(-1, listOf())
}