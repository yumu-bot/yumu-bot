package com.now.nowbot.model.osu;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.now.nowbot.entity.UserProfileLite;
import org.springframework.beans.BeanUtils;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class OsuUserPlus extends OsuUser {
    UserProfileLite profile;

    public static OsuUserPlus copyOf(OsuUser user) {
        var result = new OsuUserPlus();
        BeanUtils.copyProperties(user, result);
        return result;
    }

    public UserProfileLite getProfile() {
        return profile;
    }

    public void setProfile(UserProfileLite profile) {
        this.profile = profile;
    }
}
