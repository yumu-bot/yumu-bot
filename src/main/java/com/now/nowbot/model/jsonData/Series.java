package com.now.nowbot.model.jsonData;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Series {
    Match.MatchStat seriesStat;

    List<Match> matches;

    List<MicroUser> players = new ArrayList<>();

    // 根据多场比赛来构建系列赛
    public Series(List<Match> matches) {
        if (matches.isEmpty()) return;

        this.seriesStat = matches.getFirst().matchStat;
        this.matches = matches;

        for (var m : matches) {
            var t = m.matchStat;

            if (t.startTime.isBefore(this.seriesStat.startTime)) {
                this.seriesStat.startTime = t.startTime;
            }

            if (t.endTime.isAfter(this.seriesStat.endTime)) {
                this.seriesStat.endTime = t.endTime;
            }

            if (StringUtils.hasText(t.name)) {
                this.seriesStat.name = t.name;
            }

            this.players.addAll(m.players);
        }

        // 去重
        LinkedHashSet<MicroUser> userHashSet = new LinkedHashSet<>(this.players);

        this.players = userHashSet.stream().sorted().toList();
    }

    public Match.MatchStat getSeriesStat() {
        return seriesStat;
    }

    public void setSeriesStat(Match.MatchStat seriesStat) {
        this.seriesStat = seriesStat;
    }

    public List<Match> getMatches() {
        return matches;
    }

    public void setMatches(List<Match> matches) {
        this.matches = matches;
    }

    public List<MicroUser> getPlayers() {
        return players;
    }

    public void setPlayers(List<MicroUser> players) {
        this.players = players;
    }
}
