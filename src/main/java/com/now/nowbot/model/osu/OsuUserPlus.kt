package com.now.nowbot.model.osu

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonUnwrapped
import com.now.nowbot.entity.IDUser
import com.now.nowbot.entity.UserProfileLite
import tools.jackson.databind.PropertyNamingStrategies
import tools.jackson.databind.annotation.JsonNaming

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
class OsuUserPlus(
    @field:JsonUnwrapped
    val user: OsuUser,

    @field:JsonProperty("profile")
    var profile: UserProfileLite? = null
): IDUser {
    @get:JsonIgnore
    override val userID: Long = user.userID

    @get:JsonIgnore
    override val username: String = user.username
}
