package com.now.nowbot.model.json;

import com.fasterxml.jackson.annotation.*;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true) // 允许忽略json没有的值赋为空
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY) //扫描非public的值并注入
public class MicroUser implements Comparable<MicroUser> {
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

    // 通过 LazerFriend 设置
    @JsonProperty("is_mutual")
    Boolean isMutual;

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
    public Boolean getPmFriendsOnly() {
        return pmFriendsOnly;
    }

    public void setPmFriendsOnly(Boolean pmFriendsOnly) {
        this.pmFriendsOnly = pmFriendsOnly;
    }

    public Boolean getBot() {
        return isBot;
    }

    public void setBot(Boolean bot) {
        isBot = bot;
    }

    public Boolean getDeleted() {
        return isDeleted;
    }

    public void setDeleted(Boolean deleted) {
        isDeleted = deleted;
    }

    public Boolean getOnline() {
        return isOnline;
    }

    public void setOnline(Boolean online) {
        isOnline = online;
    }

    public Boolean getSupporter() {
        return isSupporter;
    }

    public void setSupporter(Boolean supporter) {
        isSupporter = supporter;
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

    public void setStatistics(Statistics statistics) {
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

    public void setCover(Cover cover) {
        this.cover = cover;
    }

    public String getCountryCode() {
        return countryCode;
    }


    public Boolean getMutual() {
        return isMutual;
    }

    public void setMutual(Boolean mutual) {
        isMutual = mutual;
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
        return "MicroUser{" + "avatarUrl='" + avatarUrl + '\'' +
               ", coverUrl='" + coverUrl + '\'' +
               ", group='" + group + '\'' +
               ", id=" + id +
               ", active=" + active +
               ", isBot=" + isBot +
               ", isDeleted=" + isDeleted +
               ", isOnline=" + isOnline +
               ", isSupporter=" + isSupporter +
               ", lastTime='" + lastTime + '\'' +
               ", pmFriendsOnly=" + pmFriendsOnly +
               ", userName='" + userName + '\'' +
               ", cover=" + cover +
               ", countryCode='" + countryCode + '\'' +
               ", country=" + country +
               ", isMutual=" + isMutual +
               ", statistics=" + statistics +
               ", rulesets=" + rulesets +
               '}';
    }

    @Override
    public int compareTo(@NotNull MicroUser u) {
        return Math.toIntExact(this.getId() - u.getId());
    }
}
