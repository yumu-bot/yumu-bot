package com.now.nowbot.entity

import com.fasterxml.jackson.annotation.JsonProperty
import com.now.nowbot.model.osu.MicroUser
import java.time.OffsetDateTime

data class Team(
    @field:JsonProperty("flag_url") val flagUrl: String?,
    @field:JsonProperty("id") val teamID: Long,
    @field:JsonProperty("name") val name: String?,
    @field:JsonProperty("short_name") val shortName: String,

    // 部分情况下才有
    @field:JsonProperty("empty_slots") val emptySlots: Int?,
    @field:JsonProperty("leader") val leader: MicroUser?,
    @field:JsonProperty("members") val members: List<MicroUser> = emptyList(),
    @field:JsonProperty("statistics") val statistics: TeamStatistics?,

    // Extended 模式下才有
    @field:JsonProperty("cover_url") val coverUrl: String?,
    @field:JsonProperty("created_at") val createdAt: OffsetDateTime?,
    @field:JsonProperty("default_ruleset_id") val defaultRuleset: Byte?,
    @field:JsonProperty("description") val description: String?,
    @field:JsonProperty("is_open") val open: Boolean?,
) {

    @get:JsonProperty("markdown_description")
    val markdownDescription: String
        get() = description?.toMarkdown() ?: ""

    private fun String?.toMarkdown(): String {
        var result = this ?: return ""

        // 1. 处理标题 [heading]内容[/heading] -> ### 内容
        result = result.replace(Regex("\\[heading](.*?)\\[/heading]", RegexOption.IGNORE_CASE)) {
            "\n### ${it.groupValues[1]}\n"
        }

        // 2. 处理加粗 [b]内容[/b] -> **内容**
        result = result.replace(Regex("\\[b](.*?)\\[/b]", RegexOption.IGNORE_CASE)) {
            "**${it.groupValues[1]}**"
        }

        // 3. 处理居中（Markdown 不原生支持居中，通常直接去掉标签或换行）
        result = result.replace(Regex("\\[/?center]", RegexOption.IGNORE_CASE), "\n")

        // 4. 处理链接 [url=https://...]内容[/url] -> [内容](https://...)
        result = result.replace(Regex("\\[url=(.*?)](.*?)\\[/url]", RegexOption.IGNORE_CASE)) {
            "[${it.groupValues[2]}](${it.groupValues[1]})"
        }

        // 最后：清理多余的换行
        return result.replace(Regex("\\n{3,}"), "\n\n").trim()
    }
}

data class TeamStatistics(
    @field:JsonProperty("team_id") val teamID: Int,
    @field:JsonProperty("ruleset_id") val ruleset: Byte,
    @field:JsonProperty("play_count") val playCount: Long,
    @field:JsonProperty("ranked_score") val rankedScore: Long,
    @field:JsonProperty("performance") val performance: Int,
    @field:JsonProperty("rank") val rank: Int? // 在设置了模式时能够返回
)