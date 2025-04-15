package com.now.nowbot.service.osuApiService;

import com.fasterxml.jackson.databind.JsonNode;
import com.now.nowbot.config.NowbotConfig;
import jakarta.annotation.Nullable;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.nio.file.Files;
import java.nio.file.Path;
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
                    .map(s -> {
                        if (s.trim().startsWith("osu file format")) return s;
                        throw new IllegalStateException("not beatmap file");
                    })
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

    @Nullable
    public Path getFullBackgroundPath(long bid) {
        try {
            var localPath = webClient.get()
                    .uri(URL, b -> b.path("/api/file/local/bg/{bid}").build(bid))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            var path = Path.of(localPath);
            if (Files.isRegularFile(path)) return path;

            log.error("获取谱面 {} 背景失败: 文件 [{}] 不存在, 大概率被版权然后移出了资源", bid, localPath);
            return null;
        } catch (WebClientResponseException e) {
            var json = e.getResponseBodyAs(JsonNode.class);
            if (json != null) log.error("获取谱面 {} 背景失败: {}", bid, json.get("message"));
            return null;
        }
    }
}
