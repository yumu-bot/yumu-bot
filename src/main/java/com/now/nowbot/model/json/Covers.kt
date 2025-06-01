package com.now.nowbot.model.json

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

@JsonInclude(JsonInclude.Include.NON_NULL) @JsonIgnoreProperties(ignoreUnknown = true)
data class Covers (
    @JsonProperty("cover")
    val cover: String = "",

    @JsonProperty("cover@2x")
    val cover2x: String = "",

    @JsonProperty("card")
    val card: String = "",

    @JsonProperty("card@2x")
    val card2x: String = "",

    @JsonProperty("list")
    val list: String = "",

    @JsonProperty("list@2x")
    val list2x: String = "",

    @JsonProperty("slimcover")
    val slimcover: String = "",

    @JsonProperty("slimcover@2x")
    val slimcover2x: String = "",
) {
    override fun toString(): String {
        return "Covers(cover=$cover, cover2x=$cover2x, card=$card, card2x=$card2x, list=$list, list2x=$list2x, slimcover=$slimcover, slimcover2x=$slimcover2x)"
    }
}
