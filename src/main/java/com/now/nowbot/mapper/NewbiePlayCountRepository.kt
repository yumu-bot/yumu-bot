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
                select uid, row_number() over (order by pc) as rank from (
                    select uid, sum(play_count) as pc from newbie_play_count
                    where record_date >= '2025-01-13' -- 15
                    group by uid order by pc desc
                ) as data
            )
            select rank from sr_rank where uid = :uid
            """, nativeQuery = true)
    fun getPlayCountRank(uid: Long): Optional<Int>

    @Query("""
            with sr_rank as (
                select uid, row_number() over (order by tth) as rank from (
                    select uid, sum(play_hits) as tth from newbie_play_count
                    where record_date >= '2025-01-13' -- 15
                    group by uid order by tth desc
                ) as data
            )
            select rank from sr_rank where uid = :uid
            """, nativeQuery = true)
    fun getTotalHitsRank(uid: Long): Optional<Int>

    @Query("""
            with sr_rank as (
                select uid, row_number() over (order by pp_diff) as rank from (
                    select uid, (max(pp) - min(pp)) as pp_diff from newbie_play_count
                    where record_date >= '2025-01-13' -- 14
                    group by uid
                    having count(uid) > 1
                    order by pp_diff desc
                ) as data
            )
            select rank from sr_rank where uid = :uid
            """, nativeQuery = true)
    fun getPPAddRank(uid: Long): Optional<Int>


}