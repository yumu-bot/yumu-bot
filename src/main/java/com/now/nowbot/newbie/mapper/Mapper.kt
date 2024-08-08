package com.now.nowbot.newbie.mapper

import com.now.nowbot.newbie.entity.UserPlayRecords
import com.now.nowbot.service.OsuApiService.OsuBeatmapApiService
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.stereotype.Service
import java.time.LocalDateTime


interface UserPlayRecordsMapper : JpaRepository<UserPlayRecords, Long> {
    @Query("""
        select distinct u.beatmapId as beatmapId from UserPlayRecords u
        where u.mode = 0 and u.userId = :uid and u.date between :start and :end
    """)
    fun queryMap(uid: Int, start: LocalDateTime, end: LocalDateTime): List<CountBeatmap>

    @Query("""
        select u.id as userId, u.beatmapId as beatmapId, u.rank as rank, u.count300 as n300, u.count100 as n100, u.count50 as n50, u.countMiss as misses
        from UserPlayRecords u
        where u.mode = 0 and u.userId = :uid and u.date between :start and :end and u.beatmapId in (:maps)
    """)
    fun queryData(uid: Int, start: LocalDateTime, end: LocalDateTime, maps: List<Int>): List<CountUser>

//    @Query("""
//        select b from BeatmapLite b where b.id in (:ids)
//    """)
//    fun queryMapsData(bid: Long): List<BeatmapRecord>
}

@Service
@ConditionalOnBean(LocalContainerEntityManagerFactoryBean::class, name = ["newbieEntityManagerFactory"])
class NewbieService (
    val mapper: UserPlayRecordsMapper,
    var osuMapService: OsuBeatmapApiService,
) {

    fun checkRank(bid: Int, cache: MutableMap<Int, Boolean>) = cache.getOrPut(bid) {
        osuMapService.isRanked(bid.toLong())
    }

    fun getUserPlayRecords(uid: Int, start: LocalDateTime, end: LocalDateTime, cache: MutableMap<Int, Boolean>): List<Int> {
        val maps = mapper
            .queryMap(uid, start, end)
            .map { it.beatmapId }
            .filter { checkRank(it, cache) }
            .toList()
        val playData = mapper.queryData(uid, start, end, maps)
        // 处理tth
        val countTth = playData.sumOf {
            with(it) {
                n300 + n100 + n50
            }
        }
        // 处理time
        val (pass, unpass) = playData.partition { it.rank != "F" }

        return emptyList()
    }

    fun getAll(uid:Int, cache: MutableMap<Int, Boolean> = mutableMapOf()) : Long{
        val t = LocalDateTime.of(2024, 8, 1, 0, 0)
        val t2 = LocalDateTime.of(2024, 7, 1, 0, 0)
        val maps = mapper.queryMap(uid, t2, t)
        val ranked = maps.map { it.beatmapId }
            .filter { checkRank(it, cache) }
            .toList()
        val d = mapper.queryData(uid, t2, t, ranked)
        var tth: Long = 0
        d.forEach {
            tth += it.n300+it.n100+it.n50+it.misses
        }
        return tth
    }
}



interface CountBeatmap {
    val beatmapId: Int
}
interface CountUser {
    val userId: Int
    val beatmapId: Int
    val rank: String
    val n300: Int
    val n100: Int
    val n50: Int
    val misses: Int
}

interface BeatmapRecord {
    val beatmapId:Long
    val length: Int
}