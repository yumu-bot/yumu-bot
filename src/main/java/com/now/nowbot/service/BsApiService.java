package com.now.nowbot.service;

import com.now.nowbot.config.NowbotConfig;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Optional;

@Service("BsApiService")
public class BsApiService {
    @Resource
    WebClient webClient;
    private static final String           URL   = NowbotConfig.BS_API_URL;
    private static final Optional<String> TOKEN = NowbotConfig.BS_TOKEN;

    public String getOsuFile(long bid) {
        if (TOKEN.isEmpty()) throw new RuntimeException("无法权限访问");
        return webClient.get()
                .uri(URL, b -> b.path("/api/file/map/osufile/{bid}").build(bid))
                .header("AuthorizationX", TOKEN.get())
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }
}
