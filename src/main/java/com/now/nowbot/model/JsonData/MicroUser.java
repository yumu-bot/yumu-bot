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
    String avatar;
    @JsonProperty("default_group")
    String group;
    Long id;

    @JsonProperty("is_active")
    Boolean active;
    @JsonProperty("is_bot")
    Boolean bot;
    @JsonProperty("is_deleted")
    Boolean deleted;
    @JsonProperty("is_online")
    Boolean online;
    @JsonProperty("is_supporter")
    Boolean supporter;
    @JsonProperty("last_visit")
    String lastTime;
    @JsonProperty("pm_friends_only")
    Boolean pmOnly;
    @JsonProperty("username")
    String name;
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

    public record Cover(String customUrl, String url, String id) {
        public Long getId() {
            if (id != null) return Long.parseLong(id);
            return null;
        }
    }

    ;
    @JsonIgnore
    Cover cover;

    @JsonProperty("cover")
    void setCover(Map<String, String> cover) {
        if (cover != null)
            this.cover = new Cover(cover.get("custom_url"), cover.get("url"), cover.get("id"));
    }

    @JsonProperty("statistics")
    Statistics statistics;

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
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

    public Boolean getBot() {
        return bot;
    }

    public void setBot(Boolean bot) {
        this.bot = bot;
    }

    public Boolean getDeleted() {
        return deleted;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }

    public Boolean getOnline() {
        return online;
    }

    public void setOnline(Boolean online) {
        this.online = online;
    }

    public Boolean getSupporter() {
        return supporter;
    }

    public void setSupporter(Boolean supporter) {
        this.supporter = supporter;
    }

    public LocalDateTime getLastTime() {
        if (lastTime != null) return LocalDateTime.parse(lastTime, Score.formatter);
        return LocalDateTime.now();
    }

    public void setLastTime(String lastTime) {
        this.lastTime = lastTime;
    }

    public Boolean getPmOnly() {
        return pmOnly;
    }

    public void setPmOnly(Boolean pmOnly) {
        this.pmOnly = pmOnly;
    }

    public OsuUser.Country getCountry() {
        return country;
    }

    public void setCountry(OsuUser.Country country) {
        this.country = country;
    }

    public Statistics getStatustucs() {
        return statistics;
    }

    public void setStatustucs(Statistics statistics) {
        this.statistics = statistics;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
        sb.append("avatar='").append(avatar).append('\'');
        sb.append(", group='").append(group).append('\'');
        sb.append(", id=").append(id);
        sb.append(", active=").append(active);
        sb.append(", bot=").append(bot);
        sb.append(", deleted=").append(deleted);
        sb.append(", online=").append(online);
        sb.append(", supporter=").append(supporter);
        sb.append(", lastTime='").append(lastTime).append('\'');
        sb.append(", pmOnly=").append(pmOnly);
        sb.append(", name='").append(name).append('\'');
        sb.append(", countryCode='").append(countryCode).append('\'');
        sb.append(", country=").append(country);
        sb.append(", cover=").append(cover);
        sb.append(", statistics=").append(statistics);
        sb.append('}');
        return sb.toString();
    }
}
