package com.now.nowbot.model.multiplayer;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.now.nowbot.model.JsonData.MicroUser;

import java.util.List;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Match {

    @JsonProperty("match")
    MatchStat matchStat;
    List<MatchEvent> events;
    @JsonProperty("users")
    List<MicroUser> players;
    @JsonProperty("first_event_id")
    Long firstEventId;
    @JsonProperty("latest_event_id")
    Long latestEventId;
    @JsonProperty("current_game_id")
    Long currentGameId;

    @JsonIgnoreProperties
    // 这是啥, 为什么要忽略
    boolean isMatchEnd;

    public MatchStat getMatchStat() {
        return matchStat;
    }

    public void setMatchStat(MatchStat matchStat) {
        this.matchStat = matchStat;
    }

    public List<MatchEvent> getEvents() {
        return events;
    }

    public void setEvents(List<MatchEvent> events) {
        this.events = events;
    }

    public List<MicroUser> getPlayers() {
        return players;
    }

    public void setPlayers(List<MicroUser> players) {
        this.players = players;
    }

    public Long getFirstEventId() {
        return firstEventId;
    }

    public void setFirstEventId(Long firstEventId) {
        this.firstEventId = firstEventId;
    }

    public Long getLatestEventId() {
        return latestEventId;
    }

    public void setLatestEventId(Long latestEventId) {
        this.latestEventId = latestEventId;
    }

    public Long getCurrentGameId() {
        return currentGameId;
    }

    public void setCurrentGameId(Long currentGameId) {
        this.currentGameId = currentGameId;
    }

    public boolean isMatchEnd() {
        this.isMatchEnd = (this.matchStat.getStartTime() != null && this.matchStat.getEndTime() != null);
        return isMatchEnd;
    }

    public void setMatchEnd(boolean matchEnd) {
        this.isMatchEnd = matchEnd;
    }

    public void parseNextData(Match m) {
        // 合并事件
        this.events.addAll(0, m.getEvents());
        this.players.addAll(m.getPlayers());
        //更新状态
        this.matchStat = m.getMatchStat();
        this.isMatchEnd = m.isMatchEnd();
        this.latestEventId = m.getLatestEventId();
        this.firstEventId = m.getFirstEventId();
    }
}
