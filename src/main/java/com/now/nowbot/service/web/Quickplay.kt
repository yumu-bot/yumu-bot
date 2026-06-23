package com.now.nowbot.service.web

import com.fasterxml.jackson.annotation.JsonProperty
import com.now.nowbot.model.multiplayer.RoomInfo
import com.now.nowbot.model.osu.OsuUser
import com.now.nowbot.util.JacksonUtil
import org.jsoup.Jsoup

data class Quickplay(
    @field:JsonProperty("rooms")
    val rooms: List<RoomInfo> = emptyList(),

    @field:JsonProperty("type_group")
    val typeGroup: String = "quickplay",

    @field:JsonProperty("cursor_string")
    val cursorString: String? = null,
)

data class QuickplaySummary(
    @field:JsonProperty("active")
    val active: Quickplay = Quickplay(),

    @field:JsonProperty("ended")
    val ended: Quickplay = Quickplay(),
)

data class QuickplayLeaderboardItem(

    @field:JsonProperty("id")
    val userID: Long,

    @field:JsonProperty("username")
    val username: String,

    @field:JsonProperty("country")
    val country: OsuUser.Country,

    @field:JsonProperty("wins")
    val wins: Int,

    @field:JsonProperty("playcount")
    val playCount: Int,

    @field:JsonProperty("rating")
    val rating: Int,

    @field:JsonProperty("rank")
    val rank: Int,
)

fun parseQuickplayLeaderboard(htmlContent: String): List<QuickplayLeaderboardItem> {
    val document = Jsoup.parse(htmlContent)

    // 1. 获取所有的网格行内容 (排除掉作为表头的 header 行)
    val rows = document.select(".ranking-page-grid-item:not(.ranking-page-grid-item--header) .ranking-page-grid-item__content")

    val rankingList = ArrayList<QuickplayLeaderboardItem>(rows.size)

    // 2. 循环遍历每一行
    for (row in rows) {
        try {
            val cols = row.select(".ranking-page-grid-item__col")

            // 确保这行有足够的数据列 (排位页面通常有 5 列)
            if (cols.size < 5) continue

            // 第一列: 排名 (例如: "#1")
            val rank = cols[0]
                ?.text()
                ?.trim()
                ?.replace("#", "")
                ?.toIntOrNull() ?: 0

            // 第二列: 玩家信息 (包含国旗和名字)
            val username = cols[1]
                ?.select(".ranking-page-table-main__link")
                ?.text()
                ?.trim() ?: "Unknown"

            val userID = cols[1]
                ?.selectFirst(".ranking-page-table-main__link")
                ?.attr("data-user-id")
                ?.toLongOrNull() ?: 0L

            val countryName = cols[1]
                ?.selectFirst(".flag-country")
                ?.attr("title")?.trim() ?: "Unknown"

            val countryCode = cols[1]
                ?.selectFirst("a:has(.flag-country)")
                ?.attr("href")
                ?.substringAfter("country=")
                ?.substringBefore("&")
                ?.trim() ?: "XX"

            // 第三列: 胜场 (Wins)
            val wins = cols[2]?.text()?.trim()?.replace(",", "")?.toIntOrNull() ?: 0

            // 第四列: 所有场 / 游玩次数 (Plays)
            val playCount = cols[3]?.text()?.trim()?.replace(",", "")?.toIntOrNull() ?: 0

            // 第五列: 排位分数 (Rating) - 注意有些分数带有星号(例如: 2,549*)，需要把星号也替换掉
            val rating = (cols[4]?.text()?.trim() ?: "")
                .replace(",", "")
                .replace("*", "")
                .toIntOrNull() ?: 0

            rankingList.add(
                QuickplayLeaderboardItem(
                    userID = userID,
                    username = username,
                    country = OsuUser.Country(countryCode, countryName),
                    wins = wins,
                    playCount = playCount,
                    rating = rating,
                    rank = rank,
                )
            )
        } catch (e: Exception) {
            println("解析某行数据时出错: ${e.message}")
        }
    }

    return rankingList
}

fun parseQuickplay(html: String): QuickplaySummary? {
    // 1. 使用 Jsoup 解析整个 HTML 文本
    val document = Jsoup.parse(html)

    // 2. 使用 id 选择器直接精确定位到对应的 script 标签
    val element = document.getElementById("json-user-multiplayer-index") ?: return null

    // 3. 提取标签内部的文本
    // 对于 script 标签，使用 .data() 或 .html() 可以最安全地获取未被转义的纯文本 JSON
    val json = element.data().trim()

    if (json.isBlank()) return null

    // 4. 使用 Jackson 反序列化为对象
    return try {
        JacksonUtil.parseObject<QuickplaySummary>(json)
    } catch (_: Exception) {
        null
    }
}