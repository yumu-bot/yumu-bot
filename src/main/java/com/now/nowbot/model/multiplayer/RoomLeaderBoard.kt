package com.now.nowbot.model.multiplayer

import com.fasterxml.jackson.annotation.JsonProperty
import tools.jackson.databind.PropertyNamingStrategies
import tools.jackson.databind.annotation.JsonNaming

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class RoomLeaderBoard(
    @field:JsonProperty("leaderboard")
    val leaderboard: List<RoomScore>,

    @field:JsonProperty("user_score")
    val userScore: Long?,
)
