package com.now.nowbot.model.osu

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import tools.jackson.databind.PropertyNamingStrategies
import tools.jackson.databind.annotation.JsonNaming
import java.time.OffsetDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
class BeatmapsetDiscussionPost(
    @field:JsonProperty("beatmapset_discussion_id")
    val discussionID: Long,

    @field:JsonProperty("created_at")
    val createdAt: OffsetDateTime,

    @field:JsonProperty("deleted_at")
    val deletedAt: OffsetDateTime?,

    @field:JsonProperty("deleted_by_id")
    val deletedBy: Long,

    @field:JsonProperty("id")
    val postID: Long,

    @field:JsonProperty("last_editor_id")
    val lastEditorID: Long,

    @field:JsonProperty("message")
    val message: String,

    @field:JsonProperty("system")
    val system: Boolean,

    @field:JsonProperty("updated_at")
    val updatedAt: OffsetDateTime,

    @field:JsonProperty("user_id")
    val userID: Long,
)
