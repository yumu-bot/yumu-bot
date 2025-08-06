import com.fasterxml.jackson.annotation.JsonProperty
import com.now.nowbot.model.multiplayer.Playlist
import com.now.nowbot.model.osu.MicroUser

data class RoomScore(
    @JsonProperty("accuracy")
    val accuracy: Double,

    @JsonProperty("attempts")
    val attempts: Int,

    @JsonProperty("completed")
    val completed: Int,

    @JsonProperty("pp")
    val pp: Double,

    @JsonProperty("room_id")
    val roomID: Long,

    @JsonProperty("total_score")
    val totalScore: Long,

    @JsonProperty("user_id")
    val userID: Long,

    // 只有在获取 room/xxx 的时候有
    @JsonProperty("playlist_item_attempts")
    val itemAttempts: List<Playlist>?,

    // 只有在获取 room/xxx/leaderboard 的时候有
    @JsonProperty("user")
    val user: MicroUser?,

    )