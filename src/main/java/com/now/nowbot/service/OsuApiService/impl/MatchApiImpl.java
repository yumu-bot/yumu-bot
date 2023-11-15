package com.now.nowbot.service.OsuApiService.impl;

import com.now.nowbot.model.multiplayer.Match;
import com.now.nowbot.service.OsuApiService.OsuMatchApiService;
import org.springframework.stereotype.Service;

@Service
public class MatchApiImpl implements OsuMatchApiService {
    OsuApiBaseService base;

    public MatchApiImpl(OsuApiBaseService baseService) {
        base = baseService;
    }

    private Match getMatchInfo(int mid) {
        return base.osuApiWebClient.get()
                .uri("matches/{mid}", mid)
                .headers(base::insertHeader)
                .retrieve()
                .bodyToMono(Match.class)
                .block();
    }

    private Match getMatchInfo(int mid, long before) {
        return base.osuApiWebClient.get()
                .uri(u -> u.path("matches/{mid}")
                        .queryParam("before", before)
                        .build(mid))
                .headers(base::insertHeader)
                .retrieve()
                .bodyToMono(Match.class)
                .block();
    }

    @Override
    public Match getMatchInfo(int mid, int limit) {
        Match match = null;
        long eventId = 0;
        do {
            if (eventId == 0) {
                match = getMatchInfo(mid);
            } else {
                match.parseNextData(getMatchInfo(
                        mid,
                        match.getEvents().getFirst().getId()
                ));
            }
            eventId = match.getEvents().getFirst().getId();
        }
        while (!match.getFirstEventId().equals(eventId) && --limit >= 0);
        return match;
    }
}
