package com.now.nowbot.model.json

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

class Tag {
    @JsonProperty("id")
    var id: Int = 0

    @set:JsonProperty("name")
    @get:JsonIgnoreProperties
    var name: String = ""

    @get:JsonProperty("category")
    val category: String
        get() {
            return name.split("/").firstOrNull()
                ?: "undefined"
        }

    @get:JsonProperty("type")
    val type: String
        get() {
            return name.split("/").lastOrNull()
                ?: "undefined"
        }

    @JsonProperty("ruleset_id")
    var rulesetID: Byte? = null

    @JsonProperty("description")
    var description: String = ""
}