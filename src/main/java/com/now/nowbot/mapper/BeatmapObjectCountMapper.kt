package com.now.nowbot.mapper

import com.now.nowbot.entity.BeatmapObjectCountLite
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface BeatmapObjectCountMapper:JpaRepository<BeatmapObjectCountLite, Long> {
    @Query("""
         select density from osu_beatmap_object_count where bid = :bid and check_str = :check
    """, nativeQuery = true)
    fun getDensityByBidAndCheck(bid: Long, check: String): List<IntArray>

    @Query("select b.density from BeatmapObjectCountLite b where b.bid = :bid")
    fun getDensityByBid(bid: Long): List<IntArray>

    @Query("""
        select (oc.timestamp_arr[:index] - oc.timestamp_arr[1]) as result
        from osu_beatmap_object_count as oc where 
        oc.bid = :bid
    """, nativeQuery = true)
    fun getTimeStampByBidAndIndex(bid: Long, index:Int): Int?

    @Query("""
        select oc.bid as bid, oc.timestamp_arr as times from osu_beatmap_object_count as oc where 
        oc.bid in (:bid)
    """, nativeQuery = true)
    fun getTimeStampByBid(bid: Collection<Long>): List<BeatmapObjectCountLite.TimeResult>

    @Query("""
        select (oc.timestamp_arr[:index] - oc.timestamp_arr[1])::float /
         (oc.timestamp_arr[array_length(oc.timestamp_arr,1)] - oc.timestamp_arr[1]) as result
        from osu_beatmap_object_count as oc where 
        oc.bid = :bid
    """, nativeQuery = true)
    fun getTimeStampPercentageByBidAndIndex(bid: Long, index:Int): Double?
}