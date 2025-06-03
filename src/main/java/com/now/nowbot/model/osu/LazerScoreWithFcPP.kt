package com.now.nowbot.model.osu

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.beans.BeanUtils

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
class LazerScoreWithFcPP : LazerScore() {
    @JsonProperty("fc_pp")
    var fcPP: Double = 0.0

    @JsonProperty("index")
    var index: Int = 0

    @JsonProperty("index_after")
    var indexAfter: Int = 0

    companion object {
        fun copyOf(score: LazerScore): LazerScoreWithFcPP {
            val result = LazerScoreWithFcPP()
            BeanUtils.copyProperties(score, result)
            return result
        }
    }}
