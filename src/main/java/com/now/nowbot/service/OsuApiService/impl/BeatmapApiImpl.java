package com.now.nowbot.service.OsuApiService.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.now.nowbot.model.JsonData.BeatMap;
import com.now.nowbot.model.JsonData.BeatmapDifficultyAttributes;
import com.now.nowbot.model.JsonData.Search;
import com.now.nowbot.service.OsuApiService.OsuBeatmapApiService;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class BeatmapApiImpl implements OsuBeatmapApiService {
    OsuApiBaseService base;

    public BeatmapApiImpl(OsuApiBaseService baseService) {
        base = baseService;
    }

    @Override
    public String getBeatMapFile(long bid) {
        return null;
    }

    @Override
    public BeatMap getBeatMapInfo(long bid) {
        return null;
    }

    @Override
    public BeatMap getMapInfoFromDB(long bid) {
        return null;
    }

    @Override
    public BeatmapDifficultyAttributes getAttributes(Long id) {
        return null;
    }

    @Override
    public BeatmapDifficultyAttributes getAttributes(Long id, int modsValue) {
        return null;
    }

    @Override
    public JsonNode lookupBeatmap(String checksum, String filename, Long id) {
        return null;
    }

    @Override
    public Search searchBeatmap(Map<String, Object> query) {
        return null;
    }
}
