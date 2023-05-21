package com.now.nowbot.model.JsonData;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.now.nowbot.model.enums.OsuMode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class OsuUser{
    Double pp;

    public record Country(String countryCode, String countryName) {
    }

    public record Kudosu(Integer total, Integer available) {
    }

    public record RankHistory(OsuMode mode, List<Integer> history) {
    }

    public record MonthlyPlayCount(String startDate, Integer count) {
    }

    Long id;
    @JsonProperty("avatar_url")
    String avatarUrl;

    @JsonProperty("cover_url")
    String coverUrl;
    @JsonProperty("default_group")
    String group;

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
    @JsonIgnore
    String countryCode;
    @JsonProperty("playmode")
    String playMode;
    String occupation;
    String discord;
    String interests;
    @JsonProperty("unranked_beatmapset_count")
    Integer beatmapSetCountUnranked;
    @JsonProperty("ranked_beatmapset_count")
    Integer beatmapSetCountRanked;
    @JsonProperty("ranked_and_approved_beatmapset_count")
    Integer beatmapSetCountRankedAndApproved;

    @JsonProperty("beatmap_playcounts_count")
    Integer beatmapSetCountPlaycounts;
    @JsonProperty("mapping_follower_count")
    Integer mappingFollowerCount;
    @JsonProperty("has_supported")
    Boolean hasSupported;
    @JsonProperty("profile_order")
    List<String> profileOrder;
    @JsonProperty("previous_usernames")
    List<String> previousName;
    @JsonProperty("join_date")
    String joinDate;
    @JsonProperty("max_friends")
    Integer maxFriends;
    @JsonProperty("comments_count")
    Integer commentsCount;
    @JsonProperty("support_level")
    short supportLeve;
    @JsonProperty("post_count")
    Integer postCount;
    @JsonProperty("follower_count")
    Integer followerCount;
    @JsonProperty("raw")
    String page;

    @JsonProperty("statistics")
    Statistics statistics;

    @JsonProperty("cover")
    Cover cover;
    @JsonIgnoreProperties
    List<MonthlyPlayCount> monthlyPlaycounts;

    @JsonProperty("monthly_playcounts")
    void setMonthlyPlayCount(List<HashMap<String, Object>> dataList) {
        monthlyPlaycounts = new ArrayList<>(dataList.size());
        for (var d : dataList) {
            var mp = new MonthlyPlayCount((String) d.get("start_date"), (Integer) d.get("count"));
            monthlyPlaycounts.add(mp);
        }
    }


    @JsonIgnore
    Country country;

    @JsonProperty("country")
    void setCountry(Map<String, String> country) {
        if (country != null)
            this.country = new Country(country.get("code"), country.get("name"));
    }

    @JsonIgnore
    Kudosu kudosu;

    @JsonProperty("kudosu")
    void setKudosu(Map<String, Integer> kudosu) {
        if (kudosu != null)
            this.kudosu = new Kudosu(kudosu.get("total"), kudosu.get("available"));
    }

    @JsonIgnore
    RankHistory rankHistory;

    @JsonProperty("rank_history")
    void setRankHistory(Map<String, Object> map) {
        if (map != null)
            this.rankHistory = new RankHistory(OsuMode.getMode((String) map.get("mode")), (List<Integer>) map.get("data"));
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Double getPP() {
        if (pp == null && statistics != null) {
            return statistics.getPP();
        }
        return pp;
    }



    public Integer getFollowerCount() {
        return followerCount;
    }

    public void setFollowerCount(Integer followerCount) {
        this.followerCount = followerCount;
    }

    public OsuMode getPlayMode() {
        return OsuMode.getMode(playMode);
    }

    public void setPlayMode(String playMode) {
        this.playMode = playMode;
    }

    public void setPp(Double pp) {
        this.pp = pp;
    }

    public Statistics getStatistics() {
        return statistics;
    }

    public void setStatistics(Statistics statistics) {
        this.statistics = statistics;
    }

    public String getOccupation() {
        return occupation;
    }

    public void setOccupation(String occupation) {
        this.occupation = occupation;
    }

    public Integer getBeatmapSetCountUnranked() {
        return beatmapSetCountUnranked;
    }

    public void setBeatmapSetCountUnranked(Integer beatmapSetCountUnranked) {
        this.beatmapSetCountUnranked = beatmapSetCountUnranked;
    }

    public Integer getBeatmapSetCountRanked() {
        return beatmapSetCountRanked;
    }

    public void setBeatmapSetCountRanked(Integer beatmapSetCountRanked) {
        this.beatmapSetCountRanked = beatmapSetCountRanked;
    }

    public Integer getBeatmapSetCountRankedAndApproved() {
        return beatmapSetCountRankedAndApproved;
    }

    public void setBeatmapSetCountRankedAndApproved(Integer beatmapSetCountRankedAndApproved) {
        this.beatmapSetCountRankedAndApproved = beatmapSetCountRankedAndApproved;
    }

    public Integer getBeatmapSetCountPlaycounts() {
        return beatmapSetCountPlaycounts;
    }

    public void setBeatmapSetCountPlaycounts(Integer beatmapSetCountPlaycounts) {
        this.beatmapSetCountPlaycounts = beatmapSetCountPlaycounts;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public Integer getMappingFollowerCount() {
        return mappingFollowerCount;
    }

    public void setMappingFollowerCount(Integer mappingFollowerCount) {
        this.mappingFollowerCount = mappingFollowerCount;
    }

    public Boolean HasSupported() {
        return hasSupported;
    }

    public void setHasSupported(Boolean hasSupported) {
        this.hasSupported = hasSupported;
    }


    public Boolean getPmFriendsOnly() {
        return pmFriendsOnly;
    }

    public void setPmFriendsOnly(Boolean pmFriendsOnly) {
        this.pmFriendsOnly = pmFriendsOnly;
    }

    public Cover getCover() {
        return cover;
    }

    public void setCover(Cover cover) {
        this.cover = cover;
    }

    public List<String> getProfileOrder() {
        return profileOrder;
    }

    public void setProfileOrder(List<String> profileOrder) {
        this.profileOrder = profileOrder;
    }

    public List<String> getPreviousName() {
        return previousName;
    }

    public void setPreviousName(List<String> previousName) {
        this.previousName = previousName;
    }

    public String getJoinDate() {
        return joinDate;
    }

    public void setJoinDate(String joinDate) {
        this.joinDate = joinDate;
    }

    public Integer getMaxFriends() {
        return maxFriends;
    }

    public void setMaxFriends(Integer maxFriends) {
        this.maxFriends = maxFriends;
    }

    public Integer getCommentsCount() {
        return commentsCount;
    }

    public void setCommentsCount(Integer commentsCount) {
        this.commentsCount = commentsCount;
    }

    public short getSupportLeve() {
        return supportLeve;
    }

    public void setSupportLeve(short supportLeve) {
        this.supportLeve = supportLeve;
    }

    public Integer getPostCount() {
        return postCount;
    }

    /**
     * 论坛发帖数
     *
     * @param postCount 论坛发帖数
     */
    public void setPostCount(Integer postCount) {
        this.postCount = postCount;
    }

    public String getPage() {
        return page;
    }

    public void setPage(String page) {
        this.page = page;
    }

    public Country getCountry() {
        return country;
    }

    public void setCountry(Country country) {
        this.country = country;
    }

    public Kudosu getKudosu() {
        return kudosu;
    }

    public void setKudosu(Kudosu kudosu) {
        this.kudosu = kudosu;
    }

    public RankHistory getRankHistory() {
        return rankHistory;
    }

    public void setRankHistory(RankHistory rankHistory) {
        this.rankHistory = rankHistory;
    }

    public String getUsername() {
        return userName;
    }

    public void setUsername(String username) {
        this.userName = username;
    }

    public Double getPp() {
        return pp;
    }

    public Boolean getHasSupported() {
        return hasSupported;
    }

    public List<MonthlyPlayCount> getMonthlyPlaycounts() {
        return monthlyPlaycounts;
    }

    public void setMonthlyPlaycounts(List<MonthlyPlayCount> monthlyPlaycounts) {
        this.monthlyPlaycounts = monthlyPlaycounts;
    }

    public Double getAccuracy() {
        if (statistics != null) {
            return statistics.getAccuracy();
        }
        return null;
    }

    public Long getPlayCount() {
        if (statistics != null) {
            return statistics.getPlayCount();
        }
        return null;
    }

    public Long getPlayTime() {
        if (statistics != null) {
            return statistics.getPlayTime();
        }
        return null;
    }

    public Long getTotalHits() {
        if (statistics != null) {
            return statistics.getTotalHits();
        }
        return null;
    }

    public Integer getMaxCombo() {
        if (statistics != null) {
            return statistics.getMaxCombo();
        }
        return null;
    }

    public Long getGlobalRank() {
        if (statistics != null) {
            return statistics.getGlobalRank();
        }
        return null;
    }

    public Long getCountryRank() {
        if (statistics != null) {
            return statistics.getCountryRank();
        }
        return null;
    }

    public Integer getLevelCurrent() {
        if (statistics != null) {
            return statistics.getLevelCurrent();
        }
        return null;
    }

    public Integer getLevelProgress() {
        if (statistics != null) {
            return statistics.getLevelProgress();
        }
        return null;
    }

    public String getDiscord() {
        return discord;
    }

    public void setDiscord(String discord) {
        this.discord = discord;
    }

    public String getInterests() {
        return interests;
    }

    public String getCoverUrl(){
        return coverUrl;
    }

    public void setInterests(String interests) {
        this.interests = interests;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("OsuUser{");
        sb.append("id=").append(id);
        sb.append(", pp=").append(pp);
        sb.append(", username='").append(userName).append('\'');
        sb.append(", statistics=").append(statistics);
        sb.append(", playMode='").append(playMode).append('\'');
        sb.append(", occupation='").append(occupation).append('\'');
        sb.append(", discord='").append(discord).append('\'');
        sb.append(", interests='").append(interests).append('\'');
        sb.append(", beatmapSetCountUnranked=").append(beatmapSetCountUnranked);
        sb.append(", beatmapSetCountRanked=").append(beatmapSetCountRanked);
        sb.append(", beatmapSetCountRankedAndApproved=").append(beatmapSetCountRankedAndApproved);
        sb.append(", beatmapSetCountPlaycounts=").append(beatmapSetCountPlaycounts);
        sb.append(", mappingFollowerCount=").append(mappingFollowerCount);
        sb.append(", hasSupported=").append(hasSupported);
        sb.append(", isBot=").append(isBot);
        sb.append(", pmFriendsOnly=").append(pmFriendsOnly);
        sb.append(", cover=").append(cover);
        sb.append(", profileOrder=").append(profileOrder);
        sb.append(", previousName=").append(previousName);
        sb.append(", joinDate='").append(joinDate).append('\'');
        sb.append(", maxFriends=").append(maxFriends);
        sb.append(", commentsCount=").append(commentsCount);
        sb.append(", supportLeve=").append(supportLeve);
        sb.append(", postCount=").append(postCount);
        sb.append(", followerCount=").append(followerCount);
        sb.append(", page='").append(page).append('\'');
        sb.append(", avatarUrl='").append(avatarUrl).append('\'');
        sb.append(", coverUrl='").append(coverUrl).append('\'');
        sb.append(", country=").append(country);
        sb.append(", kudosu=").append(kudosu);
        sb.append(", rankHistory=").append(rankHistory);
        sb.append('}');
        return sb.toString();
    }
}
