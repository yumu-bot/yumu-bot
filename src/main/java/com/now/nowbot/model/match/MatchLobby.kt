package com.now.nowbot.model.match

import com.fasterxml.jackson.annotation.JsonProperty
import tools.jackson.databind.JsonNode
import tools.jackson.databind.PropertyNamingStrategies
import tools.jackson.databind.annotation.JsonNaming

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class MatchLobby(
    @field:JsonProperty("cursor_string")
    val cursor: String?,
    val matches: List<Match.MatchStat>,
    private val params: JsonNode,
) {
    val limit: Int
        get() = params["limit"].asInt()

    val sort: String
        get() = params["sort"].asString()
}
