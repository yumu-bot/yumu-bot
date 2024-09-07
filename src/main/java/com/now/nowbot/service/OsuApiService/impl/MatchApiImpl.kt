package com.now.nowbot.service.OsuApiService.impl;

import com.now.nowbot.model.JsonData.Match;
import com.now.nowbot.service.OsuApiService.OsuMatchApiService;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.Optional;

@Service
public class MatchApiImpl implements OsuMatchApiService {
    OsuApiBaseService base;

    public MatchApiImpl(OsuApiBaseService baseService) {
        base = baseService;
    }

    private Match getMatchInfo(long mid) {
        return base.osuApiWebClient.get()
                .uri("matches/{mid}", mid)
                .headers(base::insertHeader)
                .retrieve()
                .bodyToMono(Match.class)
                .block();
    }

    private Match getMatchInfo(long mid, long before, long after) {
        Optional<Long> bef = before == 0 ?
                Optional.empty() : Optional.of(before);
        Optional<Long> aft = after == 0 ?
                Optional.empty() : Optional.of(after);

        return base.osuApiWebClient.get()
                .uri(u -> u.path("matches/{mid}")
                        .queryParamIfPresent("before", bef)
                        .queryParamIfPresent("after", aft)
                        .queryParam("limit", 100)
                        .build(mid))
                .headers(base::insertHeader)
                .retrieve()
                .bodyToMono(Match.class)
                .timeout(Duration.ofSeconds(5))
                .block();
    }

    @Override
    public Match getMatchInfo(long mid, int limit) {
        Match match = null;
        long eventId = 0;
        do {
            if (eventId == 0) {
                match = getMatchInfo(mid);
            } else {
                match.parseNextData(getMatchInfoBefore(
                        mid,
                        match.getEvents().getFirst().getEventID()
                ));
            }
            eventId = match.getEvents().getFirst().getEventID();
        }
        while (!match.getFirstEventID().equals(eventId) && --limit >= 0);
        return match;
    }

    @Override
    public Match getMatchInfoFirst(long mid) throws WebClientResponseException {
        return getMatchInfo(mid);
    }

    @Override
    public Match getMatchInfoBefore(long mid, long id) throws WebClientResponseException {
        return getMatchInfo(mid, id, 0);
    }

    @Override
    public Match getMatchInfoAfter(long mid, long id) throws WebClientResponseException {
        return getMatchInfo(mid, 0, id);
    }
}
