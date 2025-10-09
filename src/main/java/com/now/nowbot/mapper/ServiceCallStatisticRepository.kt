package com.now.nowbot.mapper

import com.now.nowbot.entity.ServiceCallStatistic
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.LocalDateTime

interface ServiceCallStatisticRepository : JpaRepository<ServiceCallStatistic, Long> {
    @Query(
        value = """
            SELECT * FROM service_call_stat
            WHERE time BETWEEN :from AND :to;
            """, nativeQuery = true
    ) fun getBetween(
        from: LocalDateTime, to: LocalDateTime
    ): List<ServiceCallStatistic>

    @Query(
        value = """
            SELECT * FROM service_call_stat
            WHERE group_id = :group AND time BETWEEN :from AND :to;
            """, nativeQuery = true
    ) fun getBetweenInGroup(
        group: Long, from: LocalDateTime, to: LocalDateTime
    ): List<ServiceCallStatistic>


    @Query(
        value = """
            SELECT (param -> 'bids' ->> 0)::bigint as first_bid
            FROM service_call_stat
            WHERE group_id = :group AND time BETWEEN :from AND :to
                AND name = :name
                AND jsonb_exists(param, 'bids')
                AND jsonb_typeof(param -> 'bids') = 'array'
                AND jsonb_array_length(param -> 'bids') > 0
                AND param -> 'bids' -> 0 IS NOT NULL
            ORDER BY time DESC
            LIMIT 1;
        """, nativeQuery = true
    ) fun getLastAvailableBeatmapIDByGroupAndName(
        group: Long, name: String, from: LocalDateTime, to: LocalDateTime
    ): Long?

    @Query(
        value = """
            SELECT (param -> 'bids' ->> 0)::bigint as first_bid
            FROM service_call_stat
            WHERE group_id = :group AND time BETWEEN :from AND :to
                AND jsonb_exists(param, 'bids')
                AND jsonb_typeof(param -> 'bids') = 'array'
                AND jsonb_array_length(param -> 'bids') > 0
                AND param -> 'bids' -> 0 IS NOT NULL
            ORDER BY time DESC
            LIMIT 1;
        """, nativeQuery = true
    ) fun getLastAvailableBeatmapIDByGroup(
        group: Long, from: LocalDateTime, to: LocalDateTime
    ): Long?

    @Query(
        value = """
            SELECT (param -> 'sids' ->> 0)::bigint as first_sid
            FROM service_call_stat
            WHERE group_id = :group AND time BETWEEN :from AND :to
                AND name = :name
                AND jsonb_exists(param, 'sids')
                AND jsonb_typeof(param -> 'sids') = 'array'
                AND jsonb_array_length(param -> 'sids') > 0
                AND param -> 'bids' -> 0 IS NOT NULL
            ORDER BY time DESC
            LIMIT 1;
        """, nativeQuery = true
    ) fun getLastAvailableBeatmapsetIDByGroupAndName(
        group: Long, name: String, from: LocalDateTime, to: LocalDateTime
    ): Long?

    @Query(
        value = """
            SELECT (param -> 'sids' ->> 0)::bigint as first_sid
            FROM service_call_stat
            WHERE group_id = :group AND time BETWEEN :from AND :to
                AND jsonb_exists(param, 'sids')
                AND jsonb_typeof(param -> 'sids') = 'array'
                AND jsonb_array_length(param -> 'sids') > 0
                AND param -> 'bids' -> 0 IS NOT NULL
            ORDER BY time DESC
            LIMIT 1;
        """, nativeQuery = true
    ) fun getLastAvailableBeatmapsetIDByGroup(
        group: Long, from: LocalDateTime, to: LocalDateTime
    ): Long?
}