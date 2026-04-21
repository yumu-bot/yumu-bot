package com.now.nowbot.mapper

import com.now.nowbot.entity.GuessLite
import com.now.nowbot.entity.GuesserLite
import org.springframework.data.jpa.repository.JpaRepository

interface GuessRepository: JpaRepository<GuessLite, Long>

interface GuesserRepository: JpaRepository<GuesserLite, Long>