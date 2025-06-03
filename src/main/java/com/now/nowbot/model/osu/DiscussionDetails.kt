package com.now.nowbot.model.osu

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.lang.Nullable
import java.time.OffsetDateTime

@JsonInclude(JsonInclude.Include.NON_NULL) @JsonIgnoreProperties(ignoreUnknown = true)
class DiscussionDetails {
    @JsonProperty("id")
    var discussionID: Long = 0

    @JsonProperty("beatmapset_id")
    var beatmapsetID: Long = 0

    @JsonProperty("beatmap_id")
    var beatmapID: Long? = null

    @JsonProperty("user_id")
    var userID: Long = 0

    @JsonProperty("deleted_by_id")
    var deletedBy: Long? = null

    /**
     * hype, mapper_note, problem, suggestion, praise, review. Blank defaults to all types
     */
    @JsonProperty("message_type")
    var messageType: String = ""

    @JsonProperty("parent_id")
    var parentDiscussionID: Long? = 0

    @JsonProperty("time_stamp")
    var timeStamp: Long = 0

    @JvmField @JsonProperty("resolved")
    var resolved: Boolean = false

    @JvmField @JsonProperty("can_be_resolved")
    var canBeResolved: Boolean = true

    @JsonProperty("can_grant_kudosu")
    var canGrantKudosu: Boolean = true

    @JsonProperty("created_at")
    var createdAt: OffsetDateTime = OffsetDateTime.now()

    @JsonProperty("updated_at")
    var updatedAt: OffsetDateTime = OffsetDateTime.now()

    @JsonProperty("deleted_at")
    var deletedAt: OffsetDateTime? = null

    @JsonProperty("last_post_at")
    var lastPostAt: OffsetDateTime = OffsetDateTime.now()

    @JsonProperty("kudosu_denied")
    var kudosuDenied: Boolean = false

    @JsonProperty("starting_post")
    var post: BeatmapsetDiscussionPost? = null

    //自己算
    @JvmField @Nullable
    var difficulty: String? = null

    override fun equals(other: Any?): Boolean {
        return if (other is DiscussionDetails) {
            other.discussionID == this.discussionID
        } else {
            false
        }
    }

    override fun hashCode(): Int {
        return discussionID.hashCode()
    }
}
