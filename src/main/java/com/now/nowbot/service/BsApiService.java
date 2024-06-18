package com.now.nowbot.service;

import com.now.nowbot.config.NowbotConfig;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service("BsApiService")
public class BsApiService {
    @Resource
    WebClient webClient;
    private static final String URL = NowbotConfig.BS_API_URL;

    public String getOsuFile(long bid) {
        return webClient.get()
                .uri(URL, b -> b.path("/").build())
                .header("AuthorizationX", "")
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }
}
