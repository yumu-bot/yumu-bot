package com.now.nowbot.model.match;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.now.nowbot.model.JsonData.MicroUser;

import java.util.HashSet;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Match {
    @JsonProperty("match")
    MatchInfo matchInfo;
    //    @JsonIgnore
    List<MatchEvent> events;
    //    @JsonProperty("events")
//    public List<JsonNode> eventsrr;
//    @JsonIgnore
    List<MicroUser> users;
//    @JsonProperty("users")
//    public List<JsonNode> jsonNodes;
    @JsonProperty("first_event_id")
    Long firstEventId;
    @JsonProperty("latest_event_id")
    Long latestEventId;
    @JsonProperty("current_game_id")
    Long currentGameId;

    public MatchInfo getMatchInfo() {
        return matchInfo;
    }

    public void setMatchInfo(MatchInfo matchInfo) {
        this.matchInfo = matchInfo;
    }

    public List<MatchEvent> getEvents() {
        return events;
    }

    public void setEvents(List<MatchEvent> events) {
        this.events = events;
    }

    public List<MicroUser> getUsers() {
        return users;
    }

    public void setUsers(List<MicroUser> users) {
        this.users = users;
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

    public void parseNextData(Match m) {
        var nList = m.getEvents();
        var nameSet = new HashSet<>(m.getUsers().stream().map(MicroUser::getId).toList());
        for (var mu : this.getUsers()){
            nameSet.remove(mu.getId());
        }
        getUsers().addAll(m.getUsers().stream().filter(u->nameSet.contains(u.getId())).toList());
        this.getEvents().addAll(0, nList);
    }
}
