package com.now.nowbot.mapper

import com.now.nowbot.entity.NewbiePlayCount
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.LocalDate
import java.util.*

interface NewbiePlayCountRepository : JpaRepository<NewbiePlayCount, Long> {
    fun findAllByDate(date: LocalDate): List<NewbiePlayCount>

    @Query(
        """
        select 
        count(uid) as date, sum(play_count) as pc, sum(play_hits) as tth, min(pp) as pp_min, max(pp) as pp_max
        from newbie_play_count where uid = :uid and record_date > '2025-01-10'
        """, nativeQuery = true)
    fun getHistoryDate(uid: Long): Optional<NewbiePlayCount.UserHistoryResult>

    @Query("""
            with sr_rank as (
                select uid, row_number() over (order by pc desc) as rank from (
                    select uid, sum(play_count) as pc from newbie_play_count
                    where record_date >= '2025-01-15' -- 15
                    group by uid
                ) as data
            )
            select rank from sr_rank where uid = :uid
            """, nativeQuery = true)
    fun getPlayCountRank(uid: Long): Optional<Int>

    @Query("""
            with sr_rank as (
                select uid, row_number() over (order by tth desc) as rank from (
                    select uid, sum(play_hits) as tth from newbie_play_count
                    where record_date >= '2025-01-15' -- 15
                    group by uid
                ) as data
            )
            select rank from sr_rank where uid = :uid
            """, nativeQuery = true)
    fun getTotalHitsRank(uid: Long): Optional<Int>

    @Query("""
            with sr_rank as (
                select uid, row_number() over (order by pp_diff desc) as rank from (
                    select uid, (max(pp) - min(pp)) as pp_diff from newbie_play_count
                    where record_date >= '2025-01-14' -- 14
                    group by uid
                    having count(uid) > 1
                    order by pp_diff desc
                ) as data
            )
            select rank from sr_rank where uid = :uid
            """, nativeQuery = true)
    fun getPPAddRank(uid: Long): Optional<Int>

    @Query("""
        select uid, sum(play_count) as play_count  
        from newbie_play_count
        where record_date >= '2025-01-15'
        group by uid
        order by play_count desc
        limit 10
        """, nativeQuery = true)
    fun getDailyTop5PlayCount(): List<NewbiePlayCount.UserListResult>

    @Query("""
        select uid, sum(play_hits) as play_hits  
        from newbie_play_count
        where record_date >= '2025-01-15'
        group by uid
        order by play_hits desc
        limit 10
        """, nativeQuery = true)
    fun getDailyTop5TotalHits(): List<NewbiePlayCount.UserListResult>

    @Query("""
        select uid, (max(pp) - min(pp)) as pp  
        from newbie_play_count
        where record_date >= '2025-01-14'
        group by uid
        order by pp desc
        limit 10
        """, nativeQuery = true)
    fun getDailyTop5pp(): List<NewbiePlayCount.UserListResult>

}