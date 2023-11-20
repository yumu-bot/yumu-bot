package com.now.nowbot.model.multiplayer;
import com.now.nowbot.model.JsonData.MicroUser;

import java.util.ArrayList;
import java.util.List;

public class Series {
    SeriesStat seriesStat = new SeriesStat();
    List<Match> matches = new ArrayList<>();
    List<PlayerData> playerDataList = new ArrayList<>();
    List<MicroUser> players = new ArrayList<>();

    //需要自己构建
    List<Long> FirstMapSIDs = new ArrayList<>();

    public Series(){}

    public Series(List<Match> matches) {
        this.matches = matches;
    }

    public SeriesStat getSeriesStat() {
        return seriesStat;
    }

    public void setSeriesStat(SeriesStat seriesStat) {
        this.seriesStat = seriesStat;
    }

    public List<Match> getMatches() {
        return matches;
    }

    public void setMatches(List<Match> matches) {
        this.matches = matches;
    }

    public List<PlayerData> getPlayerDataList() {
        return playerDataList;
    }

    public void setPlayerDataList(List<PlayerData> playerDataList) {
        this.playerDataList = playerDataList;
    }

    public List<MicroUser> getPlayers() {
        return players;
    }

    public void setPlayers(List<MicroUser> players) {
        this.players = players;
    }

    public List<Long> getFirstMapSIDs() {
        return FirstMapSIDs;
    }

    public void setFirstMapSIDs(List<Long> firstMapSIDs) {
        FirstMapSIDs = firstMapSIDs;
    }
}


