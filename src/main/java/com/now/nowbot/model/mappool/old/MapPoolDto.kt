package com.now.nowbot.model.mappool.old

import com.now.nowbot.model.mappool.now.Pool
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService

class MapPoolDto(
    val name: String? = "MapPool",
    poolMap: Map<String, List<Long>?>,
    private val osuBeatmapApiService: OsuBeatmapApiService
) {
    var id: Int = 0

    val modPools: List<ModPool> = poolMap.map { pool ->
        val beatMaps = (pool.value ?: listOf() ).map { osuBeatmapApiService.getBeatMap(it) }

        ModPool(pool.key, beatMaps)
    }

    private val firstMapSID: Long = if (modPools.isNotEmpty() && modPools.first().beatmaps.isNotEmpty()) {
        modPools.first().beatmaps.first().beatMapSet!!.beatMapSetID
    } else {
        0L
    }

    constructor(pool: Pool, beatmapApiService: OsuBeatmapApiService) : this(
        pool.name,
        getModMapFromPool(pool),
        beatmapApiService
    ) {
        this.id = pool.id
    }

    fun sortModPools() {
        // modPools = modPools.stream().sorted(Comparator.comparingInt(s -> s.getMod().getPriority())).toList();
    }

    companion object {
        private fun getModMapFromPool(pool: Pool): Map<String, List<Long>> {
            return pool.categoryList.associate {
                it.name to it.category.map { category -> category.bid }
            }
        }
    }
}
