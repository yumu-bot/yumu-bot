package com.now.nowbot.model.jsonData;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.*;
import java.util.stream.Collectors;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class BeatMapSet {
    String artist;

    @JsonProperty("artist_unicode")
    String artistUnicode;

    Covers covers;

    String creator;

    @JsonProperty("favourite_count")
    Integer favouriteCount;

    @Nullable
    Hype hype;

    public record Hype(Integer current, Integer required) {}


    @JsonProperty("id")
    Long SID;

    Boolean nsfw;

    Integer offset;

    @JsonProperty("play_count")
    Long playCount;

    @JsonProperty("preview_url")
    String previewUrl;

    String source;

    Boolean spotlight;

    String status;

    String title;

    @JsonProperty("title_unicode")
    String titleUnicode;

    @JsonProperty("track_id")
    Integer trackID;

    @JsonProperty("user_id")
    Long creatorID;

    Boolean video;

    @JsonProperty("bpm")
    Double BPM;

    @JsonProperty("can_be_hyped")
    Boolean canBeHyped;

    @JsonProperty("deleted_at")
    OffsetDateTime deletedAt;

    //已经弃用 Deprecated, all beatmapsets now have discussion enabled.
    //@JsonProperty("discussion_enabled")
    //Boolean discussionEnabled;

    @JsonProperty("discussion_locked")
    Boolean discussionLocked;

    @JsonProperty("is_scoreable")
    Boolean scoreable;

    @JsonProperty("last_updated")
    OffsetDateTime lastUpdated;

    @JsonProperty("legacy_thread_url")
    String legacyThreadUrl;

    @Nullable
    @JsonProperty("nominations_summary")
    NominationsSummary nominationsSummary;

    // https://osu.ppy.sh/home/changelog/web/2024.603.0 改了
    public record NominationsSummary (
            Integer current,

            // 只有一个元素的列表，存储模式信息
            @JsonProperty("eligible_main_rulesets")
            List<String> mode,

            @JsonProperty("required_meta")
            RequiredMeta required

    ) {}

    public record RequiredMeta(
            @JsonProperty("main_ruleset")
            Integer main,

            // 这个不准，需要重新计算并赋值。
            @JsonProperty("non_main_ruleset")
            Integer secondary

    ) {}

    Integer ranked;

    @JsonProperty("ranked_date")
    OffsetDateTime rankedDate;

    Boolean storyboard;

    @JsonProperty("submitted_date")
    OffsetDateTime submittedDate;

    String tags;

    @Nullable
    @JsonProperty("availability")
    Availability availability;

    public record Availability (
            @JsonProperty("download_disabled")
            Boolean downloadDisabled,

            @JsonProperty("more_information")
            String moreInformation
    ) {}

    @Nullable
    @JsonProperty("beatmaps")
    List<BeatMap> beatMaps;

    @Nullable
    List<BeatMap> converts;

    @Nullable
    @JsonProperty("current_nominations")
    List<CurrentNominations> currentNominations;

    public record CurrentNominations (
            @JsonProperty("beatmapset_id")
            Long SID,

            @JsonProperty("rulesets")
            List<String> mode,

            Boolean reset,

            @JsonProperty("user_id")
            Long UID
    ) {}
    Description description;

    public record Description (
            String description
    ) {}

    @Nullable
    Genre genre;

    public record Genre (Integer id, String name) {}

    @Nullable
    Language language;

    public record Language (Integer id, String name) {}

    @Nullable
    @JsonProperty("pack_tags")
    List<String> packTags;

    List<Integer> ratings;

    @Nullable
    @JsonProperty("recent_favourites")
    List<OsuUser> recentFavourites;

    @Nullable
    @JsonProperty("related_users")
    List<OsuUser> relatedUsers;

    @Nullable
    @JsonProperty("user")
    OsuUser creatorData;

    //自己算
    List<OsuUser> mappers = new ArrayList<>();

    //自己算
    List<OsuUser> nominators = new ArrayList<>();

    //自己算
    Double publicRating;

    //自己算
    Boolean hasLeaderBoard;

    Boolean fromDatabase;

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getArtistUnicode() {
        return artistUnicode;
    }

    public void setArtistUnicode(String artistUnicode) {
        this.artistUnicode = artistUnicode;
    }

    public Covers getCovers() {
        return covers;
    }

    public void setCovers(Covers covers) {
        this.covers = covers;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public Integer getFavouriteCount() {
        return favouriteCount;
    }

    public void setFavouriteCount(Integer favouriteCount) {
        this.favouriteCount = favouriteCount;
    }

    @Nullable
    public Hype getHype() {
        return hype;
    }

    public void setHype(@Nullable Hype hype) {
        this.hype = hype;
    }

    public Long getSID() {
        return SID;
    }

    public void setSID(Long SID) {
        this.SID = SID;
    }

    public Boolean getNsfw() {
        return nsfw;
    }

    public void setNsfw(Boolean nsfw) {
        this.nsfw = nsfw;
    }

    public Integer getOffset() {
        return offset;
    }

    public void setOffset(Integer offset) {
        this.offset = offset;
    }

    public Long getPlayCount() {
        return playCount;
    }

    public void setPlayCount(Long playCount) {
        this.playCount = playCount;
    }

    public String getPreviewUrl() {
        return previewUrl;
    }

    public void setPreviewUrl(String previewUrl) {
        this.previewUrl = previewUrl;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Boolean getSpotlight() {
        return spotlight;
    }

    public void setSpotlight(Boolean spotlight) {
        this.spotlight = spotlight;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitleUnicode() {
        return titleUnicode;
    }

    public void setTitleUnicode(String titleUnicode) {
        this.titleUnicode = titleUnicode;
    }

    public Integer getTrackID() {
        return trackID;
    }

    public void setTrackID(Integer trackID) {
        this.trackID = trackID;
    }

    public Long getCreatorID() {
        return creatorID;
    }

    public void setCreatorID(Long creatorID) {
        this.creatorID = creatorID;
    }

    public Boolean getVideo() {
        return video;
    }

    public void setVideo(Boolean video) {
        this.video = video;
    }

    public Double getBPM() {
        return BPM;
    }

    public void setBPM(Double BPM) {
        this.BPM = BPM;
    }

    public Boolean getCanBeHyped() {
        return canBeHyped;
    }

    public void setCanBeHyped(Boolean canBeHyped) {
        this.canBeHyped = canBeHyped;
    }

    public OffsetDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(OffsetDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    public Boolean getDiscussionLocked() {
        return discussionLocked;
    }

    public void setDiscussionLocked(Boolean discussionLocked) {
        this.discussionLocked = discussionLocked;
    }

    public Boolean getScoreable() {
        return scoreable;
    }

    public void setScoreable(Boolean scoreable) {
        this.scoreable = scoreable;
    }

    public OffsetDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(OffsetDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public String getLegacyThreadUrl() {
        return legacyThreadUrl;
    }

    public void setLegacyThreadUrl(String legacyThreadUrl) {
        this.legacyThreadUrl = legacyThreadUrl;
    }

    // 这个不准，需要重新计算并赋值。
    @Nullable
    public NominationsSummary getNominationsSummary() {
        var s = nominationsSummary;
        if (s == null) return null;

        var r = s.required;

        int secondary;

        if (CollectionUtils.isEmpty(beatMaps)) {
            secondary = 0;
        } else {

            final DateTimeFormatter formatter = new DateTimeFormatterBuilder()
                    .appendPattern("yyyy-MM-dd")
                    .appendLiteral("T")
                    .appendPattern("HH:mm:ss")
                    .appendZoneId().toFormatter();

            final LocalDateTime changedTime = LocalDateTime.from(formatter.parse("2024-06-03T00:00:00Z"));

            // 没榜，或者最后更新时间晚于这一天的谱面才应用这次更改
            if (this.getLastUpdated().toLocalDateTime().isAfter(changedTime) || ! this.hasLeaderBoard()) {

                secondary = Math.max(
                        beatMaps.stream().map(BeatMap::getModeInt).collect(Collectors.toSet()).size() - 1
                        , 0);
            } else {
                // 之前的，其他模式要 x2
                secondary = Math.max(
                        (beatMaps.stream().map(BeatMap::getModeInt).collect(Collectors.toSet()).size() - 1) * 2
                        , 0);
            }
        }

        this.nominationsSummary = new NominationsSummary(s.current, s.mode, new RequiredMeta(r.main, secondary));

        return this.nominationsSummary;
    }

    public void setNominationsSummary(@Nullable NominationsSummary nominationsSummary) {
        this.nominationsSummary = nominationsSummary;
    }

    public Integer getRanked() {
        return ranked;
    }

    public void setRanked(Integer ranked) {
        this.ranked = ranked;
    }

    public OffsetDateTime getRankedDate() {
        return rankedDate;
    }

    public void setRankedDate(OffsetDateTime rankedDate) {
        this.rankedDate = rankedDate;
    }

    public Boolean getStoryboard() {
        return storyboard;
    }

    public void setStoryboard(Boolean storyboard) {
        this.storyboard = storyboard;
    }

    public OffsetDateTime getSubmittedDate() {
        return submittedDate;
    }

    public void setSubmittedDate(OffsetDateTime submittedDate) {
        this.submittedDate = submittedDate;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    @Nullable
    public Availability getAvailability() {
        return availability;
    }

    public void setAvailability(@Nullable Availability availability) {
        this.availability = availability;
    }

    @Nullable
    public List<BeatMap> getBeatMaps() {
        return beatMaps;
    }

    public void setBeatMaps(@Nullable List<BeatMap> beatMaps) {
        this.beatMaps = beatMaps;
    }

    //获取最高难度
    public @Nullable BeatMap getTopDiff() {
        if (CollectionUtils.isEmpty(beatMaps)) return null;
        if (beatMaps.size() == 1) return beatMaps.getFirst();

        Comparator<BeatMap> starComparator = (o1, o2) -> (int) (o1.starRating * 100f - o2.starRating * 100f);

        return Collections.max(beatMaps, starComparator);

    }

    @Nullable
    public List<BeatMap> getConverts() {
        return converts;
    }

    public void setConverts(@Nullable List<BeatMap> converts) {
        this.converts = converts;
    }

    @Nullable
    public List<CurrentNominations> getCurrentNominations() {
        return currentNominations;
    }

    public void setCurrentNominations(@Nullable List<CurrentNominations> currentNominations) {
        this.currentNominations = currentNominations;
    }

    public Description getDescription() {
        return description;
    }

    public void setDescription(Description description) {
        this.description = description;
    }

    @Nullable
    public Genre getGenre() {
        return genre;
    }

    public void setGenre(@Nullable Genre genre) {
        this.genre = genre;
    }

    @Nullable
    public Language getLanguage() {
        return language;
    }

    public void setLanguage(@Nullable Language language) {
        this.language = language;
    }

    @Nullable
    public List<String> getPackTags() {
        return packTags;
    }

    public void setPackTags(@Nullable List<String> packTags) {
        this.packTags = packTags;
    }

    public List<Integer> getRatings() {
        return ratings;
    }

    public void setRatings(List<Integer> ratings) {
        this.ratings = ratings;
    }

    @Nullable
    public List<OsuUser> getRecentFavourites() {
        return recentFavourites;
    }

    public void setRecentFavourites(@Nullable List<OsuUser> recentFavourites) {
        this.recentFavourites = recentFavourites;
    }

    @Nullable
    public List<OsuUser> getRelatedUsers() {
        return relatedUsers;
    }

    public void setRelatedUsers(@Nullable List<OsuUser> relatedUsers) {
        this.relatedUsers = relatedUsers;
    }

    @Nullable
    public OsuUser getCreatorData() {
        return creatorData;
    }

    public void setCreatorData(@Nullable OsuUser creatorData) {
        this.creatorData = creatorData;
    }

    public List<OsuUser> getMappers() {
        if (nominators.isEmpty() && Objects.nonNull(currentNominations)) {
            nominators = this.getNominators();
        }

        if (mappers.isEmpty() && Objects.nonNull(relatedUsers) && Objects.nonNull(nominators)) {
            for (OsuUser u : relatedUsers) {
                if (!(nominators.contains(u) || Objects.equals(u.getUserID(), creatorID))) {
                    mappers.add(u);
                }
            }
        }

        return mappers;
    }

    public void setMappers(List<OsuUser> mappers) {
        this.mappers = mappers;
    }

    public List<OsuUser> getNominators() {
        if (nominators.isEmpty() && Objects.nonNull(currentNominations) && Objects.nonNull(relatedUsers)) {
            for (CurrentNominations c : currentNominations) {
                for (OsuUser u : relatedUsers) {
                    if (Objects.equals(u.getUserID(), c.UID)) {
                        nominators.add(u);
                        break;
                    }
                }
            }
        }

        return nominators;
    }

    public void setNominators(List<OsuUser> nominators) {
        this.nominators = nominators;
    }

    public double getPublicRating() {
        if (Objects.isNull(ratings)) {
            publicRating = 0D;
            return publicRating;
        }

        double r = 0;
        double sum;

        sum = ratings.stream().reduce(Integer::sum).orElse(0);

        if (sum == 0D) {
            publicRating = 0D;
            return publicRating;
        }

        for (int j = 0; j <= 10; j++) {
            r += (j * ratings.get(j) / sum);
        }

        publicRating = r;
        return publicRating;
    }

    public void setPublicRating(Double publicRating) {
        this.publicRating = publicRating;
    }

    public boolean hasLeaderBoard() {
        if (Objects.nonNull(status)) {
            hasLeaderBoard = (Objects.equals(status, "ranked") || Objects.equals(status, "qualified") || Objects.equals(status, "loved") || Objects.equals(status, "approved"));
        } else {
            switch (ranked) {
                case 1, 2, 3, 4 -> hasLeaderBoard = true;
                case null, default -> hasLeaderBoard = false;
            }
        }
        return hasLeaderBoard;
    }

    public void setHasLeaderBoard(Boolean hasLeaderBoard) {
        this.hasLeaderBoard = hasLeaderBoard;
    }

    public Boolean getFromDatabase() {
        return fromDatabase;
    }

    public void setFromDatabase(Boolean fromDatabase) {
        this.fromDatabase = fromDatabase;
    }

    @Override
    public String toString() {
        return STR."BeatMapSet{artist='\{artist}\{'\''}, artistUnicode='\{artistUnicode}\{'\''}, covers=\{covers}, creator='\{creator}\{'\''}, favouriteCount=\{favouriteCount}, hype=\{hype}, SID=\{SID}, nsfw=\{nsfw}, offset=\{offset}, playCount=\{playCount}, previewUrl='\{previewUrl}\{'\''}, source='\{source}\{'\''}, spotlight=\{spotlight}, status='\{status}\{'\''}, title='\{title}\{'\''}, titleUnicode='\{titleUnicode}\{'\''}, trackID=\{trackID}, creatorID=\{creatorID}, video=\{video}, BPM=\{BPM}, canBeHyped=\{canBeHyped}, deletedAt=\{deletedAt}, discussionLocked=\{discussionLocked}, scoreable=\{scoreable}, lastUpdated=\{lastUpdated}, legacyThreadUrl='\{legacyThreadUrl}\{'\''}, nominationsSummary=\{nominationsSummary}, ranked=\{ranked}, rankedDate=\{rankedDate}, storyboard=\{storyboard}, submittedDate=\{submittedDate}, tags='\{tags}\{'\''}, availability=\{availability}, beatMaps=\{beatMaps}, converts=\{converts}, currentNominations=\{currentNominations}, description=\{description}, genre=\{genre}, language=\{language}, packTags=\{packTags}, ratings=\{ratings}, recentFavourites=\{recentFavourites}, relatedUsers=\{relatedUsers}, creatorData=\{creatorData}, mappers=\{mappers}, nominators=\{nominators}, publicRating=\{publicRating}, hasLeaderBoard=\{hasLeaderBoard}, fromDatabase=\{fromDatabase}\{'}'}";
    }
}
