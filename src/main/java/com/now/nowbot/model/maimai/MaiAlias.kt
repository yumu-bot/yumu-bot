package com.now.nowbot.model.maimai

import com.fasterxml.jackson.annotation.JsonProperty

// 外号
data class MaiAlias(
    @field:JsonProperty("song_id") val songID: Int = 0,
    @field:JsonProperty("aliases") val alias: List<String> = mutableListOf(),
)
