package com.now.nowbot.model.osu

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSetter
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.enums.OsuMode.CATCH
import com.now.nowbot.model.enums.OsuMode.CATCH_RELAX
import jakarta.persistence.Column
import tools.jackson.databind.PropertyNamingStrategies
import tools.jackson.databind.annotation.JsonNaming
import java.time.OffsetDateTime

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class Beatmap(
    @field:JsonProperty("beatmapset_id")
    var beatmapsetID: Long = 0L,

    @field:JsonProperty("difficulty_rating")
    var starRating: Double = 0.0,

    @field:JsonProperty("id")
    @Column(name = "id")
    var beatmapID: Long = 0L,

    @field:JsonProperty("lazer_only")
    var lazerOnly: Boolean = false,

    @set:JsonProperty("mode")
    @get:JsonIgnore
    var modeStr: String = "osu",

    @field:JsonProperty("status")
    var status: String = "graveyard",

    @field:JsonProperty("total_length")
    var totalLength: Int = 0,

    @field:JsonProperty("user_id")
    /**
     * 这个只能记录第一个谱师，所以尽量不要用这个参数，而是使用 mapperIDs
     */
    var mapperID: Long = 0,

    @field:JsonProperty("version")
    var difficultyName: String = "",

    @field:JsonProperty("beatmapset")
    var beatmapset: Beatmapset? = null,

    @field:JsonProperty("checksum")
    var md5: String? = null,

    // @field:JsonProperty("failtimes")
    // var failTimes: JsonNode? = null,

    //retry == fail, fail == exit
    @set:JsonProperty("failtimes")
    @get:JsonIgnore
    var failTimes: FailTimesData? = null,

    @field:JsonProperty("max_combo")
    var maxCombo: Int? = 0,

    @set:JsonProperty("top_tag_ids")
    @get:JsonIgnore
    var tagIDs: List<TagData>? = null,

    // 自己设
    @set:JsonIgnore
    @get:JsonProperty("tags")
    var tags: List<Tag>? = null,

    // Extend!
    @field:JsonProperty("od")
    @field:JsonAlias("accuracy")
    var od: Float? = null,

    @field:JsonProperty("ar")
    var ar: Float? = null,

    @field:JsonProperty("bpm")
    var bpm: Float = 0f,

    @field:JsonProperty("convert")
    var convert: Boolean? = null,

    @field:JsonProperty("count_circles")
    var circles: Int? = null,

    @field:JsonProperty("count_sliders")
    var sliders: Int? = null,

    @field:JsonProperty("count_spinners")
    var spinners: Int? = null,

    @field:JsonProperty("cs")
    var cs: Float? = null,

    @field:JsonProperty("deleted_at")
    var deletedAt: OffsetDateTime? = null,

    @field:JsonProperty("hp")
    @field:JsonAlias("drain")
    var hp: Float? = null,

    @field:JsonProperty("hit_length")
    var hitLength: Int? = null,

    @field:JsonProperty("is_scoreable")
    var scoreAble: Boolean? = null,

    @field:JsonProperty("last_updated")
    var lastUpdated: String? = null,

    /**
     * 只有 id 和 username
     */
    @field:JsonProperty("owners")
    var owners: List<NanoUser>? = null,

    @field:JsonProperty("mode_int")
    var modeInt: Int? = null,

    @field:JsonProperty("passcount")
    var passCount: Int? = null,

    @field:JsonProperty("playcount")
    var playCount: Int = 0,

    @field:JsonProperty("ranked")
    var ranked: Int = 0,

    @field:JsonProperty("url")
    var url: String? = null,

    // 如果是走用户 token，还可以看到玩家玩了这张图几次
    @field:JsonProperty("current_user_playcount")
    var currentPlayCount: Int? = null
) {

    @set:JsonIgnore
    @get:JsonProperty("mode")
    var mode: OsuMode
    get() {
        return OsuMode.getMode(modeStr)
    }
    set(value) {
        modeStr = value.shortName
    }

    data class FailTimesData(
        @param:JsonSetter("fail")
        @get:JsonIgnore
        val retries: IntArray = intArrayOf(),

        @param:JsonSetter("exit")
        @get:JsonIgnore
        val fails: IntArray = intArrayOf(),
    ) {

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as FailTimesData
            if (!retries.contentEquals(other.retries)) return false
            if (!fails.contentEquals(other.fails)) return false
            return true
        }

        override fun hashCode(): Int {
            var result = fails.contentHashCode()
            result = 31 * result + retries.contentHashCode()
            return result
        }
    }

    @get:JsonProperty("retries")
    val retries: IntArray
        get() = failTimes?.retries ?: intArrayOf()

    @get:JsonProperty("fails")
    val fails: IntArray
        get() = failTimes?.fails ?: intArrayOf()

    //自己算
    @get:JsonProperty("retry")
    val retry: Int
        get() = failTimes?.retries?.sum() ?: 0

    //自己算
    @get:JsonProperty("fail")
    val fail: Int
        get() = failTimes?.fails?.sum() ?: 0

    //自己算
    @get:JsonProperty("has_leader_board")
    val hasLeaderBoard: Boolean
        get() {
            return when (status.firstOrNull()) {
                'r', 'q', 'l', 'a' -> true
                else -> when (ranked) {
                    1, 2, 3, 4 -> true
                    else -> false
                }
            }
        }

    //自己取
    @get:JsonProperty("preview_name")
    val previewName: String
        get() = if (beatmapset != null) {
            beatmapset!!.artist + " - " + beatmapset!!.title + " (" + beatmapset!!.creator + ") [" + difficultyName + "]"
        } else {
            difficultyName
        }

    //自己取
    @field:JsonProperty("user")
    var user: OsuUser? = null

    //自己算
    @get:JsonProperty("total_notes")
    val totalNotes = when(this.mode) {
        CATCH, CATCH_RELAX -> (circles ?: 0) + (sliders ?: 0)
        else -> (circles ?: 0) + (sliders ?: 0) + (spinners ?: 0)
    }

    data class TagData(
        @field:JsonProperty("tag_id") val id: Int,
        @field:JsonProperty("count") val count: Int,
    )

    @get:JsonProperty("mapper_ids")
    val mapperIDs: List<Long>
        get() = owners?.map { it.userID } ?: listOf(mapperID)

    @get:JsonIgnore
    val mappers: List<NanoUser>
        get() = owners ?: listOf(NanoUser(mapperID, user?.username ?: "UID:${mapperID}"))


    //自己保存一份
    data class BeatmapDetails(
        val ar: Float = 0f,
        val cs: Float = 0f,
        val od: Float = 0f,
        val hp: Float = 0f,
        val bpm: Float = 0f,

        @field:JsonProperty("drain")
        val hitLength: Int = 0,

        @field:JsonProperty("total")
        val totalLength: Int = 0,
    ) {
        fun toMap(): Map<String, Any> {
            return mapOf(
                "cs" to cs,
                "ar" to ar,
                "od" to od,
                "hp" to hp,
                "bpm" to bpm,
                "drain" to hitLength,
                "total" to totalLength,
            )
        }
    }

    @delegate:JsonIgnore
    val originalDetails: BeatmapDetails by lazy {
        BeatmapDetails(
            ar ?: 0f, cs ?: 0f, od ?: 0f, hp ?: 0f, bpm,
            hitLength ?: 0, totalLength
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Beatmap

        return beatmapID == other.beatmapID && mode == other.mode
    }

    override fun hashCode(): Int {
        return 31 * beatmapID.hashCode() + mode.hashCode()
    }
}
