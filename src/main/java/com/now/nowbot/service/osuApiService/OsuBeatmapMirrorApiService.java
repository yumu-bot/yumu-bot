package com.now.nowbot.service.osuApiService;

import com.now.nowbot.config.NowbotConfig;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Optional;

// BsApiService
@Service("OsuBeatmapMirrorApiService")
public class OsuBeatmapMirrorApiService {

    @Resource
    WebClient webClient;

    private static final String           URL   = NowbotConfig.BEATMAP_MIRROR_URL;
    private static final Optional<String> TOKEN = NowbotConfig.BEATMAP_MIRROR_TOKEN;

    public String getOsuFile(long bid) {
        return webClient.get()
                .uri(URL, b -> b.path("/api/file/map/osufile/{bid}").build(bid))
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }
}
