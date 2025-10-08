package com.now.nowbot.mapper

import com.now.nowbot.entity.ServiceCallStatisticLite
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.LocalDateTime

interface ServiceCallStatisticRepository : JpaRepository<ServiceCallStatisticLite, Long> {
    @Query(
        value = """
            SELECT service FROM service_call_stat
            WHERE time BETWEEN :from AND :to;
            """, nativeQuery = true
    ) fun getBetween(
        from: LocalDateTime, to: LocalDateTime
    ): List<ServiceCallStatisticLite.ServiceCall>

    @Query(
        value = """
            SELECT service FROM service_call_stat
            WHERE group_id = :group AND time BETWEEN :from AND :to;
            """, nativeQuery = true
    ) fun getBetweenInGroup(
        group: Long, from: LocalDateTime, to: LocalDateTime
    ): List<ServiceCallStatisticLite.ServiceCall>
}