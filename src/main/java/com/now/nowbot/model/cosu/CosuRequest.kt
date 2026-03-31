package com.now.nowbot.model.cosu

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

@JsonInclude(JsonInclude.Include.NON_NULL)
data class CosuRequest(
    @field:JsonProperty("file")
    var path: String = "",
    @field:JsonProperty("mode")
    var mode: String = "osu",

    @field:JsonProperty("score")
    var score: CosuScore? = null,
)