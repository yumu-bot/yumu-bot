package com.now.nowbot.model.json;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.now.nowbot.model.enums.OsuMod;
import org.jetbrains.annotations.NotNull;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;

import java.time.OffsetDateTime;
import java.util.*;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Match implements Cloneable {
    @JsonProperty("match")
    MatchStat matchStat;

    @JsonProperty("events")
    List<MatchEvent> events;

    @JsonProperty("users")
    List<MicroUser> players;

    @JsonProperty("first_event_id")
    Long firstEventID;

    @JsonProperty("latest_event_id")
    Long latestEventID;

    @JsonProperty("current_game_id")
    Long currentGameID;

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

    public Long getFirstEventID() {
        return firstEventID;
    }

    public void setFirstEventID(Long firstEventID) {
        this.firstEventID = firstEventID;
    }

    public Long getLatestEventID() {
        return latestEventID;
    }

    public void setLatestEventID(Long latestEventID) {
        this.latestEventID = latestEventID;
    }

    public Long getCurrentGameID() {
        return currentGameID;
    }

    public void setCurrentGameID(Long currentGameID) {
        this.currentGameID = currentGameID;
    }

    @Override
    public Match clone() {
        try {
            return (Match) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    @Override
    public String toString() {
        return STR."Match{matchStat=\{matchStat}, events=\{events}, players=\{players}, firstEventId=\{firstEventID}, latestEventId=\{latestEventID}, currentGameId=\{currentGameID}\{'}'}";
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MatchStat {
        @JsonProperty("id")
        Long matchID;

        @JsonProperty("start_time")
        OffsetDateTime startTime;

        @JsonProperty("end_time")
        OffsetDateTime endTime;

        String name;

        public Long getMatchID() {
            return matchID;
        }

        public void setMatchID(Long matchID) {
            this.matchID = matchID;
        }

        public OffsetDateTime getStartTime() {
            return startTime;
        }

        public void setStartTime(OffsetDateTime startTime) {
            this.startTime = startTime;
        }

        public OffsetDateTime getEndTime() {
            return endTime;
        }

        public void setEndTime(OffsetDateTime endTime) {
            this.endTime = endTime;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return STR."MatchStat{matchID=\{matchID}, startTime=\{startTime}, endTime=\{endTime}, name='\{name}\{'\''}\{'}'}";
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MatchEvent implements Comparable<MatchEvent> {

        public record Detail(String type, String text) {}

        @JsonProperty("id")
        Long eventID;

        @JsonProperty("detail")
        Detail detail;

        OffsetDateTime timestamp;

        @JsonProperty("user_id")
        Long userID;

        @JsonProperty("game")
        MatchRound round;

        public Long getEventID() {
            return eventID;
        }

        public void setEventID(Long eventID) {
            this.eventID = eventID;
        }

        public Detail getDetail() {
            return detail;
        }

        public void setDetail(Detail detail) {
            this.detail = detail;
        }

        public OffsetDateTime getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(OffsetDateTime timestamp) {
            this.timestamp = timestamp;
        }

        public Long getUserID() {
            return userID;
        }

        public void setUserID(Long userID) {
            this.userID = userID;
        }

        public MatchRound getRound() {
            return round;
        }

        public void setRound(MatchRound round) {
            this.round = round;
        }

        @Override
        public String toString() {
            return STR."MatchEvent{eventID=\{eventID}, detail=\{detail}, timestamp=\{timestamp}, userID=\{userID}, round=\{round}\{'}'}";
        }

        @Override
        public int compareTo(@NotNull MatchEvent o) {
            return Math.toIntExact(o.eventID - eventID);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MatchRound {
        @JsonProperty("id")
        Integer roundID;

        @JsonProperty("beatmap_id")
        Long beatMapID;

        @JsonProperty("start_time")
        OffsetDateTime startTime;

        @JsonProperty("end_time")
        OffsetDateTime endTime;

        String mode;

        @Nullable
        @JsonProperty("mod_int")
        Integer modInt;

        @JsonProperty("scoring_type")
        String scoringType;

        @JsonProperty("team_type")
        String teamType;

        List<String> mods;

        @Nullable
        @JsonProperty("beatmap")
        BeatMap beatMap;

        @JsonProperty("scores")
        List<MatchScore> scores;

        public Long getBeatMapID() {
            return beatMapID;
        }

        public void setBeatMapID(Long beatMapID) {
            this.beatMapID = beatMapID;
        }

        public Integer getRoundID() {
            return roundID;
        }

        public void setRoundID(Integer roundID) {
            this.roundID = roundID;
        }

        public OffsetDateTime getStartTime() {
            return startTime;
        }

        public void setStartTime(OffsetDateTime startTime) {
            this.startTime = startTime;
        }

        public OffsetDateTime getEndTime() {
            return endTime;
        }

        public void setEndTime(OffsetDateTime endTime) {
            this.endTime = endTime;
        }

        public String getMode() {
            return mode;
        }

        public void setMode(String mode) {
            this.mode = mode;
        }

        public Integer getModInt() {
            return Optional.ofNullable(modInt).orElse(OsuMod.getModsValueFromAbbrList(this.mods));
        }

        public void setModInt(@Nullable Integer modInt) {
            this.modInt = modInt;
        }

        public String getScoringType() {
            return scoringType;
        }

        public void setScoringType(String scoringType) {
            this.scoringType = scoringType;
        }

        public String getTeamType() {
            return teamType;
        }

        public void setTeamType(String teamType) {
            this.teamType = teamType;
        }

        public List<String> getMods() {
            return mods;
        }

        public void setMods(List<String> mods) {
            this.mods = mods;
        }

        @Nullable
        public BeatMap getBeatMap() {
            return beatMap;
        }

        public void setBeatMap(@Nullable BeatMap beatMap) {
            this.beatMap = beatMap;
        }

        public List<MatchScore> getScores() {
            return scores;
        }

        public void setScores(List<MatchScore> scores) {
            this.scores = scores;
        }

        public record TeamScore(Long total, Long red, Long blue) {}

        public TeamScore getTeamScore() {
            long total = 0L, red = 0L, blue = 0L;

            if (Objects.equals(teamType, "team-vs")) {
                for (MatchScore s : scores) {

                    switch (s.getPlayerStat().team()) {
                        case "red" -> red += s.getScore();
                        case "blue" -> blue += s.getScore();
                    }
                }

                total = red + blue;

            } else {
                for (MatchScore s : scores) {
                    total += s.getScore();
                }
            }

            return new TeamScore(total, red, blue);
        }

        @Nullable
        public String getWinningTeam() {
            if (! Objects.equals(this.getTeamType(), "team-vs")) return "none";
            var ts = getTeamScore();

            if (ts.red() > ts.blue()) return "red";
            else if (ts.red() < ts.blue()) return "blue";
            else return null; //平局
        }

        @NonNull
        public Long getWinningTeamScore() {
            if (Objects.equals(teamType, "team-vs")) {
                var ts = getTeamScore();
                return Math.max(ts.red(), ts.blue());
            } else {
                return this.getScores().stream().mapToLong(MatchScore::getScore).reduce(Long::max).orElse(0L);
            }
        }

        @Override
        public String toString() {
            return STR."MatchRound{beatMapID=\{beatMapID}, roundID=\{roundID}, startTime=\{startTime}, endTime=\{endTime}, mode='\{mode}\{'\''}, modInt=\{modInt}, scoringType='\{scoringType}\{'\''}, teamType='\{teamType}\{'\''}, mods=\{mods}, beatMap=\{beatMap}, scores=\{scores}\{'}'}";
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MatchScore {
        public record PlayerStat(Integer slot, String team, Boolean pass) {}

        Double accuracy;

        @JsonProperty("best_id")
        Long bestID;

        OffsetDateTime timestamp;

        @JsonProperty("id")
        Long scoreID;

        @JsonProperty("max_combo")
        Integer maxCombo;

        String mode;

        @JsonProperty("mode_int")
        Integer modeInt;

        String[] mods;
        Boolean passed;
        Boolean perfect;
        Double pp;
        String rank;
        Boolean replay;
        Integer score;
        Statistics statistics;
        String type;

        @JsonProperty("user_id")
        Long userID;

        @JsonProperty("match")
        PlayerStat playerStat;

        // 自设
        Integer ranking;

        // 自设
        MicroUser user;

        public Double getAccuracy() {
            return accuracy;
        }

        public void setAccuracy(Double accuracy) {
            this.accuracy = accuracy;
        }

        public Long getBestID() {
            return bestID;
        }

        public void setBestID(Long bestID) {
            this.bestID = bestID;
        }

        public OffsetDateTime getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(OffsetDateTime timestamp) {
            this.timestamp = timestamp;
        }

        public Long getScoreID() {
            return scoreID;
        }

        public void setScoreID(Long scoreID) {
            this.scoreID = scoreID;
        }

        public Integer getMaxCombo() {
            return maxCombo;
        }

        public void setMaxCombo(Integer maxCombo) {
            this.maxCombo = maxCombo;
        }

        public String getMode() {
            return mode;
        }

        public void setMode(String mode) {
            this.mode = mode;
        }

        public Integer getModeInt() {
            return modeInt;
        }

        public void setModeInt(Integer modeInt) {
            this.modeInt = modeInt;
        }

        public String[] getMods() {
            return mods;
        }

        public void setMods(String[] mods) {
            this.mods = mods;
        }

        public Boolean getPassed() {
            return passed;
        }

        public void setPassed(Boolean passed) {
            this.passed = passed;
        }

        public Boolean getPerfect() {
            return perfect;
        }

        public void setPerfect(Boolean perfect) {
            this.perfect = perfect;
        }

        public Double getPp() {
            return pp;
        }

        public void setPp(Double pp) {
            this.pp = pp;
        }

        public String getRank() {
            return rank;
        }

        public void setRank(String rank) {
            this.rank = rank;
        }

        public Boolean getReplay() {
            return replay;
        }

        public void setReplay(Boolean replay) {
            this.replay = replay;
        }

        public Integer getScore() {
            return score;
        }

        public void setScore(Integer score) {
            this.score = score;
        }

        public Statistics getStatistics() {
            return statistics;
        }

        public void setStatistics(Statistics statistics) {
            this.statistics = statistics;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Long getUserID() {
            return userID;
        }

        public void setUserID(Long userID) {
            this.userID = userID;
        }

        public MatchScore.PlayerStat getPlayerStat() {
            return playerStat;
        }

        public void setPlayerStat(MatchScore.PlayerStat playerStat) {
            this.playerStat = playerStat;
        }

        public Integer getRanking() {
            return ranking;
        }

        public void setRanking(Integer ranking) {
            this.ranking = ranking;
        }

        public MicroUser getUser() {
            return user;
        }

        public void setUser(MicroUser user) {
            this.user = user;
        }

        @Override
        public String toString() {
            return STR."MatchScore{accuracy=\{accuracy}, bestID=\{bestID}, timestamp=\{timestamp}, scoreID=\{scoreID}, maxCombo=\{maxCombo}, mode='\{mode}\{'\''}, modeInt=\{modeInt}, mods=\{Arrays.toString(mods)}, passed=\{passed}, perfect=\{perfect}, pp=\{pp}, rank='\{rank}\{'\''}, replay=\{replay}, score=\{score}, statistics=\{statistics}, type='\{type}\{'\''}, userID=\{userID}, playerStat=\{playerStat}, ranking=\{ranking}, user=\{user}\{'}'}";
        }
    }

    public boolean isMatchEnd() {
        return this.getMatchStat().getEndTime() != null;
    }

    public void parseNextData(Match m) {
        if (CollectionUtils.isEmpty(m.getEvents())) return;
        // 合并事件
        if (events.getFirst().getEventID() > m.getEvents().getLast().getEventID()) {
            // 新事件在前
            this.events.addAll(0, m.getEvents());
        } else if (events.getLast().getEventID() < m.getEvents().getFirst().getEventID()){
            // 新事件在后
            this.events.addAll(m.getEvents());
        } else if (events.getFirst().getEventID() > m.getEvents().getFirst().getEventID()) {
            // 在中间
            events.removeIf(e -> e.getEventID() <= m.getEvents().getLast().getEventID());
            this.events.addAll(0, m.getEvents());
        } else if (events.getLast().getEventID() < m.getEvents().getLast().getEventID()){
            // 在中间
            events.removeIf(e -> e.getEventID() >= m.getEvents().getFirst().getEventID());
            this.events.addAll(m.getEvents());
        }

        if (CollectionUtils.isEmpty(this.players)) {
            this.players = m.getPlayers();
        } else {
            var p = new ArrayList<MicroUser>(this.players.size());

            p.addAll(this.players);
            p.addAll(m.getPlayers());

            this.players = p.stream().distinct().toList();
        }

        //更新状态
        this.matchStat = m.getMatchStat();
        this.latestEventID = m.getLatestEventID();
        this.firstEventID = m.getFirstEventID();
    }

    public static Match mergeMultipleMatches(List<Match> matches) {
        var m0 = new Match();

        for (var m : matches) {
            m0 = merge2Matches(m0, m);
        }

        return m0;
    }

    public static Match merge2Matches(Match m1, Match m2) {
        var m = m1.clone();
        m.getEvents().addAll(m2.getEvents());
        m.setFirstEventID(Math.min(m1.getFirstEventID(), m2.getFirstEventID()));
        m.setLatestEventID(Math.max(m1.getLatestEventID(), m2.getLatestEventID()));

        m.getPlayers().addAll(m2.getPlayers());
        m.setPlayers(m.getPlayers().stream().distinct().toList());

        m.getMatchStat().setStartTime(
                m1.getMatchStat().getStartTime().isBefore(
                        m2.getMatchStat().getStartTime()) ? m1.getMatchStat().getStartTime() : m2.getMatchStat().getStartTime()
        );
        m.getMatchStat().setEndTime(
                m1.getMatchStat().getEndTime().isAfter(
                        m2.getMatchStat().getEndTime()) ? m1.getMatchStat().getEndTime() : m2.getMatchStat().getEndTime()
        );

        m.getMatchStat().setMatchID(m1.getMatchStat().getMatchID());
        m.getMatchStat().setName(m1.getMatchStat().getName());

        return m;
    }
}
