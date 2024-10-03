package com.now.nowbot.service.osuApiService;

import com.now.nowbot.config.NowbotConfig;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Optional;

// BsApiService
@Service("OsuBeatmapMirrorApiService")
public class OsuBeatmapMirrorApiService {

    private static final Logger log = LoggerFactory.getLogger(OsuBeatmapMirrorApiService.class);
    @Resource
    WebClient webClient;

    private static final String           URL   = NowbotConfig.BEATMAP_MIRROR_URL;
    private static final Optional<String> TOKEN = NowbotConfig.BEATMAP_MIRROR_TOKEN;

    public String getOsuFile(long bid) {
        try {
            return webClient.get()
                    .uri(URL, b -> b.path("/api/file/map/osufile/{bid}").build(bid))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (Exception e) {
            if (e instanceof WebClientResponseException re) {
                log.error("谱面镜像站：返回谱面 {} 失败：{}", bid, re.getStatusCode());
                log.error(re.getResponseBodyAsString());
            }
            if (e instanceof WebClientRequestException re) {
                log.error("谱面镜像站：请求谱面 {} 失败!", bid);
                log.error(re.getMessage());
            }
            throw e;
        }
    }
}
