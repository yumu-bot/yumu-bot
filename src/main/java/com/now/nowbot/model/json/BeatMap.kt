package com.now.nowbot.model.json

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import com.now.nowbot.model.enums.OsuMode
import jakarta.persistence.Column
import org.springframework.lang.NonNull
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
            modeStr = value.name.lowercase()
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
    var md5: String = ""

    @get:JsonProperty("retryList")
    val retryList: List<Int>
        get() {
            return getList(failTimes, "fail")
        }

    @get:JsonProperty("failList")
    val failList: List<Int>
        get() {
            return getList(failTimes, "exit")
        }

    //retry == fail, fail == exit
    @JsonProperty("failtimes")
    val failTimes: JsonNode? = null

    @JsonProperty("max_combo")
    var maxCombo: Int? = 0

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

    @JsonProperty("mode_int")
    var modeInt: Int? = null

    @JsonProperty("passcount")
    var passCount: Int? = null

    @JsonProperty("playcount")
    var playCount: Int? = null

    @JsonProperty("ranked")
    var ranked: Int? = null

    @JsonProperty("url")
    var url: String? = null

    //自己算
    @get:JsonProperty("retry")
    val retry: Int
        get() {
            return retryList.sum()
        }

    //自己算
    @get:JsonProperty("fail")
    val fail: Int
        get() {
            return failList.sum()
        }

    //自己算
    @get:JsonProperty("has_leader_board")
    val hasLeaderBoard: Boolean
        get(){
            return if (status.isNotBlank()) {
                status == "ranked" || status == "qualified" || status == "loved" || status == "approved"
            } else {
                when (ranked) {
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

            var list: List<Int> = listOf()
            if (data.hasNonNull(fieldName) && data[fieldName].isArray) {
                list = StreamSupport.stream(data[fieldName].spliterator(), false)
                    .map { n: JsonNode -> n.asInt(0) }
                    .toList()
            }
            return list
        }

        @JvmStatic
        @NonNull
        fun extend(lite: BeatMap?, extended: BeatMap?): BeatMap? {
            var liteMap = lite

            if (extended == null) {
                return liteMap
            } else if (liteMap?.CS == null) {
                liteMap = extended
                return liteMap
            }

            extended.starRating = liteMap.starRating
            extended.CS = liteMap.CS
            extended.AR = liteMap.AR
            extended.OD = liteMap.OD
            extended.HP = liteMap.HP
            extended.totalLength = liteMap.totalLength
            extended.hitLength = liteMap.hitLength
            extended.BPM = liteMap.BPM

            liteMap = extended
            return liteMap
        }
    }
}
