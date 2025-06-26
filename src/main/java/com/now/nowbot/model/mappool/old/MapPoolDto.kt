package com.now.nowbot.model.mappool.old

import com.now.nowbot.model.osu.LazerMod
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.mappool.now.Pool
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.service.osuApiService.OsuCalculateApiService
import com.now.nowbot.util.AsyncMethodExecutor

class MapPoolDto(
    var name: String? = "MapPool",
    var mode: OsuMode = OsuMode.DEFAULT,
    poolMap: Map<String, List<Long>>?,
    private val beatmapApiService: OsuBeatmapApiService,
    private val calculateApiService: OsuCalculateApiService
) {
    var id: Int = 0

    val modPools: List<ModPool> = if (poolMap.isNullOrEmpty()) {
        listOf()
    } else {
        val action = AsyncMethodExecutor.awaitSupplierExecute {
            poolMap.mapValues { pool ->
                pool.value.map {
                    beatmapApiService.getBeatmapFromDatabase(it)
                }
            }
        }

        action.map { ModPool(it.key, it.value) }
    }

    val firstMapSID: Long = modPools.firstOrNull()?.beatmaps?.firstOrNull()?.beatmapset?.beatmapsetID ?: 0L

    val averageStarRating : Double = if (modPools.isEmpty()) {
        0.0
    } else {
        val hasChange = modPools.filter { LazerMod.hasStarRatingChange(listOf(it.mod ?: return@filter false)) }

        AsyncMethodExecutor.awaitRunnableExecute(hasChange.map {
            it.beatmaps.map { b ->
                AsyncMethodExecutor.Runnable {
                    calculateApiService.applyBeatMapChanges(b, listOf(it.mod!!))
                    calculateApiService.applyStarToBeatMap(b, mode, listOf(it.mod!!))
                }
            }
        }.flatten())

        modPools.flatMap { it.beatmaps }.map { it.starRating }.average()
    }


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
