package com.now.nowbot.model.multiplayer;

import com.now.nowbot.service.OsuApiService.OsuMatchApiService;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
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
                recordID = match.getEvents().stream().filter(s -> s.getRound() != null).filter(s -> s.getRound().getId() != null).toList().getLast().getId();
            } catch (NullPointerException e) {
                recordID = match.latestEventId;
            }

            while (start && !match.isMatchEnd()) {
                try {
                    Thread.sleep(Duration.ofSeconds(10));
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                var newMatch = matchApiService.getMatchInfoAfter(matchID, recordID);
                if (newMatch.latestEventId == recordID) continue;
                if (Objects.nonNull(newMatch.getCurrentGameId())) {
                    if (recordID != newMatch.latestEventId - 1) {
                        recordID = newMatch.latestEventId - 1;
                        onEvents(newMatch.getEvents(), match);
                    }
                    continue;
                }
                recordID = newMatch.latestEventId;
                match.parseNextData(newMatch);
                onEvents(newMatch.getEvents(), match);
            }
        });
    }

    public void stopListener() {
        start = false;
    }
}
