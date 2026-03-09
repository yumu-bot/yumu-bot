package com.now.nowbot.model.osu

import com.fasterxml.jackson.annotation.JsonProperty
import tools.jackson.databind.PropertyNamingStrategies
import tools.jackson.databind.annotation.JsonNaming
import java.time.OffsetDateTime

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
class Kudosu {
    @JsonProperty("id")
    var kudosuID: Long = 0

    // give, vote.give, reset, vote.reset, revoke, or vote.revoke.
    @JsonProperty("action")
    var action: String = ""

    @JsonProperty("amount")
    var amount: Int = 0

    // beatmap_discussion
    @JsonProperty("model")
    var model: String = ""

    @JsonProperty("created_at")
    var createdAt: OffsetDateTime = OffsetDateTime.now()

    @JsonProperty("giver")
    var giver: Giver? = null

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
    data class Giver(
        @field:JsonProperty("url")
        var url: String? = "",

        @field:JsonProperty("username")
        var name: String = ""
    )

    @JsonProperty("post")
    var post: Post? = null

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
    data class Post(
        // 贴子的链接
        @field:JsonProperty("url")
        var url: String? = null,

        // 歌名 [难度名]
        @field:JsonProperty("title")
        var title: String = ""
    )

    @field:JsonProperty("details")
    var details: Details? = null

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
    data class Details(
        @field:JsonProperty("url")
        var event: String? = null,
    )
}
