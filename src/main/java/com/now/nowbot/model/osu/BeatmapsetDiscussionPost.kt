package com.now.nowbot.model.osu

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.OffsetDateTime

@JsonInclude(JsonInclude.Include.NON_NULL) @JsonIgnoreProperties(ignoreUnknown = true)
class BeatmapsetDiscussionPost(
    @JsonProperty("beatmapset_discussion_id")
    val discussionID: Long,

    @JsonProperty("created_at")
    val createdAt: OffsetDateTime,

    @JsonProperty("deleted_at")
    val deletedAt: OffsetDateTime,

    @JsonProperty("deleted_by_id")
    val deletedBy: Long,

    @JsonProperty("id")
    val postID: Long,

    @JsonProperty("last_editor_id")
    val lastEditorID: Long,

    @JsonProperty("message")
    val message: String,

    @JsonProperty("system")
    val system: Boolean,

    @JsonProperty("updated_at")
    val updatedAt: OffsetDateTime,

    @JsonProperty("user_id")
    val userID: Long,
)
