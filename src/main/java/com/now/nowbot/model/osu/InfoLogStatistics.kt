package com.now.nowbot.model.osu

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime

class InfoLogStatistics : Statistics() {
    @JsonProperty("time")
    var logTime: LocalDateTime = LocalDateTime.now()
}
