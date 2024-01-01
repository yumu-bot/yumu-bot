package com.now.nowbot.model.multiplayer;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.now.nowbot.model.JsonData.MicroUser;
import org.springframework.util.CollectionUtils;

import java.util.List;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Match implements Cloneable {

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
    // 这玩意是自己算的
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

    @Override
    public Match clone() {
        try {
            return (Match) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    public void parseNextData(Match m) {
        if (CollectionUtils.isEmpty(m.getEvents())) return;
        // 合并事件
        if (events.getFirst().getId() > m.getEvents().getLast().getId()) {
            this.events.addAll(0, m.getEvents());
        } else if (events.getLast().getId() < m.getEvents().getFirst().getId()){
            this.events.addAll(m.getEvents());
        } else if (events.getFirst().getId() > m.getEvents().getFirst().getId()) {
            events.removeIf(e -> e.getId() <= m.getEvents().getLast().getId());
            this.events.addAll(0, m.getEvents());
        } else if (events.getLast().getId() < m.getEvents().getLast().getId()){
            events.removeIf(e -> e.getId() >= m.getEvents().getFirst().getId());
            this.events.addAll(m.getEvents());
        }

        this.players.addAll(m.getPlayers());
        //更新状态
        this.matchStat = m.getMatchStat();
        this.isMatchEnd = m.isMatchEnd();
        this.latestEventId = m.getLatestEventId();
        this.firstEventId = m.getFirstEventId();
    }
}
