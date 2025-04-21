package com.now.nowbot.model.mappool.old;

import com.now.nowbot.model.json.BeatMap;
import com.now.nowbot.model.mappool.now.Pool;
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService;
import org.springframework.lang.Nullable;

import java.util.*;

public class MapPoolDto {
    Integer id = 0;
    String name = "MapPool";
    Long firstMapSID = 0L;
    List<ModPool> modPools = new ArrayList<>();

    public MapPoolDto(@Nullable String name, Map<String, List<Long>> modBIDMap, OsuBeatmapApiService osuBeatmapApiService) {
        if (!(name == null || name.isBlank())) this.name = name;

        modBIDMap.forEach((key, value) -> {
            List<BeatMap> beatmaps = new ArrayList<>();

            for (var bid : value) {
                var b = osuBeatmapApiService.getBeatMap(bid);

                beatmaps.add(b);
            }

            this.modPools.add(new ModPool(key, beatmaps));

        });

        if (! modPools.isEmpty() && ! modPools.getFirst().getBeatmaps().isEmpty()) {
            firstMapSID = Objects.requireNonNull(
                    modPools.getFirst().getBeatmaps()
                            .getFirst().getBeatMapSet()).getBeatMapSetID();
        }
    }

    public MapPoolDto(Pool pool, OsuBeatmapApiService osuBeatmapApiService) {
        this(pool.getName(), getModMapFromPool(pool), osuBeatmapApiService);
        this.id = pool.getId();
    }

    private static Map<String, List<Long>> getModMapFromPool(Pool pool) {
        Map<String, List<Long>> modBidMap = new LinkedHashMap<>();
        pool.getCategoryList().forEach(group -> {
            var mList = new ArrayList<Long>();
            group.getCategory().forEach(category -> mList.add(category.bid()));
            modBidMap.put(group.getName(), mList);
        });
        return modBidMap;
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
