package com.now.nowbot.model.json;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.now.nowbot.util.JacksonUtil;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Stream;

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

    public void mergeDiscussion(Discussion that, String sort) {
        this.setCursorString(that.getCursorString());
        this.setCursor(that.getCursor());
        // discussions, includedDiscussions合并
        if (Objects.equals("id_asc", sort)) {

            if (! CollectionUtils.isEmpty(that.getDiscussions())) {
                if (! CollectionUtils.isEmpty(this.getDiscussions())) {
                    that.getDiscussions().addAll(this.getDiscussions());
                } else {
                    this.setDiscussions(that.getDiscussions());
                }
            }

            if (! CollectionUtils.isEmpty(that.getIncludedDiscussions())) {
                if (! CollectionUtils.isEmpty(this.getIncludedDiscussions())) {
                    that.getIncludedDiscussions().addAll(this.getIncludedDiscussions());
                } else {
                    this.setIncludedDiscussions(that.getIncludedDiscussions());
                }
            }

        } else {

            if (! CollectionUtils.isEmpty(that.getDiscussions())) {
                if (! CollectionUtils.isEmpty(this.getDiscussions())) {
                    this.getDiscussions().addAll(that.getDiscussions());
                } else {
                    this.setDiscussions(that.getDiscussions());
                }
            }

            if (! CollectionUtils.isEmpty(that.getIncludedDiscussions())) {
                if (! CollectionUtils.isEmpty(this.getIncludedDiscussions())) {
                    this.getIncludedDiscussions().addAll(that.getIncludedDiscussions());
                } else {
                    this.setIncludedDiscussions(that.getIncludedDiscussions());
                }
            }
        }

        // user去重
        this.setUsers(
                OsuUser.merge2OsuUserList(this.getUsers(), that.getUsers())
        );

    }

    /**
     * 置顶未解决的讨论
     */
    public static List<DiscussionDetails> toppingUnsolvedDiscussionDetails(List<DiscussionDetails> discussions) {
        List<DiscussionDetails> u = new ArrayList<>();
        List<DiscussionDetails> s = new ArrayList<>();

        for (DiscussionDetails d : discussions) {
            var c = d.getCanBeResolved();
            var r = d.getResolved();

            if (c && !r) {
                u.add(d);
            } else {
                s.add(d);
            }
        }
        return Stream.of(u, s).flatMap(Collection::stream).distinct().toList();
    }

    /**
     * 把谱面难度名字嵌入到 DiscussionDetails 里
     * @param diffs 难度 bid 和难度名的 map
     */
    public void addDifficulty4DiscussionDetails(Map<Long, String> diffs) {
        this.setDiscussions(
                this.getDiscussions().stream()
                        .peek(d -> d.setDifficulty(
                                diffs.get(d.getBID())
                        )).toList()
        );
    }
}
