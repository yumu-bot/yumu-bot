package com.now.nowbot.mapper

import com.now.nowbot.entity.NewbiePlayCount
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface NewbiePlayCountRepository:JpaRepository<NewbiePlayCount, Long> {
    fun findAllByDate(date: LocalDate): List<NewbiePlayCount>
}