package com.now.nowbot.model.JsonData;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.lang.Nullable;

import java.util.Comparator;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Search {
    @JsonProperty("rule")
    String rule;
    @JsonProperty("result_count")
    int result_count;
    @JsonProperty("beatmapsets")
    List<BeatMapSet> beatmapsets;
    @JsonProperty("total")
    Integer          total;
    @JsonProperty("cursor_string")
    String           cursorString;
    @JsonProperty("cursor")
    SearchCursor cursor;
    @JsonProperty("search")
    SearchInfo searchInfo;

    public List<BeatMapSet> getBeatmapsets() {
        return beatmapsets;
    }

    public void sortBeatmapDiff() {
        beatmapsets.forEach(set -> set.getBeatMaps().sort(Comparator.comparing(BeatMap::getDifficultyRating)));
    }

    public void setBeatmapsets(List<BeatMapSet> beatmapsets) {
        this.beatmapsets = beatmapsets;
    }

    public Integer getTotal() {
        return total;
    }

    public void setTotal(Integer total) {
        this.total = total;
    }

    public String getCursorString() {
        return cursorString;
    }

    public void setCursorString(String cursorString) {
        this.cursorString = cursorString;
    }

    public String getRule() {
        return rule;
    }

    public void setRule(String rule) {
        this.rule = rule;
    }
    public int getResultCount() {
        return result_count;
    }

    public void setResultCount(int result_count) {
        this.result_count = result_count;
    }

    public SearchCursor getCursor() {
        return cursor;
    }

    public void setCursor(SearchCursor cursor) {
        this.cursor = cursor;
    }
    @Nullable
    public SearchInfo getSearchInfo() {
        return searchInfo;
    }

    public void setSearchInfo(SearchInfo searchInfo) {
        this.searchInfo = searchInfo;
    }
}
