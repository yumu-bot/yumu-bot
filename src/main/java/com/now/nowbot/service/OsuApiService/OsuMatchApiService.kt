package com.now.nowbot.service.OsuApiService;

import com.now.nowbot.model.JsonData.Match;
import org.springframework.web.reactive.function.client.WebClientResponseException;

public interface OsuMatchApiService {
    Match getMatchInfo(long mid, int limit) throws WebClientResponseException;
    Match getMatchInfoFirst(long mid) throws WebClientResponseException;
    Match getMatchInfoBefore(long mid, long id) throws WebClientResponseException;
    Match getMatchInfoAfter(long mid, long id) throws WebClientResponseException;
}
