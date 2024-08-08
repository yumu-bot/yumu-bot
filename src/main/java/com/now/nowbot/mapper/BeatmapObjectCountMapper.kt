package com.now.nowbot.mapper

import com.now.nowbot.entity.BeatmapObjectCountLite
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface BeatmapObjectCountMapper:JpaRepository<BeatmapObjectCountLite, Long> {
    @Query("select b.density from BeatmapObjectCountLite b where b.bid = :bid and b.check = :check")
    fun getDensityByBidAndCheck(bid: Long, check: String): IntArray?

    @Query("select b.density from BeatmapObjectCountLite b where b.bid = :bid")
    fun getDensityByBid(bid: Long): IntArray?

    @Query("""
        select (oc.timestamp_arr[:index] - oc.timestamp_arr[1]) as result
        from osu_beatmap_object_count as oc where 
        oc.bid = :bid
    """, nativeQuery = true)
    fun getTimeStampByBidAndIndex(bid: Long, index:Int): Int?

    @Query("""
        select (oc.timestamp_arr[:index] - oc.timestamp_arr[1])\\:\\:float  /
         (oc.timestamp_arr[array_length(oc.timestamp_arr,1)] - oc.timestamp_arr[1]) as result
        from osu_beatmap_object_count as oc where 
        oc.bid = :bid
    """, nativeQuery = true)
    fun getTimeStampPercentageByBidAndIndex(bid: Long, index:Int): Double?
}