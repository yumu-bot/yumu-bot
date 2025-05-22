package com.now.nowbot.model.json

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

@JsonInclude(JsonInclude.Include.NON_NULL) @JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
class Cover {
    @JsonProperty("custom_url")
    var custom: String? = null

    var url: String? = null

    @set:JsonProperty("id")
    @get:JsonIgnoreProperties
    var idStr: String? = null

    @get:JsonProperty("id")
    val id: Int?
        get() = if (!idStr.isNullOrBlank()) {
            idStr!!.toIntOrNull()
        } else {
            null
        }


    constructor()

    constructor(custom: String, url: String, id: Int) {
        this.custom = custom
        this.url = url
        this.idStr = id.toString()
    }

    override fun toString(): String {
        return "Cover(custom=$custom, url=$url, id=$id)"
    }
}
