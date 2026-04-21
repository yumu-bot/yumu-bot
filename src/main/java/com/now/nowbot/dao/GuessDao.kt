package com.now.nowbot.dao

import com.now.nowbot.entity.GuessLite
import com.now.nowbot.mapper.GuessRepository
import com.now.nowbot.mapper.GuesserRepository
import com.now.nowbot.service.messageServiceImpl.GuessService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class GuessDao(
    private val guessRepository: GuessRepository,

    @Suppress("UNUSED")
    private val guesserRepository: GuesserRepository,
) {

    @Transactional
    fun save(guessGame: GuessService.GuessGame) {
        val lite = GuessLite.from(guessGame)
        if (lite != null) {
            guessRepository.save(lite)
        }
    }

}