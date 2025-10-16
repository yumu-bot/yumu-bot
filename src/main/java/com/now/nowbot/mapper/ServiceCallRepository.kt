package com.now.nowbot.mapper

import com.now.nowbot.entity.ServiceCallLite
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.LocalDateTime

interface ServiceCallRepository : JpaRepository<ServiceCallLite, Long> {

    @Query(
        value = """
            SELECT * FROM service_call
            WHERE ctime BETWEEN :from AND :to;
            """, nativeQuery = true
    ) fun getBetween(
        from: LocalDateTime, to: LocalDateTime
    ): List<ServiceCallLite>
}