package com.now.nowbot.model.mappool;

import com.now.nowbot.model.JsonData.BeatMap;
import com.now.nowbot.service.OsuApiService.OsuBeatmapApiService;
import org.springframework.lang.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class MapPool {
    Integer id = 0;
    String name = "MapPool";
    Long firstMapSID = 0L;
    List<ModPool> modPools = new ArrayList<>();

    public MapPool(@Nullable String name, Map<String, List<Long>> modBIDMap, OsuBeatmapApiService osuBeatmapApiService) {
        if (!(name == null || name.isBlank())) this.name = name;

        modBIDMap.forEach((key, value) -> {
            List<BeatMap> beatmaps = new ArrayList<>();

            for (var bid : value) {
                var b = osuBeatmapApiService.getBeatMapInfo(bid);

                beatmaps.add(b);
            }

            this.modPools.add(new ModPool(key, beatmaps));

        });

        if (!modPools.isEmpty() && !modPools.get(0).getBeatMaps().isEmpty()) {
            firstMapSID = Long.valueOf(
                    modPools.get(0).getBeatMaps()
                            .get(0).getBeatMapSet().getSID());
        }
    }

    public void sortModPools() {
        modPools = modPools.stream().sorted(Comparator.comparingInt(s -> s.getMod().getPriority())).toList();
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getFirstMapSID() {
        return firstMapSID;
    }

    public void setFirstMapSID(Long firstMapSID) {
        this.firstMapSID = firstMapSID;
    }

    public List<ModPool> getModPools() {
        return modPools;
    }

    public void setModPools(List<ModPool> modPools) {
        this.modPools = modPools;
    }
}
