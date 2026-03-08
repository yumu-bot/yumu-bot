package com.now.nowbot.model.maimai

import com.fasterxml.jackson.annotation.JsonProperty

data class MaiRanking (
    @field:JsonProperty("username")
    var name: String = "",

    @field:JsonProperty("ra")
    var rating: Int = 0,
)