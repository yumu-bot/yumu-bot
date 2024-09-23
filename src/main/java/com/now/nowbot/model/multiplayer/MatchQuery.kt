package com.now.nowbot.model.multiplayer

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode

data class MatchQuery(
    @JsonProperty("cursor_string")
    val cursor: String?,
    val matches: List<MonitoredMatch.MatchStat>,
    private val params: JsonNode,
) {
    val limit: Int
        get() = params["limit"].asInt()

    val sort: String
        get() = params["sort"].asText()
}
