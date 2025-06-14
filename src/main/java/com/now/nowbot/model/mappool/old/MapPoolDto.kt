package com.now.nowbot.model.mappool.old

import com.now.nowbot.model.osu.LazerMod
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.mappool.now.Pool
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.service.osuApiService.OsuCalculateApiService

class MapPoolDto(
    var name: String? = "MapPool",
    var mode: OsuMode = OsuMode.DEFAULT,
    poolMap: Map<String, List<Long>>?,
    private val beatmapApiService: OsuBeatmapApiService,
    private val calculateApiService: OsuCalculateApiService
) {
    var id: Int = 0

    val modPools: List<ModPool> = poolMap?.map { pool ->
        val beatmaps = pool.value.map { beatmapApiService.getBeatmap(it) }

        ModPool(pool.key, beatmaps)
    } ?: listOf()

    val firstMapSID: Long = if (modPools.isNotEmpty() && modPools.first().beatmaps.isNotEmpty()) {
        modPools.first().beatmaps.first().beatmapset!!.beatmapsetID
    } else {
        0L
    }

    val averageStarRating : Double = if (modPools.isNotEmpty()) {
        modPools.map {
            if (LazerMod.hasStarRatingChange(listOf(it.mod))) {
                it.beatmaps.forEach { b ->
                    calculateApiService.applyBeatMapChanges(b, listOf(it.mod))
                    calculateApiService.applyStarToBeatMap(b, mode, listOf(it.mod))
                }
            }

            it.beatmaps.map { it.starRating }
        }.flatten().average()
    } else 0.0

    constructor(pool: Pool, beatmapApiService: OsuBeatmapApiService, calculateApiService: OsuCalculateApiService) : this(
        pool.name,
        OsuMode.getMode(pool.mode),
        getModMapFromPool(pool),
        beatmapApiService,
        calculateApiService
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
