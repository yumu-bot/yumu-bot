package com.now.nowbot.model.json

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

@JsonInclude(JsonInclude.Include.NON_NULL) @JsonIgnoreProperties(ignoreUnknown = true)
class Covers {
    @JvmField
    @JsonProperty("cover")
    var cover: String? = ""

    @JvmField
    @JsonProperty("cover@2x")
    var cover2x: String? = ""

    @JvmField
    @JsonProperty("card")
    var card: String? = ""

    @JvmField
    @JsonProperty("card@2x")
    var card2x: String? = ""

    @JvmField
    @JsonProperty("list")
    var list: String? = ""

    @JvmField
    @JsonProperty("list@2x")
    var list2x: String? = ""

    @JvmField
    @JsonProperty("slimcover")
    var slimcover: String? = ""

    @JvmField
    @JsonProperty("slimcover@2x")
    var slimcover2x: String? = ""
    
    override fun toString(): String {
        return "Covers(cover=$cover, cover2x=$cover2x, card=$card, card2x=$card2x, list=$list, list2x=$list2x, slimcover=$slimcover, slimcover2x=$slimcover2x)"
    }
}
