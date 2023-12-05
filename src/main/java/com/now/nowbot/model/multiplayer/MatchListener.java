package com.now.nowbot.model.multiplayer;

import com.now.nowbot.service.OsuApiService.OsuMatchApiService;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.BiConsumer;

public class MatchListener {
    Match                                     match;
    OsuMatchApiService                        matchApiService;
    List<BiConsumer<List<MatchEvent>, Match>> consumerList = new ArrayList<>();
    volatile boolean start = false;

    public MatchListener(Match match, OsuMatchApiService service) {
        this.match = match;
        this.matchApiService = service;

    }

    public void addEventListener(BiConsumer<List<MatchEvent>, Match> doListener) {
        consumerList.add(doListener);
    }

    private void onEvents(List<MatchEvent> events, Match match) {
        consumerList.forEach(c -> c.accept(events, match));
    }

    public void startListener() {
        if (match.isMatchEnd() || start) {
            return;
        }
        start = true;
        Thread.startVirtualThread(() -> {
            long matchID = match.getMatchStat().getId();
            long recordID;
            //recordID = match.latestEventId;
            try {
                var games = match.getEvents().stream().filter(s -> s.getRound() != null).filter(s -> s.getRound().getId() != null).toList();
                recordID = games.getLast().getId() - 1;
            } catch (NoSuchElementException e) {
                recordID = match.latestEventId;
            }

            while (start && !match.isMatchEnd()) {
                try {
                    Thread.sleep(Duration.ofSeconds(10));
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                var newMatch = matchApiService.getMatchInfoAfter(matchID, recordID);

                long newID;
                try {
                    var newGames = newMatch.getEvents().stream().filter(s -> s.getRound() != null).filter(s -> s.getRound().getId() != null).toList();
                    newID = newGames.getLast().getId();
                } catch (NoSuchElementException e) {
                    newID = newMatch.latestEventId;
                }

                if (newID == recordID) continue;

                if (Objects.nonNull(newMatch.getCurrentGameId())) {
                    if (recordID != newID - 1) {
                        recordID = newID - 1;
                        onEvents(newMatch.getEvents(), match);
                    }
                    continue;
                }
                recordID = newID;
                match.parseNextData(newMatch);
                onEvents(newMatch.getEvents(), match);
            }
        });
    }

    public void stopListener() {
        start = false;
    }
}
