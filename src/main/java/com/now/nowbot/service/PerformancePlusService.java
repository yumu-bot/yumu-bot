package com.now.nowbot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.now.nowbot.config.FileConfig;
import com.now.nowbot.model.JsonData.PPPlus;
import com.now.nowbot.model.JsonData.Score;
import com.now.nowbot.model.enums.Mod;
import com.now.nowbot.service.OsuApiService.impl.BeatmapApiImpl;
import com.now.nowbot.util.JacksonUtil;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service("PP_PLUS_SEV")
public class PerformancePlusService {
    private static final Logger log = LoggerFactory.getLogger(PerformancePlusService.class);
    private final String API = "https://ppp.365246692.xyz"; //"http://localhost:46880";
    private final Path   OSU_FILE_DIR;
    @Resource
    WebClient      webClient;
    @Resource
    BeatmapApiImpl beatmapApi;

    public PerformancePlusService(FileConfig config) {
        OSU_FILE_DIR = Path.of(config.getOsuFilePath());
    }

    public PPPlus getMapPerformancePlus(Long beatmapId) {
        checkFile(beatmapId);
        return webClient.get()
                .uri(
                        u -> u.pathSegment(API + "/api/calculation")
                                .queryParam("BeatmapId", beatmapId)
                                .build()
                )
                .retrieve()
                .bodyToMono(PPPlus.class)
                .block();
    }

    public List<PPPlus> getScorePerformancePlus(Iterable<Score> scores) {
        List<ScorePerformancePlus> body = new ArrayList<>();
        for (var score : scores) {
            checkFile(score.getBeatMap().getId());
            var mods = Mod.getModsValueFromStr(score.getMods());
            var combo = score.getMaxCombo();
            var misses = Objects.requireNonNullElse(score.getStatistics().getCountMiss(), 0);
            // 这俩我猜测是 50 和 100 的数量
            var mehs = Objects.requireNonNullElse(score.getStatistics().getCount50(), 0);
            var oks = Objects.requireNonNullElse(score.getStatistics().getCount100(), 0);
            body.add(new ScorePerformancePlus(score.getBeatMap().getId().toString(), mods, combo, misses, mehs, oks));
        }

        return webClient.post()
                .uri(API + "/api/batch/calculation")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(JacksonUtil.toJson(body))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(node -> JacksonUtil.parseObjectList(node, PPPlus.class))
                .block();
    }

    // beatmapId 居然要 String ??? [https://difficalcy.syrin.me/api-reference/difficalcy-osu/#post-apibatchcalculation](啥玩意)
    record ScorePerformancePlus(String beatmapId, int mods, int combo, int misses, int mehs, int oks) {
    }


    private void checkFile(Long beatmapId) {
        var beatmapFiles = OSU_FILE_DIR.resolve(beatmapId + ".osu");
        if (! Files.isRegularFile(beatmapFiles)) {
            try {
                beatmapApi.getBeatMapFile(beatmapId);
            } catch (Exception e) {
                log.error("下载谱面文件失败", e);
                throw new RuntimeException("下载谱面文件失败");
            }
        }
    }
}
