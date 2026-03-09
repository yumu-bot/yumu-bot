package com.now.nowbot.model.osu

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.now.nowbot.entity.UserProfileLite
import org.springframework.beans.BeanUtils
import tools.jackson.databind.PropertyNamingStrategies
import tools.jackson.databind.annotation.JsonNaming

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonNaming(
    PropertyNamingStrategies.SnakeCaseStrategy::class
)
class OsuUserPlus : OsuUser() {
    var profile: UserProfileLite? = null

    companion object {
        @JvmStatic
        fun copyOf(user: OsuUser): OsuUserPlus {
            val result = OsuUserPlus()
            BeanUtils.copyProperties(user, result)
            return result
        }
    }
}
