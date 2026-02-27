package com.now.nowbot.model.osu

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty

data class Tag(
    @field:JsonProperty("id") var id: Int = 0,

    @set:JsonProperty("name") @get:JsonIgnore var name: String = "",

    @field:JsonProperty("ruleset_id") var rulesetID: Byte? = null,

    @field:JsonProperty("description") var description: String = "",
) {
    @get:JsonProperty("category") val category: String
        get() = name.split("/").firstOrNull() ?: "undefined"

    @get:JsonProperty("type") val type: String
        get() = name.split("/").lastOrNull() ?: "undefined"

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return if (other is Tag) {
            other.id == this.id
        } else {
            false
        }
    }
}