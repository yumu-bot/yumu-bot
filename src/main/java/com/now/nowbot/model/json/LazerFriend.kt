package com.now.nowbot.model.json

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
class LazerFriend {
    @JsonProperty("target_id")
    var targetID: Long = 0L

    @JsonProperty("relation_type")
    var relationType: String = ""

    @JsonProperty("mutual")
    var isMutual: Boolean = false

    @JsonProperty("target")
    var target: MicroUser = MicroUser()
        get()  = field.apply {
            isMutual = this@LazerFriend.isMutual
        }
}
