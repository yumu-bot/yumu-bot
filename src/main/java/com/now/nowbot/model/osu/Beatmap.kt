package com.now.nowbot.model.osu

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import com.now.nowbot.model.enums.OsuMode
import jakarta.persistence.Column
import java.time.OffsetDateTime

data class Beatmap(
    @field:JsonProperty("beatmapset_id")
    var beatmapsetID: Long = 0L,

    @field:JsonProperty("difficulty_rating")
    var starRating: Double = 0.0,

    @field:JsonProperty("id")
    @Column(name = "id")
    var beatmapID: Long = 0L,

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

    //retry == fail, fail == exit
    @field:JsonProperty("failtimes")
    var failTimes: JsonNode? = null,

    @field:JsonProperty("max_combo")
    var maxCombo: Int? = 0,

    @set:JsonProperty("top_tag_ids")
    @get:JsonIgnoreProperties
    var tagIDs: List<TagData>? = null,

    // 自己设
    @set:JsonIgnoreProperties
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
    var owners: List<MicroUser>? = null,

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

    @get:JsonProperty("retries")
    val retries: List<Int>
    get() {
        return getList(failTimes, "fail")
    }

    @get:JsonProperty("fails")
    val fails: List<Int>
    get() {
        return getList(failTimes, "exit")
    }

    //自己算
    @get:JsonProperty("retry")
    val retry: Int
        get() {
            return retries.sum()
        }

    //自己算
    @get:JsonProperty("fail")
    val fail: Int
        get() {
            return fails.sum()
        }

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
    val totalNotes = (circles ?: 0) + (sliders ?: 0) + (spinners ?: 0)

    data class TagData(
        @field:JsonProperty("tag_id") val id: Int,
        @field:JsonProperty("count") val count: Int,
    )

    @get:JsonProperty("mapper_ids")
    val mapperIDs: List<Long>
        get() = owners?.map { it.userID } ?: listOf(mapperID)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Beatmap

        return beatmapID == other.beatmapID
    }

    override fun hashCode(): Int {
        return beatmapID.hashCode()
    }

    companion object {
        @JvmStatic
        private fun getList(data: JsonNode?, fieldName: String): List<Int> {
            if (data == null) return listOf(0)

            return if (data.hasNonNull(fieldName) && data[fieldName].isArray) {
                data[fieldName].map { it.asInt(0) }

                /*
                StreamSupport.stream(data[fieldName].spliterator(), false)
                    .map { n: JsonNode -> n.asInt(0) }
                    .toList()

                 */
            } else listOf()
        }
    }
}
