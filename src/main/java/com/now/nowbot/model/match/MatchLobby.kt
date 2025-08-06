package com.now.nowbot.model.match

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode

data class MatchLobby(
    @JsonProperty("cursor_string")
    val cursor: String?,
    val matches: List<Match.MatchStat>,
    private val params: JsonNode,
) {
    val limit: Int
        get() = params["limit"].asInt()

    val sort: String
        get() = params["sort"].asText()
}
