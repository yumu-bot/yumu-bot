package com.now.nowbot.model.JsonData;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.service.OsuApiService.OsuUserApiService;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class OsuUser {
    @JsonProperty("avatar_url")
    String avatarUrl;

    @JsonProperty("country_code")
    String countryCode;

    @JsonProperty("default_group")
    @Nullable
    String defaultGroup;

    //不要动这个
    Long id;

    //最近有活跃？
    @JsonProperty("is_active")
    Boolean isActive;

    @JsonProperty("is_bot")
    Boolean isBot;

    @JsonProperty("is_deleted")
    Boolean isDeleted;

    @JsonProperty("is_online")
    Boolean isOnline;

    @JsonProperty("is_supporter")
    Boolean isSupporter;

    @JsonProperty("is_restricted")
    Boolean isRestricted;

    @JsonProperty("last_visit")
    OffsetDateTime lastVisit;

    @JsonProperty("pm_friends_only")
    Boolean pmFriendsOnly;

    @JsonProperty("profile_colour")
    @Nullable
    String profileColor;

    String username;

    // Optional attributes

    @JsonProperty("cover_url")
    String coverUrl;

    String discord;

    @JsonProperty("has_supported")
    Boolean hasSupported;

    String interests;

    @JsonProperty("join_date")
    OffsetDateTime joinDate;

    String location;

    @JsonProperty("max_blocks")
    Integer maxBlocks;

    @JsonProperty("max_friends")
    Integer maxFriends;

    String occupation;

    @JsonProperty("playmode")
    String playMode;

    @JsonProperty("playstyle")
    List<String> playStyle;

    @JsonProperty("post_count")
    Integer postCount;

    @JsonProperty("profile_order")
    List<String> profileOrder;

    @Nullable
    String title;

    @JsonProperty("title_url")
    @Nullable
    String titleUrl;

    String twitter;

    String website;

    @JsonProperty("country")
    Country country;

    public record Country(String code, String name) {
    }

    @JsonProperty("cover")
    Cover cover;

    @JsonProperty("kudosu")
    Kudosu kudosu;

    public record Kudosu(Integer available, Integer total) {}

    @JsonProperty("account_history")
    @Nullable
    List<UserAccountHistory> accountHistory;

    //type: note, restriction, silence.
    public record UserAccountHistory (
            @Nullable String description, Long id, Integer length, Boolean permanent, OffsetDateTime timestamp, String type
    ) {}

    @JsonProperty("active_tournament_banner")
    @Nullable
    ProfileBanner profileBanner;

    public record ProfileBanner (
            Long id, Long tournament_id, @Nullable String image,

            @JsonProperty("image@2x")
            @Nullable String image2x
    ) {}


    @JsonProperty("active_tournament_banners")
    List<ProfileBanner> profileBanners;

    List<UserBadge> badges;

    public record UserBadge (
            @JsonProperty("awarded_at") OffsetDateTime awardAt, String description, @JsonProperty("image@2x_url") String image2xUrl, @JsonProperty("image_url") String imageUrl, String url) {}

    @JsonProperty("beatmap_playcounts_count")
    Integer beatmapPlaycount;

    @JsonProperty("comments_count")
    Integer CommentsCount;

    @JsonProperty("favourite_beatmapset_count")
    Integer favoriteCount;


    @JsonProperty("follower_count")
    Integer followerCount;

    @JsonProperty("graveyard_beatmapset_count")
    Integer graveyardCount;

    @JsonProperty("groups")
    List<UserGroup> groups;

    @JsonProperty("guest_beatmapset_count")
    Integer guestCount;

    @JsonProperty("loved_beatmapset_count")
    Integer lovedCount;

    @JsonProperty("mapping_follower_count")
    Integer mappingFollowerCount;

    @JsonIgnoreProperties
    List<UserMonthly> monthlyPlaycounts;

    @JsonProperty("monthly_playcounts")
    void setMonthlyPlayCount(List<HashMap<String, Object>> dataList) {
        monthlyPlaycounts = new ArrayList<>(dataList.size());
        for (var d : dataList) {
            var mp = new UserMonthly((String) d.get("start_date"), (Integer) d.get("count"));
            monthlyPlaycounts.add(mp);
        }
    }


    public record UserMonthly (String start_date, Integer count) {}

    @JsonProperty("nominated_beatmapset_count")
    Integer nominatedCount;

    Page page;

    public record Page(String html, String raw) {}

    @JsonProperty("pending_beatmapset_count")
    Integer pendingCount;

    @JsonProperty("previous_usernames")
    List<String> previousNames;

    @JsonProperty("rank_highest")
    HighestRank highestRank;

    public record HighestRank(Integer rank, @JsonProperty("updated_at") OffsetDateTime updatedAt) {}

    @JsonProperty("ranked_beatmapset_count")
    Integer rankedCount;

    @JsonIgnoreProperties
    List<UserMonthly> replaysWatchedCounts;

    @JsonProperty("replays_watched_counts")
    void setReplaysWatchedCount(List<HashMap<String, Object>> dataList) {
        replaysWatchedCounts = new ArrayList<>(dataList.size());
        for (var d : dataList) {
            var mp = new UserMonthly((String) d.get("start_date"), (Integer) d.get("count"));
            replaysWatchedCounts.add(mp);
        }
    }

    @JsonProperty("scores_best_count")
    Integer scoreBestCount;

    @JsonProperty("scores_first_count")
    Integer scoreFirstCount;

    @JsonProperty("scores_pinned_count")
    Integer scorePinnedCount;

    @JsonProperty("scores_recent_count")
    Integer scoreRecentCount;

    @JsonProperty("statistics")
    Statistics statistics;

    @JsonProperty("support_level")
    Integer supportLevel;


    @JsonProperty("user_achievements")
    List<UserAchievement> userAchievements;

    public record UserAchievement(@JsonProperty("achieved_at") OffsetDateTime achievedAt, @JsonProperty("achievement_id") Integer achievementID) {}

    @JsonProperty("rank_history")
    RankHistory rankHistory;

    public record RankHistory(String mode, List<Integer> data) {}

    // ranked 和 pending
    /*
    @JsonProperty("ranked_and_approved_beatmapset_count")
    Integer rankedAndApprovedCount;

    @JsonProperty("unranked_beatmapset_count")
    Integer unrankedCount;

     */

    //自己算
    Double PP;

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    @Nullable
    public String getDefaultGroup() {
        return defaultGroup;
    }

    public void setDefaultGroup(@Nullable String defaultGroup) {
        this.defaultGroup = defaultGroup;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUID() {
        return id;
    }

    public void setUID(Long id) {
        this.id = id;
    }

    public Boolean getActive() {
        return isActive;
    }

    public void setActive(Boolean active) {
        isActive = active;
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

    public OffsetDateTime getLastVisit() {
        return lastVisit;
    }

    public void setLastVisit(OffsetDateTime lastVisit) {
        this.lastVisit = lastVisit;
    }

    //这个是把基础 OsuUser 转换成完整 OsuUser 的方法
    public void parseFull(OsuUserApiService osuUserApiService) {
        OsuUser o;
        try {
            o = osuUserApiService.getPlayerInfo(this.getUID());
        } catch (Exception e) {
            return;
        }

        this.setAccountHistory(o.getAccountHistory());
        this.setActive(o.getActive());
        this.setAvatarUrl(o.getAvatarUrl());
        this.setBot(o.getBot());
        this.setBadges(o.getBadges());
        this.setBeatmapPlaycount(o.getBeatmapPlaycount());
        this.setCommentsCount(o.getCommentsCount());
        this.setCountry(o.getCountry());
        this.setCountryCode(o.getCountryCode());
        this.setCover(o.getCover());
        this.setCoverUrl(o.getCoverUrl());
        this.setDefaultGroup(o.getDefaultGroup());
        this.setDeleted(o.getDeleted());
        this.setDiscord(o.getDiscord());
        this.setFavoriteCount(o.getFavoriteCount());
        this.setFollowerCount(o.getFollowerCount());
        this.setGroups(o.getGroups());
        this.setGraveyardCount(o.getGraveyardCount());
        this.setGuestCount(o.getGuestCount());
        this.setHasSupported(o.getHasSupported());
        this.setHighestRank(o.getHighestRank());
        this.setId(o.getId());
        this.setInterests(o.getInterests());
        this.setJoinDate(o.getJoinDate());
        this.setKudosu(o.getKudosu());
        this.setLastVisit(o.getLastVisit());
        this.setLocation(o.getLocation());
        this.setLovedCount(o.getLovedCount());
        this.setMappingFollowerCount(o.getMappingFollowerCount());
        this.setMaxBlocks(o.getMaxBlocks());
        this.setMaxFriends(o.getMaxFriends());
        this.setMonthlyPlaycounts(o.getMonthlyPlaycounts());
        this.setNominatedCount(o.getNominatedCount());
        this.setOccupation(o.getOccupation());
        this.setOnline(o.getOnline());
        this.setPage(o.getPage());
        this.setPP(o.getPP());
        this.setPendingCount(o.getPendingCount());
        this.setPlayMode(o.getPlayMode());
        this.setPmFriendsOnly(o.getPmFriendsOnly());
        this.setPlayStyle(o.getPlayStyle());
        this.setPreviousNames(o.getPreviousNames());
        this.setProfileOrder(o.getProfileOrder());
        this.setProfileBanner(o.getProfileBanner());
        this.setProfileBanners(o.getProfileBanners());
        this.setProfileColor(o.getProfileColor());
        this.setPostCount(o.getPostCount());
        this.setRankHistory(o.getRankHistory());
        this.setRankedCount(o.getRankedCount());
        this.setReplaysWatchedCounts(o.getReplaysWatchedCounts());
        this.setStatistics(o.getStatistics());
        this.setSupporter(o.getSupporter());
        this.setSupportLevel(o.getSupportLevel());
        this.setScoreBestCount(o.getScoreBestCount());
        this.setScoreRecentCount(o.getScoreRecentCount());
        this.setScorePinnedCount(o.getScorePinnedCount());
        this.setScoreFirstCount(o.getScoreFirstCount());
        this.setTitle(o.getTitle());
        this.setTwitter(o.getTwitter());
        this.setTitleUrl(o.getTitleUrl());
        this.setUsername(o.getUsername());
        this.setUserAchievements(o.getUserAchievements());
        this.setWebsite(o.getWebsite());
    }

    public Boolean getPmFriendsOnly() {
        return pmFriendsOnly;
    }

    @Nullable
    public String getProfileColor() {
        return profileColor;
    }

    public void setProfileColor(@Nullable String profileColor) {
        this.profileColor = profileColor;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    public String getDiscord() {
        return discord;
    }

    public void setDiscord(String discord) {
        this.discord = discord;
    }

    public Boolean getHasSupported() {
        return hasSupported;
    }

    public void setHasSupported(Boolean hasSupported) {
        this.hasSupported = hasSupported;
    }

    public String getInterests() {
        return interests;
    }

    public void setInterests(String interests) {
        this.interests = interests;
    }

    public OffsetDateTime getJoinDate() {
        return joinDate;
    }

    public void setJoinDate(OffsetDateTime joinDate) {
        this.joinDate = joinDate;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Integer getMaxBlocks() {
        return maxBlocks;
    }

    public void setMaxBlocks(Integer maxBlocks) {
        this.maxBlocks = maxBlocks;
    }

    public Integer getMaxFriends() {
        return maxFriends;
    }

    public void setMaxFriends(Integer maxFriends) {
        this.maxFriends = maxFriends;
    }

    public String getOccupation() {
        return occupation;
    }

    public void setOccupation(String occupation) {
        this.occupation = occupation;
    }

    public String getPlayMode() {
        return playMode;
    }

    public void setPlayMode(String playMode) {
        this.playMode = playMode;
    }

    public OsuMode getOsuMode() {
        return OsuMode.getMode(playMode);
    }

    public void setOsuMode(OsuMode osuMode) {
        this.playMode = osuMode.getName();
    }

    public List<String> getPlayStyle() {
        return playStyle;
    }

    public void setPlayStyle(List<String> playStyle) {
        this.playStyle = playStyle;
    }

    public Integer getPostCount() {
        return postCount;
    }

    public void setPostCount(Integer postCount) {
        this.postCount = postCount;
    }

    public List<String> getProfileOrder() {
        return profileOrder;
    }

    public void setProfileOrder(List<String> profileOrder) {
        this.profileOrder = profileOrder;
    }

    @Nullable
    public String getTitle() {
        return title;
    }

    public void setTitle(@Nullable String title) {
        this.title = title;
    }

    @Nullable
    public String getTitleUrl() {
        return titleUrl;
    }

    public void setTitleUrl(@Nullable String titleUrl) {
        this.titleUrl = titleUrl;
    }

    public String getTwitter() {
        return twitter;
    }

    public void setTwitter(String twitter) {
        this.twitter = twitter;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public Country getCountry() {
        return country;
    }

    public void setCountry(Country country) {
        this.country = country;
    }

    public Cover getCover() {
        return cover;
    }

    public void setCover(Cover cover) {
        this.cover = cover;
    }

    public Kudosu getKudosu() {
        return kudosu;
    }

    public void setKudosu(Kudosu kudosu) {
        this.kudosu = kudosu;
    }

    public @Nullable List<UserAccountHistory> getAccountHistory() {
        return accountHistory;
    }

    public void setAccountHistory(@Nullable List<UserAccountHistory> accountHistory) {
        this.accountHistory = accountHistory;
    }

    @Nullable
    public ProfileBanner getProfileBanner() {
        return profileBanner;
    }

    public void setProfileBanner(@Nullable ProfileBanner profileBanner) {
        this.profileBanner = profileBanner;
    }

    public List<ProfileBanner> getProfileBanners() {
        return profileBanners;
    }

    public void setProfileBanners(List<ProfileBanner> profileBanners) {
        this.profileBanners = profileBanners;
    }

    public List<UserBadge> getBadges() {
        return badges;
    }

    public void setBadges(List<UserBadge> badges) {
        this.badges = badges;
    }

    public Integer getBeatmapPlaycount() {
        return beatmapPlaycount;
    }

    public void setBeatmapPlaycount(Integer beatmapPlaycount) {
        this.beatmapPlaycount = beatmapPlaycount;
    }

    public Integer getCommentsCount() {
        return CommentsCount;
    }

    public void setCommentsCount(Integer commentsCount) {
        CommentsCount = commentsCount;
    }

    public Integer getFavoriteCount() {
        return favoriteCount;
    }

    public void setFavoriteCount(Integer favoriteCount) {
        this.favoriteCount = favoriteCount;
    }

    public Integer getFollowerCount() {
        return followerCount;
    }

    public void setFollowerCount(Integer followerCount) {
        this.followerCount = followerCount;
    }

    public Integer getGraveyardCount() {
        return graveyardCount;
    }

    public void setGraveyardCount(Integer graveyardCount) {
        this.graveyardCount = graveyardCount;
    }

    public List<UserGroup> getGroups() {
        return groups;
    }

    public void setGroups(List<UserGroup> groups) {
        this.groups = groups;
    }

    public Integer getGuestCount() {
        return guestCount;
    }

    public void setGuestCount(Integer guestCount) {
        this.guestCount = guestCount;
    }

    public Integer getLovedCount() {
        return lovedCount;
    }

    public void setLovedCount(Integer lovedCount) {
        this.lovedCount = lovedCount;
    }

    public Integer getMappingFollowerCount() {
        return mappingFollowerCount;
    }

    public void setMappingFollowerCount(Integer mappingFollowerCount) {
        this.mappingFollowerCount = mappingFollowerCount;
    }

    public List<UserMonthly> getMonthlyPlaycounts() {
        return monthlyPlaycounts;
    }

    public void setMonthlyPlaycounts(List<UserMonthly> monthlyPlaycounts) {
        this.monthlyPlaycounts = monthlyPlaycounts;
    }

    public Integer getNominatedCount() {
        return nominatedCount;
    }

    public void setNominatedCount(Integer nominatedCount) {
        this.nominatedCount = nominatedCount;
    }

    public Page getPage() {
        return page;
    }

    public void setPage(Page page) {
        this.page = page;
    }

    public Integer getPendingCount() {
        return pendingCount;
    }

    public void setPendingCount(Integer pendingCount) {
        this.pendingCount = pendingCount;
    }

    public List<String> getPreviousNames() {
        return previousNames;
    }

    public void setPreviousNames(List<String> previousNames) {
        this.previousNames = previousNames;
    }

    public HighestRank getHighestRank() {
        return highestRank;
    }

    public void setHighestRank(HighestRank highestRank) {
        this.highestRank = highestRank;
    }

    public Integer getRankedCount() {
        return rankedCount;
    }

    public void setRankedCount(Integer rankedCount) {
        this.rankedCount = rankedCount;
    }

    public List<UserMonthly> getReplaysWatchedCounts() {
        return replaysWatchedCounts;
    }

    public void setReplaysWatchedCounts(List<UserMonthly> replaysWatchedCounts) {
        this.replaysWatchedCounts = replaysWatchedCounts;
    }

    public Integer getScoreBestCount() {
        return scoreBestCount;
    }

    public void setScoreBestCount(Integer scoreBestCount) {
        this.scoreBestCount = scoreBestCount;
    }

    public Integer getScoreFirstCount() {
        return scoreFirstCount;
    }

    public void setScoreFirstCount(Integer scoreFirstCount) {
        this.scoreFirstCount = scoreFirstCount;
    }

    public Integer getScorePinnedCount() {
        return scorePinnedCount;
    }

    public void setScorePinnedCount(Integer scorePinnedCount) {
        this.scorePinnedCount = scorePinnedCount;
    }

    public Integer getScoreRecentCount() {
        return scoreRecentCount;
    }

    public void setScoreRecentCount(Integer scoreRecentCount) {
        this.scoreRecentCount = scoreRecentCount;
    }

    public Statistics getStatistics() {
        return statistics;
    }

    public void setStatistics(Statistics statistics) {
        this.statistics = statistics;
    }

    public Integer getSupportLevel() {
        return supportLevel;
    }

    public void setSupportLevel(Integer supportLevel) {
        this.supportLevel = supportLevel;
    }

    public List<UserAchievement> getUserAchievements() {
        return userAchievements;
    }

    public void setUserAchievements(List<UserAchievement> userAchievements) {
        this.userAchievements = userAchievements;
    }

    public RankHistory getRankHistory() {
        return rankHistory;
    }

    public void setRankHistory(RankHistory rankHistory) {
        this.rankHistory = rankHistory;
    }

    public Double getPP() {
        if (PP == null && statistics != null) {
            PP = statistics.getPP();
        }
        return PP;
    }

    public void setPP(Double PP) {
        this.PP = PP;
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

    public Boolean getRestricted() {
        return isRestricted;
    }

    public void setRestricted(Boolean restricted) {
        isRestricted = restricted;
    }

    public void setPmFriendsOnly(Boolean pmFriendsOnly) {
        this.pmFriendsOnly = pmFriendsOnly;
    }

    @Override
    public String toString() {
        return STR."OsuUser{avatarUrl='\{avatarUrl}\{'\''}, countryCode='\{countryCode}\{'\''}, defaultGroup='\{defaultGroup}\{'\''}, id=\{id}, isActive=\{isActive}, isBot=\{isBot}, isDeleted=\{isDeleted}, isOnline=\{isOnline}, isSupporter=\{isSupporter}, lastVisit=\{lastVisit}, PMFriendsOnly=\{pmFriendsOnly}, profileColor='\{profileColor}\{'\''}, username='\{username}\{'\''}, coverUrl='\{coverUrl}\{'\''}, discord='\{discord}\{'\''}, hasSupported=\{hasSupported}, interests='\{interests}\{'\''}, joinDate=\{joinDate}, location='\{location}\{'\''}, maxBlocks=\{maxBlocks}, maxFriends=\{maxFriends}, occupation='\{occupation}\{'\''}, playMode='\{playMode}\{'\''}, playStyle=\{playStyle}, postCount=\{postCount}, profileOrder=\{profileOrder}, title='\{title}\{'\''}, titleUrl='\{titleUrl}\{'\''}, twitter='\{twitter}\{'\''}, website='\{website}\{'\''}, country=\{country}, cover=\{cover}, kudosu=\{kudosu}, accountHistory=\{accountHistory}, profileBanner=\{profileBanner}, profileBanners=\{profileBanners}, badges=\{badges}, beatmapPlaycount=\{beatmapPlaycount}, CommentsCount=\{CommentsCount}, favoriteCount=\{favoriteCount}, followerCount=\{followerCount}, graveyardCount=\{graveyardCount}, groups=\{groups}, guestCount=\{guestCount}, lovedCount=\{lovedCount}, mappingFollowerCount=\{mappingFollowerCount}, monthlyPlaycounts=\{monthlyPlaycounts}, nominatedCount=\{nominatedCount}, page=\{page}, pendingCount=\{pendingCount}, previousNames=\{previousNames}, highestRank=\{highestRank}, rankedCount=\{rankedCount}, replaysWatchedCounts=\{replaysWatchedCounts}, scoreBestCount=\{scoreBestCount}, scoreFirstCount=\{scoreFirstCount}, scorePinnedCount=\{scorePinnedCount}, scoreRecentCount=\{scoreRecentCount}, statistics=\{statistics}, supportLevel=\{supportLevel}, userAchievements=\{userAchievements}, rankHistory=\{rankHistory}, PP=\{PP}\{'}'}";
    }

    public String toCSV() {
        return STR."\{username},\{id},\{avatarUrl},\{countryCode},\{defaultGroup},\{isActive},\{isBot},\{isDeleted},\{isOnline},\{isSupporter},\{isRestricted},\{lastVisit},\{pmFriendsOnly},\{profileColor},\{coverUrl},\{discord},\{hasSupported},\{interests},\{joinDate},\{location},\{maxBlocks},\{maxFriends},\{occupation},\{playMode},\{playStyle},\{postCount},\{profileOrder},\{title},\{titleUrl},\{twitter},\{website},\{country},\{cover},\{kudosu},\{accountHistory},\{profileBanner},\{profileBanners},\{badges},\{beatmapPlaycount},\{CommentsCount},\{favoriteCount},\{followerCount},\{graveyardCount},\{groups},\{guestCount},\{lovedCount},\{mappingFollowerCount},\{monthlyPlaycounts},\{nominatedCount},\{page},\{pendingCount},\{previousNames},\{highestRank},\{rankedCount},\{replaysWatchedCounts},\{scoreBestCount},\{scoreFirstCount},\{scorePinnedCount},\{scoreRecentCount},\{statistics},\{supportLevel},\{userAchievements},\{rankHistory},\{PP}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OsuUser osuUser)) return false;

        return id.equals(osuUser.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    /**
     * List<OsuUser> 去重方法
     * @param to 要合并进去 List
     * @param from 要用来合并的 List
     * @return 合并好的 List
     */

    public static List<OsuUser> merge2OsuUserList(@Nullable List<OsuUser> to, @Nullable List<OsuUser> from) {
        if (CollectionUtils.isEmpty(to)) {
            return from;
        }

        if (CollectionUtils.isEmpty(from)) {
            return to;
        }

        var toSet = new HashSet<>(to);
        var fromSet = new HashSet<>(from);

        if (! (toSet.containsAll(fromSet) || CollectionUtils.isEmpty(fromSet))) {
            toSet.addAll(fromSet);
            return new ArrayList<>(toSet);
        }

        return to;
    }
}
