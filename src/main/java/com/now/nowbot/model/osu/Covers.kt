package com.now.nowbot.model.osu

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

@JsonInclude(JsonInclude.Include.NON_NULL) @JsonIgnoreProperties(ignoreUnknown = true)
data class Covers (
    @field:JsonProperty("cover")
    val cover: String = "",

    @field:JsonProperty("cover@2x")
    val cover2x: String = "",

    @field:JsonProperty("card")
    val card: String = "",

    @field:JsonProperty("card@2x")
    val card2x: String = "",

    @field:JsonProperty("list")
    val list: String = "",

    @field:JsonProperty("list@2x")
    val list2x: String = "",

    @field:JsonProperty("slimcover")
    val slimcover: String = "",

    @field:JsonProperty("slimcover@2x")
    val slimcover2x: String = "",
) {
    
    // https://discord.com/channels/188630481301012481/1097318920991559880/1432996516934455378
    //
    //       ╭────() @YumeMuzi: .......
    //    ╭─────╮ peppy
    //    │ ppy │ and this will get you banned.
    //    ╰─────╯

    @get:JsonProperty("fullsize")
    val fullsize: String
        get() = list.replace("@2x", "").replace("list", "fullsize")

    override fun toString(): String {
        return "Covers(cover=$cover, cover2x=$cover2x, card=$card, card2x=$card2x, list=$list, list2x=$list2x, slimcover=$slimcover, slimcover2x=$slimcover2x)"
    }

    companion object {
        enum class CoverType {

            FULL_SIZE, CARD, CARD_2X, COVER, COVER_2X, LIST, LIST_2X, SLIM_COVER, SLIM_COVER_2X;

            companion object {
                fun getCovetType(string: String?): CoverType {
                    return when (string?.replace(" ", "")) {
                        "fullsize", "full", "raw", "r", "f", "background", "b" -> FULL_SIZE
                        "list", "l", "l1" -> LIST
                        "list2", "list2x", "list@2x", "l2" -> LIST_2X
                        "c", "card", "c1" -> CARD
                        "card2", "card2x", "card@2x", "c2" -> CARD_2X
                        "slim", "slimcover", "s", "s1" -> SLIM_COVER
                        "slim2", "slim2x", "slim@2x", "slimcover2", "slimcover2x", "slimcover@2x", "s2" -> SLIM_COVER_2X
                        "2", "cover2", "cover2x", "cover@2x", "o2" -> COVER_2X
                        null -> COVER
                        else -> COVER
                    }
                }

                fun Covers.getString(type: CoverType): String {
                    return when (type) {
                        LIST -> list
                        LIST_2X -> list2x
                        CARD -> card
                        CARD_2X -> card2x
                        SLIM_COVER -> slimcover
                        SLIM_COVER_2X -> slimcover2x
                        COVER_2X -> cover2x
                        COVER -> cover
                        FULL_SIZE -> list.replace("@2x", "").replace("list", "fullsize")
                    }
                }
            }
        }

        fun getCoverFromCacheID(beatmapsetID: Long, cacheID: Long? = null): Covers {

            val prefix = "https://assets.ppy.sh/beatmaps/${beatmapsetID}/covers/"

            val suffix = if (cacheID != null) {
                ".jpg?${cacheID}"
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
