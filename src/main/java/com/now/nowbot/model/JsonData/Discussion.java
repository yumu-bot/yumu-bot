package com.now.nowbot.model.JsonData;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.now.nowbot.util.JacksonUtil;
import org.springframework.lang.Nullable;

import java.util.List;
import java.util.stream.StreamSupport;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Discussion {
    @JsonProperty("beatmaps")
    @Nullable
    List<BeatMap> beatMaps;

    @JsonIgnore
    BeatMapSet beatMapSet;

    @JsonProperty("beatmapsets")
    public void parseBeatMapSet(JsonNode data) {

        if (data.hasNonNull("beatmapsets") && data.get("beatmapsets").isArray()) {
            JsonNode n = StreamSupport.stream(data.get("beatmapsets").spliterator(), false).toList().getFirst();

            beatMapSet = JacksonUtil.parseObject(n, BeatMapSet.class);
        }
    }

    List<BeatMapSetDiscussion> discussions;

    @JsonProperty("included_discussions")
    @Nullable
    List<BeatMapSetDiscussion> includedDiscussions;

    public record ReviewsConfig(@JsonProperty("max_blocks") Integer maxBlocks) {}

    @JsonProperty("reviews_config")
    ReviewsConfig reviewsConfig;

    List<OsuUser> users;

    public record Cursor(Integer page, Integer limit) {}

    @JsonProperty("cursor")
    @Nullable
    Cursor cursor;

    @JsonProperty("cursor_string")
    @Nullable
    String cursorString;

    @Nullable
    public List<BeatMap> getBeatMaps() {
        return beatMaps;
    }

    public void setBeatMaps(@Nullable List<BeatMap> beatMaps) {
        this.beatMaps = beatMaps;
    }

    public BeatMapSet getBeatMapSet() {
        return beatMapSet;
    }

    public void setBeatMapSet(BeatMapSet beatMapSet) {
        this.beatMapSet = beatMapSet;
    }

    public List<BeatMapSetDiscussion> getDiscussions() {
        return discussions;
    }

    public void setDiscussions(List<BeatMapSetDiscussion> discussions) {
        this.discussions = discussions;
    }

    @Nullable
    public List<BeatMapSetDiscussion> getIncludedDiscussions() {
        return includedDiscussions;
    }

    public void setIncludedDiscussions(@Nullable List<BeatMapSetDiscussion> includedDiscussions) {
        this.includedDiscussions = includedDiscussions;
    }

    public ReviewsConfig getReviewsConfig() {
        return reviewsConfig;
    }

    public void setReviewsConfig(ReviewsConfig reviewsConfig) {
        this.reviewsConfig = reviewsConfig;
    }

    public List<OsuUser> getUsers() {
        return users;
    }

    public void setUsers(List<OsuUser> users) {
        this.users = users;
    }

    @Nullable
    public Cursor getCursor() {
        return cursor;
    }

    public void setCursor(@Nullable Cursor cursor) {
        this.cursor = cursor;
    }

    @Nullable
    public String getCursorString() {
        return cursorString;
    }

    public void setCursorString(@Nullable String cursorString) {
        this.cursorString = cursorString;
    }
}
