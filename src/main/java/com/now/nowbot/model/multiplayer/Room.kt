package com.now.nowbot.model.multiplayer

import RoomInfo
import com.fasterxml.jackson.annotation.JsonProperty
import com.now.nowbot.model.osu.Beatmap
import com.now.nowbot.model.osu.Beatmapset
import com.now.nowbot.model.osu.MicroUser
import java.time.OffsetDateTime

data class Room(
    @JsonProperty("beatmaps")
    val beatmaps: List<Beatmap>,

    @JsonProperty("beatmapsets")
    val beatmapsets: List<Beatmapset>,

    @JsonProperty("current_playlist_item_id")
    val currentEventID: Long?,

    @JsonProperty("events")
    val events: List<RoomEvent>,

    @JsonProperty("first_event_id")
    val firstEventID: Long,

    @JsonProperty("latest_event_id")
    val latestEventID: Long,

    @JsonProperty("playlist_items")
    val items: List<Playlist>,

    @JsonProperty("room")
    val roomInfo: RoomInfo,

    @JsonProperty("users")
    val users: List<MicroUser>

    ) {

    data class RoomEvent(
        @JsonProperty("id")
        val eventID: Long,

        @JsonProperty("created_at")
        val createdTime: OffsetDateTime,

        @JsonProperty("event_type")
        val eventType: String,

        @JsonProperty("playlist_item_id")
        val itemID: Long?,

        @JsonProperty("user_id")
        val userID: Long?,
    ) {
        val type: EventType
            get() = EventType.getType(eventType)
    }

    enum class EventType(val value: String) {
        PlayerJoined("player_joined"),
        PlayerKicked("player_kicked"),
        PlayerLeft("player_left"),
        HostChanged("host_changed"),
        GameStarted("game_started"),
        GameCompleted("game_completed"),
        GameAborted("game_aborted"),

        Other("other"),

        ;

        companion object {
            fun getType(value: String) = when (value) {
                PlayerJoined.value -> PlayerJoined
                PlayerKicked.value -> PlayerKicked
                PlayerLeft.value -> PlayerLeft
                HostChanged.value -> HostChanged
                GameStarted.value -> GameStarted
                GameCompleted.value -> GameCompleted
                GameAborted.value -> GameAborted
                else -> Other
            }
        }
    }

    /**
     * 合并多个 Room
     */
    fun add(another: Room): Room {
        val earlier: Room
        val later: Room

        if (this.currentEventID != null) {
            if (another.currentEventID != null) {
                val sort = listOf(this, another)
                    .sortedBy { it.currentEventID }

                earlier = sort.first()
                later = sort.last()
            } else {
                earlier = this
                later = another
            }
        } else {
            if (another.currentEventID != null) {
                earlier = another
                later = this
            } else {
                val sort = listOf(this, another)
                    .sortedBy { it.users.size }

                earlier = sort.first()
                later = sort.last()
            }
        }

        return Room(
            beatmaps = (earlier.beatmaps + later.beatmaps).distinctBy { it.beatmapID },

            beatmapsets = (earlier.beatmapsets + later.beatmapsets).distinctBy { it.beatmapsetID },

            currentEventID = later.currentEventID,
            events = (earlier.events + later.events).distinctBy { it.eventID },
            firstEventID = earlier.firstEventID,
            latestEventID = later.latestEventID,
            items = (earlier.items + later.items).distinctBy { it.listID },
            roomInfo = later.roomInfo,
            users = (earlier.users + later.users).distinctBy { it.userID }
        )
    }
}
