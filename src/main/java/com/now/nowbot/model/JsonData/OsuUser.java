package com.now.nowbot.model.JsonData;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.now.nowbot.model.enums.OsuMode;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class OsuUser {
    Long id;
    Double pp;
    String username;
    @JsonProperty("statistics")
    Statistics statistics;


    public record Country(String  countryCode, String  countryName){}
    public record Kudosu(Integer total, Integer available){}
    public record RankHistory(OsuMode mode, List<Integer> history){}

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
    /** 这是什么我也不知道 */
    @JsonProperty("beatmap_playcounts_count")
    Integer beatmapSetCountPlaycounts;
    @JsonProperty("mapping_follower_count")
    Integer mappingFollowerCount;
    @JsonProperty("has_supported")
    Boolean hasSupported;
    @JsonProperty("is_bot")
    Boolean isBot;
    @JsonProperty("pm_friends_only")
    Boolean pmFriendsOnly;
    Cover cover;
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
    @JsonProperty("avatar_url")
    String avatarUrl;
    @JsonProperty("cover_url")
    String coverUrl;


    @JsonIgnore
    Country country;
    @JsonProperty("country")
    void setCountry(Map<String,String> country){
        if (country != null)
            this.country = new Country(country.get("code"),country.get("name"));
    }
    @JsonIgnore
    Kudosu kudosu;
    @JsonProperty("kudosu")
    void setKudosu(Map<String,Integer> kudosu){
        if (kudosu != null)
            this.kudosu = new Kudosu(kudosu.get("total"),kudosu.get("available"));
    }
    @JsonIgnore
    RankHistory rankHistory;
    @JsonProperty("rank_history")
    void setRankHistory(Map<String,Object> map){
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
        if (pp == null && statistics != null){
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

    public Boolean isBot() {
        return isBot;
    }

    public void setBot(Boolean bot) {
        isBot = bot;
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
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
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

    public void setInterests(String interests) {
        this.interests = interests;
    }
}
