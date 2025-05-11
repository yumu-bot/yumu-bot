package com.now.nowbot.model.json

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import com.now.nowbot.model.enums.OsuMode
import jakarta.persistence.Column
import java.time.OffsetDateTime
import java.util.stream.StreamSupport

open class BeatMap {
    @JsonProperty("beatmapset_id")
    var beatMapSetID: Long = 0

    @JsonProperty("difficulty_rating")
    var starRating: Double = 0.0

    @JsonProperty("id")
    @Column(name = "id")
    var beatMapID: Long = 0

    @set:JsonProperty("mode")
    @get:JsonIgnore
    var modeStr = "osu"

    @set:JsonIgnore
    @get:JsonProperty("mode")
    var mode: OsuMode
        get() {
            return OsuMode.getMode(modeStr)
        }
        set(value) {
            modeStr = value.shortName
        }

    @JsonProperty("status")
    var status: String = "graveyard"

    @JsonProperty("total_length")
    var totalLength: Int = 0

    @JsonProperty("user_id")
    var mapperID: Long = 0

    @JsonProperty("version")
    var difficultyName: String = ""

    @JsonProperty("beatmapset")
    var beatMapSet: BeatMapSet? = null

    @JsonProperty("checksum")
    var md5: String? = ""
        get() {
            return field ?: ""
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

    //retry == fail, fail == exit
    @JsonProperty("failtimes")
    val failTimes: JsonNode? = null

    @JsonProperty("max_combo")
    var maxCombo: Int? = 0

    @set:JsonProperty("top_tag_ids")
    @get:JsonIgnoreProperties
    var tagIDs: List<TagData>? = null

    data class TagData(
        @JsonProperty("tag_id") val id: Int,
        @JsonProperty("count") val count: Int,
    )

    // 自己设
    @set:JsonIgnoreProperties
    @get:JsonProperty("tags")
    var tags: List<Tag>? = null

    // Extend!

    @JsonProperty("accuracy")
    var OD: Float? = null

    @JsonProperty("ar")
    var AR: Float? = null

    @JsonProperty("bpm")
    var BPM: Float? = null

    @JsonProperty("convert")
    var convert: Boolean? = null

    @JsonProperty("count_circles")
    var circles: Int? = null

    @JsonProperty("count_sliders")
    var sliders: Int? = null

    @JsonProperty("count_spinners")
    var spinners: Int? = null

    @JsonProperty("cs")
    var CS: Float? = null

    @JsonProperty("deleted_at")
    var deletedAt: OffsetDateTime? = null

    @JsonProperty("drain")
    var HP: Float? = null

    @JsonProperty("hit_length")
    var hitLength: Int? = null

    @JsonProperty("is_scoreable")
    var scoreAble: Boolean? = null

    @JsonProperty("last_updated")
    var lastUpdated: String? = null

    /**
     * 只有 beatmapSet 内的 beatmap 才有的属性
     */
    @JsonProperty("owners")
    var owners: List<MicroUser>? = null

    @JsonProperty("mode_int")
    var modeInt: Int? = null

    @JsonProperty("passcount")
    var passCount: Int? = null

    @JsonProperty("playcount")
    var playCount: Int = 0

    @JsonProperty("ranked")
    var ranked: Int = 0

    @JsonProperty("url")
    var url: String? = null

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
            return when (status.trim()) {
                "ranked", "qualified", "loved", "approved" -> true
                else -> when (ranked) {
                    1, 2, 3, 4 -> true
                    else -> false
                }
            }
        }

    //自己取
    @get:JsonProperty("preview_name")
    val previewName: String
        get() {
            return if (beatMapSet != null) {
                beatMapSet!!.artist + " - " + beatMapSet!!.title + " (" + beatMapSet!!.creator + ") [" + difficultyName + "]"
            } else {
                difficultyName
            }
        }


    companion object {
        @JvmStatic
        private fun getList(data: JsonNode?, fieldName: String): List<Int> {
            if (data == null) return listOf(0)

            return if (data.hasNonNull(fieldName) && data[fieldName].isArray) {
                StreamSupport.stream(data[fieldName].spliterator(), false)
                    .map { n: JsonNode -> n.asInt(0) }
                    .toList()
            } else listOf()
        }
    }
}
