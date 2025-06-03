package com.now.nowbot.model.osu

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.OffsetDateTime

@JsonIgnoreProperties(ignoreUnknown = true) @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
class ActivityEvent {
    data class Achievement(
        @JsonProperty("icon_url") val url: String,
        @JsonProperty("id") val achievementID: Int,
        val name: String,
        val grouping: String,
        val ordering: String,
        val slug: String,
        val description: String,
        val mode: String?,
        val instructions: String
    )

    data class EventBeatMap(
        val title: String,
        @JsonProperty("url")
        val url: String?
    ) {
        @get:JsonProperty("id")
        val id: Long
            get() = url?.replace("/b/", "")?.toLongOrNull() ?: 0L
    }

    data class EventBeatMapSet(
        val title: String,

        @JsonProperty("url")
        val url: String?
    ) {
        @get:JsonProperty("id")
        val id: Long
            get() = url?.replace("/s/", "")?.toLongOrNull() ?: 0L
    }

    data class EventUser(
        @JsonProperty("username")
        val name: String,

        @JsonProperty("url")
        val url: String?,
        @JsonProperty("previous_username")
        val previousUsername: String?
    ) {
        @get:JsonProperty("id")
        val id: Long
            get() = url?.replace("/u/", "")?.toLongOrNull() ?: 0L
    }

    enum class EventType(vararg f: String) {
        Achievement("achievement", "user"),
        BeatmapPlaycount("beatmap", "count"),
        BeatmapsetApprove("approval", "beatmapset", "user"),
        BeatmapsetDelete("beatmapset"),
        BeatmapsetRevive("beatmapset", "user"),
        BeatmapsetUpdate("beatmapset", "user"),
        BeatmapsetUpload("beatmapset", "user"),

        Rank("scoreRank", "rank", "mode", "beatmap", "user"),
        RankLost("mode", "beatmap", "user"),
        UserSupportAgain("user"),
        UserSupportFirst("user"),
        UserSupportGift("user"),
        UsernameChange("user"),
        ;

        val field: Array<String> = f.toList().toTypedArray()

        companion object {
            fun getEventType(string: String?): EventType? {
                if (string.isNullOrBlank()) return null

                EventType.entries.forEach {
                    if (it.name.equals(string.trim(), true)) {
                        return it
                    }
                }

                return null
            }
        }
    }

    //@JsonFormat(locale = "zh", timezone = "GMT+8", pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ")
    @JsonProperty("created_at")
    var createdAt: OffsetDateTime? = null

    @JsonProperty("id")
    var eventID: Long = 0

    @JsonIgnore
    var type: EventType? = null

    @JsonProperty("type") fun setType(type: String?) {
        this.type = EventType.getEventType(type)
    }

    /*****************************下面属性根据不同 type 进行变动, 只包含 enum 中括号内的属性 */
    @JsonProperty("count")
    var count: Int? = null

    @JsonProperty("approval")
    var approval: String? = null

    @JsonProperty("scoreRank")
    var scoreRank: String? = null

    @JsonProperty("rank")
    var rank: Int? = null

    @JsonProperty("mode")
    var mode: String? = null

    @JsonProperty("achievement")
    var achievement: Achievement? = null

    @JsonProperty("user")
    var user: EventUser? = null

    @JsonProperty("beatmap")
    var beatmap: EventBeatMap? = null

    @JsonProperty("beatmapset")
    var beatmapSet: EventBeatMapSet? = null

    val isMapping: Boolean
        get() = type == EventType.BeatmapsetApprove
                || type == EventType.BeatmapsetDelete
                || type == EventType.BeatmapsetRevive
                || type == EventType.BeatmapsetUpdate
                || type == EventType.BeatmapsetUpload



    override fun equals(other: Any?): Boolean {
        if (other is ActivityEvent) {
            return this.type == other.type && (beatmapSet?.url == other.beatmapSet?.url)
        }
        return false
    }

    override fun hashCode(): Int {
        return (this.type!!.name + (beatmapSet?.url ?: "")).hashCode()
    }

    override fun toString(): String {
        return "ActivityEvent(createdAt=$createdAt, eventID=$eventID, type=$type, count=$count, approval=$approval, scoreRank=$scoreRank, rank=$rank, mode=$mode, achievement=$achievement, user=$user, beatmap=$beatmap, beatmapSet=$beatmapSet, isMapping=$isMapping)"
    }
}
