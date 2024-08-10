package com.now.nowbot.mapper

import com.now.nowbot.entity.NewbiePlayCount
import org.springframework.data.jpa.repository.JpaRepository

interface NewbiePlayCountRepository:JpaRepository<NewbiePlayCount, Long> {

}