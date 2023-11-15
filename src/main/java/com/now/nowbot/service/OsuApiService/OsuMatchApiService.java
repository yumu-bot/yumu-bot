package com.now.nowbot.service.OsuApiService;

import com.now.nowbot.model.multiplayer.Match;
import org.springframework.web.reactive.function.client.WebClientResponseException;

public interface OsuMatchApiService {
    Match getMatchInfo(int mid, int limit) throws WebClientResponseException;
}
