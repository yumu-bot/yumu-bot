package com.now.nowbot.model.json

import com.fasterxml.jackson.annotation.JsonProperty

data class MaiRanking (
    @JsonProperty("username")
    var name: String = "",

    @JsonProperty("ra")
    var rating: Int = 0,
)