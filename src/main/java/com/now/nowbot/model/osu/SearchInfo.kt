package com.now.nowbot.model.osu

import com.fasterxml.jackson.annotation.JsonProperty

class SearchInfo {
    @JsonProperty("c") 
    var general: String? = null

    @JsonProperty("sort") 
    var sort: String? = null

    @JsonProperty("s") 
    var status: String? = null

    @JsonProperty("nsfw") 
    var nsfw: Boolean? = null

    @JsonProperty("g") 
    var genre: Byte = 0

    @JsonProperty("l") 
    var language: Byte = 0

    @JsonProperty("e") 
    var others: String? = null

    @JsonProperty("r") 
    var rank: String? = null

    @JsonProperty("played") 
    var played: String? = null
}
