package com.now.nowbot.model.jsonData;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.now.nowbot.entity.UserProfile;
import org.springframework.beans.BeanUtils;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class ScoreWithUserProfile extends Score {
    UserProfile profile;

    public static ScoreWithUserProfile copyOf(Score score) {
        var result = new ScoreWithUserProfile();
        BeanUtils.copyProperties(score, result);
        return result;
    }

    public UserProfile getProfile() {
        return profile;
    }

    public void setProfile(UserProfile profile) {
        this.profile = profile;
    }
}
