package com.now.nowbot.dao

import com.now.nowbot.entity.PPMinusLite
import com.now.nowbot.mapper.PPMinusRepository
import com.now.nowbot.model.osu.LazerScore
import com.now.nowbot.model.osu.OsuUser
import com.now.nowbot.util.DataUtil
import org.springframework.stereotype.Component

@Component
class PPMinusDao(private val ppMinusRepository: PPMinusRepository) {
    fun savePPMinus(user: OsuUser, bests: List<LazerScore>): Boolean {
        val history = ppMinusRepository.findByUserID(user.id, user.currentOsuMode.modeValue)

        val aDay: Long = 20 * 60 * 60 * 1000 // 20 小时

        if (history.isNullOrEmpty() || history.maxOf { it.time!! } + aDay < System.currentTimeMillis()) {
            val lite = PPMinusLite().toLite(user, bests)

            ppMinusRepository.save(lite)
            return true
        } else return false
    }

    fun getSurroundingPPMinus(user: OsuUser, bests: List<LazerScore>, delta: Int = 500): List<PPMinusLite> {
        val rawPP = user.pp - DataUtil.getBonusPP(user.pp, bests.map { it.pp })

        val surrounds = ppMinusRepository.findSurroundingByUserID(user.id, rawPP - delta, rawPP + delta, user.currentOsuMode.modeValue)

        return surrounds ?: emptyList()
    }
}