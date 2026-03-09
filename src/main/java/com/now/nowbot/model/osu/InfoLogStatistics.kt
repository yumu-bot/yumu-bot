package com.now.nowbot.model.osu

import com.fasterxml.jackson.annotation.JsonProperty
import tools.jackson.databind.PropertyNamingStrategies
import tools.jackson.databind.annotation.JsonNaming
import java.time.LocalDateTime

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
class InfoLogStatistics : Statistics() {
    @JsonProperty("time")
    var logTime: LocalDateTime = LocalDateTime.now()
}
