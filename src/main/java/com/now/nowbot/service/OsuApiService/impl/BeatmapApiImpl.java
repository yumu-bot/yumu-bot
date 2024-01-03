package com.now.nowbot.service.OsuApiService.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.now.nowbot.config.FileConfig;
import com.now.nowbot.dao.BeatMapDao;
import com.now.nowbot.model.JsonData.BeatMap;
import com.now.nowbot.model.JsonData.BeatMapSet;
import com.now.nowbot.model.JsonData.BeatmapDifficultyAttributes;
import com.now.nowbot.model.JsonData.Search;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.service.OsuApiService.OsuBeatmapApiService;
import com.now.nowbot.util.AsyncMethodExecutor;
import com.now.nowbot.util.JacksonUtil;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
public class BeatmapApiImpl implements OsuBeatmapApiService {
    OsuApiBaseService base;
    FileConfig fileConfig;
    BeatMapDao beatMapDao;

    public BeatmapApiImpl(OsuApiBaseService baseService,
                          @Lazy FileConfig config,
                          @Lazy BeatMapDao mapDao
    ) {
        base = baseService;
        fileConfig = config;
        beatMapDao = mapDao;
    }

    @Override
    public String getBeatMapFile(long bid) throws Exception {
        return AsyncMethodExecutor.execute(() -> _getBetamapFile(bid), "_getBetamapFile" + bid, (String) null);
    }

    private String _getBetamapFile(long bid) throws IOException {
        Path f = Path.of(fileConfig.getOsuFilePath(), bid + ".osu");
        if (Files.isRegularFile(f)) {
            return Files.readString(f);
        }
        String osuStr = base.osuApiWebClient.get()
                .uri("https://osu.ppy.sh/osu/{bid}", bid)
                .retrieve()
                .bodyToMono(String.class)
                .block();
        if (Objects.isNull(osuStr)) return null;
        Files.writeString(f, osuStr);
        return osuStr;
    }

    @Override
    public BeatMap getBeatMapInfo(long bid) {
        return base.osuApiWebClient.get()
                .uri("beatmaps/{bid}", bid)
                .headers(base::insertHeader)
                .retrieve()
                .bodyToMono(BeatMap.class)
                .map(b -> {
                    beatMapDao.saveMap(b);
                    return b;
                })
                .block();
    }

    @Override
    public BeatMapSet getBeatMapSetInfo(long sid) {
        return base.osuApiWebClient.get()
                .uri("beatmapsets/{sid}", sid)
                .headers(base::insertHeader)
                .retrieve()
                .bodyToMono(BeatMapSet.class)
                .map(s -> {
                    beatMapDao.saveMapSet(s);
                    return s;
                })
                .block();
    }

    @Override
    public BeatMap getMapInfoFromDB(long bid) {
        try {
            var lite = beatMapDao.getBeatMapLite(bid);
            return BeatMapDao.fromBeatmapLite(lite);
        } catch (Exception e) {
            return getBeatMapInfo(bid);
        }
    }

    @Override
    public BeatmapDifficultyAttributes getAttributes(Long id, OsuMode mode) {
        Map<String, Object> body = new HashMap<>();
        if (!OsuMode.isDefault(mode)) {
            body.put("ruleset_id", mode.getModeValue());
        }
        return base.osuApiWebClient.post()
                .uri("beatmaps/{id}/attributes", id)
                .headers(base::insertHeader)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .mapNotNull(j -> JacksonUtil.parseObject(j.get("attributes"), BeatmapDifficultyAttributes.class))
                .block();
    }

    @Override
    public BeatmapDifficultyAttributes getAttributes(Long id, OsuMode mode, int modsValue) {
        Map<String, Object> body = new HashMap<>();
        if (!OsuMode.isDefault(mode)) {
            body.put("ruleset_id", mode.getModeValue());
        }
        if (modsValue != 0) {
            body.put("mods", modsValue);
        }
        return base.osuApiWebClient.post()
                .uri("beatmaps/{id}/attributes", id)
                .headers(base::insertHeader)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .mapNotNull(j -> JacksonUtil.parseObject(j.get("attributes"), BeatmapDifficultyAttributes.class))
                .block();
    }

    @Override
    public JsonNode lookupBeatmap(String checksum, String filename, Long id) {
        return base.osuApiWebClient.get()
                .uri(u -> u.path("beatmapsets/lookup")
                        .queryParamIfPresent("checksum", Optional.ofNullable(checksum))
                        .queryParamIfPresent("filename", Optional.ofNullable(filename))
                        .queryParamIfPresent("id", Optional.ofNullable(id))
                        .build())
                .headers(base::insertHeader)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
    }

    @Override
    public Search searchBeatmap(Map<String, Object> query) {
        return base.osuApiWebClient.get()
                .uri(u -> {
                    u.path("beatmapsets/search");
                    query.forEach((k, v) -> {
                        if (Objects.nonNull(v)) u.queryParam(k, v);
                        else u.queryParam(k);
                    });
                    return u.build();
                })
                .headers(base::insertHeader)
                .retrieve()
                .bodyToMono(Search.class)
                .block();
    }
}
