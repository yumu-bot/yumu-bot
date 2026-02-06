package com.now.nowbot.model.osu

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnore
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
    @get:JsonIgnore
    var idStr: String? = null

    @get:JsonProperty("id")
    val id: Int?
        get() = if (!idStr.isNullOrBlank()) {
            idStr!!.toIntOrNull()
        } else {
            null
        }

    override fun toString(): String {
        return "Cover(custom=$custom, url=$url, id=$id)"
    }
}
