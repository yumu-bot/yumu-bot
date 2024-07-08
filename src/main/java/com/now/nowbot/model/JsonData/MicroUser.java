package com.now.nowbot.model.JsonData;

import com.fasterxml.jackson.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true) // 允许忽略json没有的值赋为空
@JsonInclude(JsonInclude.Include.NON_NULL)
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

    // 只有 getUsers包含
    @JsonProperty("statistics_rulesets")
    UserStatisticsRulesets rulesets;

    public String getAvatarUrl() {
        return avatarUrl;
    }
    public void setAvatarUrl(String avatar) {
        this.avatarUrl = avatar;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
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

    public Long getUserID() {
        return id;
    }

    public void setUserID(Long id) {
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

    public UserStatisticsRulesets getRulesets() {
        return rulesets;
    }

    public void setRulesets(UserStatisticsRulesets rulesets) {
        this.rulesets = rulesets;
    }

    @Override
    public int hashCode() {
        return getUserID().hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (! (o instanceof MicroUser microUser)) return false;

        return getUserID().equals(microUser.getUserID());
    }

    @Override
    public String toString() {
        return STR."MicroUser{avatar='\{avatarUrl}\{'\''}, group='\{group}\{'\''}, id=\{id}, active=\{active}, bot=\{isBot}, deleted=\{isDeleted}, online=\{isOnline}, supporter=\{isSupporter}, lastTime='\{lastTime}\{'\''}, pmOnly=\{pmFriendsOnly}, name='\{userName}\{'\''}, countryCode='\{countryCode}\{'\''}, country=\{country}, cover=\{cover}, statistics=\{statistics}\{'}'}";
    }
}
