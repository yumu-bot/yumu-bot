package com.now.nowbot.model.JsonData;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true) // 允许忽略json没有的值赋为空
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY) //扫描非public的值并注入
public class MicroUser {
    @JsonProperty("avatar_url")
    String avatarUrl;

    @JsonProperty("cover_url")
    String coverUrl;
    @JsonProperty("default_group")
    String group;
    Long id;

    @JsonProperty("is_active")
    Boolean active;
    @JsonProperty("is_bot")
    Boolean isBot;
    @JsonProperty("is_deleted")
    Boolean isDeleted;
    @JsonProperty("is_online")
    Boolean isOnline;
    @JsonProperty("is_supporter")
    Boolean isSupporter;
    @JsonProperty("last_visit")
    String lastTime;
    @JsonProperty("pm_friends_only")
    Boolean pmFriendsOnly;
    @JsonProperty("username")
    String userName;
    Cover cover;
    @JsonIgnore
    String countryCode;
    @JsonIgnore
    OsuUser.Country country;

    @JsonProperty("country_code")
    void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    @JsonProperty("country")
    void setCountry(Map<String, String> country) {
        if (country != null)
            this.country = new OsuUser.Country(country.get("code"), country.get("name"));
    }


    @JsonProperty("statistics")
    Statistics statistics;

    public String getAvatar() {
        return avatarUrl;
    }
    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatar(String avatar) {
        this.avatarUrl = avatar;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Boolean getIsBot() {
        return isBot;
    }

    public void setIsBot(Boolean isBot) {
        this.isBot = isBot;
    }

    public Boolean getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(Boolean isDeleted) {
        this.isDeleted = isDeleted;
    }

    public Boolean getIsOnline() {
        return isOnline;
    }

    public void setIsOnline(Boolean isOnline) {
        this.isOnline = isOnline;
    }

    public Boolean getIsSupporter() {
        return isSupporter;
    }

    public void setIsSupporter(Boolean isSupporter) {
        this.isSupporter = isSupporter;
    }

    public LocalDateTime getLastTime() {
        if (lastTime != null) return LocalDateTime.parse(lastTime, Score.formatter);
        return LocalDateTime.now();
    }

    public void setLastTime(String lastTime) {
        this.lastTime = lastTime;
    }

    public Boolean getPmOnly() {
        return pmFriendsOnly;
    }

    public void setPmOnly(Boolean pmFriendsOnly) {
        this.pmFriendsOnly = pmFriendsOnly;
    }

    public OsuUser.Country getCountry() {
        return country;
    }

    public void setCountry(OsuUser.Country country) {
        this.country = country;
    }

    public Statistics getStatistics() {
        return statistics;
    }

    public void setStatustucs(Statistics statistics) {
        this.statistics = statistics;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Cover getCover() {
        return cover;
    }

    public String getCountryCode() {
        return countryCode;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("MicroUser{");
        sb.append("avatar='").append(avatarUrl).append('\'');
        sb.append(", group='").append(group).append('\'');
        sb.append(", id=").append(id);
        sb.append(", active=").append(active);
        sb.append(", bot=").append(isBot);
        sb.append(", deleted=").append(isDeleted);
        sb.append(", online=").append(isOnline);
        sb.append(", supporter=").append(isSupporter);
        sb.append(", lastTime='").append(lastTime).append('\'');
        sb.append(", pmOnly=").append(pmFriendsOnly);
        sb.append(", name='").append(userName).append('\'');
        sb.append(", countryCode='").append(countryCode).append('\'');
        sb.append(", country=").append(country);
        sb.append(", cover=").append(cover);
        sb.append(", statistics=").append(statistics);
        sb.append('}');
        return sb.toString();
    }
}
