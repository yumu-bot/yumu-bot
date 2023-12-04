package com.now.nowbot.model.multiplayer;

import com.now.nowbot.service.OsuApiService.OsuMatchApiService;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class MatchListener {
    Match                            match;
    OsuMatchApiService               matchApiService;
    List<Consumer<List<MatchEvent>>> consumerList = new ArrayList<>();
    volatile boolean start = false;

    public MatchListener(Match match, OsuMatchApiService service) {
        this.match = match;
        this.matchApiService = service;

    }

    public void addEventListener(Consumer<List<MatchEvent>> doListener) {
        consumerList.add(doListener);
    }

    private void onEvents(List<MatchEvent> events) {
        consumerList.forEach(c -> c.accept(events));
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
                System.out.println("post");
                var m = matchApiService.getMatchInfoAfter(mid, lastId);
                if (m.latestEventId == lastId) continue;
                lastId = m.latestEventId;
                match.parseNextData(m);
                consumerList.forEach(c -> c.accept(m.getEvents()));
            }
        });
    }
    public void stopListener() {
        start = false;
    }
}
