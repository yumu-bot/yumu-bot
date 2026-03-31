package com.now.nowbot.model.cosu

import com.fasterxml.jackson.annotation.JsonProperty

data class CosuResponse(
    @field:JsonProperty("difficulty")
    val difficulty: CosuDifficulty,

    @field:JsonProperty("performance")
    val performance: CosuPerformance? = null,
)
