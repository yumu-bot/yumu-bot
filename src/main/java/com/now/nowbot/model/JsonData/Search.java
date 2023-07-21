package com.now.nowbot.model.JsonData;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Search {
    @JsonProperty("rule")
    String rule;

    @JsonProperty("beatmapsets")
    List<BeatMapSet> beatmapsets;
    @JsonProperty("total")
    Integer          total;
    @JsonProperty("cursor_string")
    String           cursorString;
    @JsonProperty("cursor")
    SearchCursor cursor;

    public List<BeatMapSet> getBeatmapsets() {
        return beatmapsets;
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

    public SearchCursor getCursor() {
        return cursor;
    }

    public void setCursor(SearchCursor cursor) {
        this.cursor = cursor;
    }
}
