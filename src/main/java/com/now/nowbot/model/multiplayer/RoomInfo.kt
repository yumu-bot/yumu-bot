import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.now.nowbot.model.multiplayer.Playlist
import com.now.nowbot.model.osu.MicroUser
import java.time.OffsetDateTime

@JsonIgnoreProperties(ignoreUnknown = true)
data class RoomInfo(
    @JsonProperty("id")
    val roomID: Long,

    @JsonProperty("name")
    val name: String,

    @JsonProperty("category")
    val category: String, // normal，看起来可以放月赛或每日挑战之类的东西

    @JsonProperty("status")
    val status: String, // playing

    @JsonProperty("type")
    val type: String, // head_to_head

    @JsonProperty("user_id")
    val userID: Long,

    @JsonProperty("starts_at")
    val startedTime: OffsetDateTime,

    @JsonProperty("ends_at")
    val endedTime: OffsetDateTime?,

    @JsonProperty("max_attempts")
    val maxAttempts: Int?, // 应该是无上限

    @JsonProperty("participant_count")
    val participantCount: Int,

    @JsonProperty("channel_id")
    val channelID: Long,

    @JsonProperty("active")
    val active: Boolean,

    @JsonProperty("has_password")
    val hasPassword: Boolean,

    @JsonProperty("queue_mode")
    val queueMode: String, // all_players_round_robin

    @JsonProperty("auto_skip")
    val autoSkip: Boolean,

    // 只有在获取 room/xxx 的时候有
    @JsonProperty("current_user_score")
    val currentUserScore: RoomScore?,

    // 只有在获取 room/xxx 的时候有
    @JsonProperty("host")
    val host: MicroUser?,

    // 只有在获取 room/xxx 的时候有
    @JsonProperty("playlist")
    val playlist: List<Playlist>?,

    // 只有在获取 room/xxx 的时候有
    @JsonProperty("recent_participants")
    val recentParticipants: List<MicroUser>?
    )