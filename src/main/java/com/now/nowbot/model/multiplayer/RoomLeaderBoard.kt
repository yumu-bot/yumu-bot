package com.now.nowbot.model.multiplayer

import RoomScore
import com.fasterxml.jackson.annotation.JsonProperty

data class RoomLeaderBoard(
    @JsonProperty("leaderboard")
    val leaderboard: List<RoomScore>,

    @JsonProperty("user_score")
    val userScore: Long?,
)
