package com.now.nowbot.model.multiplayer;

import com.now.nowbot.service.OsuApiService.OsuMatchApiService;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

public class MatchListener {
    Match                            match;
    OsuMatchApiService               matchApiService;
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
            long mid = match.getMatchStat().getId();
            long lastId = match.latestEventId;
            while (start && !match.isMatchEnd()) {
                try {
                    Thread.sleep(Duration.ofSeconds(10));
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                var newMatch = matchApiService.getMatchInfoAfter(mid, lastId);
                if (newMatch.latestEventId == lastId) continue;
                lastId = newMatch.latestEventId;
                var lastEvent = newMatch.getEvents().getLast();
                if (lastEvent.getDetail().type().equals("other")) {
                    --lastId;
                } else {
                    match.parseNextData(newMatch);
                }
                onEvents(newMatch.getEvents(), match);
            }
        });
    }
    public void stopListener() {
        start = false;
    }
}
