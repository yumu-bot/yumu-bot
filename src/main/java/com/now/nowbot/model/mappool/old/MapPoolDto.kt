package com.now.nowbot.model.mappool.old

import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.mappool.now.Pool
import com.now.nowbot.model.osu.LazerMod.Companion.isAffectStarRating
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.service.osuApiService.OsuCalculateApiService
import com.now.nowbot.util.AsyncMethodExecutor
import java.util.concurrent.Callable

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
        poolMap.mapValues { (key, value) ->

            val action = value.map {
                Callable {
                    beatmapApiService.getBeatmapFromDatabase(it)
                }
            }
            AsyncMethodExecutor.awaitCallableExecute(action)
        }.map { ModPool(it.key, it.value) }
    }

    val firstMapSID: Long = modPools.firstOrNull()?.beatmaps?.firstOrNull()?.beatmapset?.beatmapsetID ?: 0L

    val averageStarRating : Double = if (modPools.isEmpty()) {
        0.0
    } else {
        val hasChange = modPools.filter { pool ->
            val mod = pool.mod ?: return@filter false

            listOf(mod).isAffectStarRating()
        }

        AsyncMethodExecutor.awaitRunnableExecute(hasChange.map { pool ->
            val mod = pool.mod!!

            pool.beatmaps.map { b ->
                AsyncMethodExecutor.Runnable {
                    calculateApiService.applyBeatmapChanges(b, listOf(mod))
                    calculateApiService.applyStarToBeatmap(b, mode, listOf(mod))
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
