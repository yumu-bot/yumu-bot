package com.now.nowbot.model.osu

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

@JsonInclude(JsonInclude.Include.NON_NULL) @JsonIgnoreProperties(ignoreUnknown = true)
data class Covers (
    @get:JsonProperty("cover")
    val cover: String = "",

    @get:JsonProperty("cover@2x")
    val cover2x: String = "",

    @get:JsonProperty("card")
    val card: String = "",

    @get:JsonProperty("card@2x")
    val card2x: String = "",

    @get:JsonProperty("list")
    val list: String = "",

    @get:JsonProperty("list@2x")
    val list2x: String = "",

    @get:JsonProperty("slimcover")
    val slimcover: String = "",

    @get:JsonProperty("slimcover@2x")
    val slimcover2x: String = "",
) {
    override fun toString(): String {
        return "Covers(cover=$cover, cover2x=$cover2x, card=$card, card2x=$card2x, list=$list, list2x=$list2x, slimcover=$slimcover, slimcover2x=$slimcover2x)"
    }

    companion object {
        fun getCover(beatmapsetID: Long, coverID: Long?): Covers {

            val prefix = "https://assets.ppy.sh/beatmaps/${beatmapsetID}/covers/"

            val suffix = if (coverID != null) {
                ".jpg?$coverID"
            } else {
                ".jpg"
            }

            return Covers(
                "${prefix}cover${suffix}",
                "${prefix}cover@2x${suffix}",
                "${prefix}card${suffix}",
                "${prefix}card@2x${suffix}",
                "${prefix}list${suffix}",
                "${prefix}list@2x${suffix}",
                "${prefix}silmcover${suffix}",
                "${prefix}silmcover@2x${suffix}",
            )
        }
    }
}
