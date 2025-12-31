package com.now.nowbot.model.multiplayer

import com.fasterxml.jackson.annotation.JsonProperty

data class RoomLeaderBoard(
    @field:JsonProperty("leaderboard")
    val leaderboard: List<RoomScore>,

    @field:JsonProperty("user_score")
    val userScore: Long?,
)
