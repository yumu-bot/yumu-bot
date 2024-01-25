package com.now.nowbot.model.JsonData;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.now.nowbot.util.JacksonUtil;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;

import java.util.*;
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
        if (!data.hasNonNull("beatmapsets") || !data.get("beatmapsets").isArray()) return;
        beatMapSet = JacksonUtil.parseObject(data.get("beatmapsets").get(0), BeatMapSet.class);
    }

    List<DiscussionDetails> discussions;

    @JsonProperty("included_discussions")
    @Nullable
    List<DiscussionDetails> includedDiscussions;

    public record ReviewsConfig(@JsonProperty("max_blocks") Integer maxBlocks) {
    }

    @JsonProperty("reviews_config")
    ReviewsConfig reviewsConfig;

    List<OsuUser> users;

    public record Cursor(Integer page, Integer limit) {
    }

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

    public List<DiscussionDetails> getDiscussions() {
        return discussions;
    }

    public void setDiscussions(List<DiscussionDetails> discussions) {
        this.discussions = discussions;
    }

    @Nullable
    public List<DiscussionDetails> getIncludedDiscussions() {
        return includedDiscussions;
    }

    public void setIncludedDiscussions(@Nullable List<DiscussionDetails> includedDiscussions) {
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

    public void nextDiscussion(Discussion other, String sort) {
        // discussions, includedDiscussions合并
        if (Objects.equals("id_asc", sort)) {
            if (CollectionUtils.isEmpty(other.getDiscussions())) {
                if (CollectionUtils.isEmpty(this.getDiscussions()))
                    other.getDiscussions().addAll(this.getDiscussions());
                this.setDiscussions(other.getDiscussions());
            }

            if (CollectionUtils.isEmpty(other.getIncludedDiscussions())) {
                if (CollectionUtils.isEmpty(this.getIncludedDiscussions()))
                    other.getIncludedDiscussions().addAll(this.getIncludedDiscussions());
                this.setIncludedDiscussions(other.getIncludedDiscussions());
            }
        } else {
            if (CollectionUtils.isEmpty(other.getDiscussions())) {
                if (CollectionUtils.isEmpty(this.getDiscussions()))
                    this.getDiscussions().addAll(other.getDiscussions());
                else
                    this.setDiscussions(other.getDiscussions());
            }

            if (CollectionUtils.isEmpty(other.getIncludedDiscussions())) {
                if (CollectionUtils.isEmpty(this.getIncludedDiscussions()))
                    this.getIncludedDiscussions().addAll(other.getIncludedDiscussions());
                else
                    other.setIncludedDiscussions(this.getIncludedDiscussions());
            }

        }

        // user去重
        if (!this.getUsers().containsAll(other.getUsers())) {
            if (CollectionUtils.isEmpty(other.getUsers())) {
            } else if (CollectionUtils.isEmpty(this.getUsers())) {
                this.setUsers(other.getUsers());
            } else {
                var idSet1 = new HashSet<>(this.getUsers());
                var idSet2 = new HashSet<>(other.getUsers());
                idSet1.addAll(idSet2);
                this.setUsers(new ArrayList<>(idSet1));
            }
        }
    }
}
