package com.now.nowbot.model.json

import com.fasterxml.jackson.annotation.JsonProperty

class ChuAlias(
    @JsonProperty("song_id") val songID: Int = 0,
    @JsonProperty("aliases") val alias: List<String> = mutableListOf(),
)