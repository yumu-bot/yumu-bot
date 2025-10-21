package com.now.nowbot.dao

import com.now.nowbot.mapper.SkillRepository
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.osu.Beatmap
import com.now.nowbot.model.skill.Skill
import org.springframework.stereotype.Component

@Component
class SkillDao(private val skillRepository: SkillRepository) {
    fun saveAndUpdateSkill(beatmap: Beatmap, mode: OsuMode, skill: Skill) {
        val values = skill.values.take(6).map { it.toDouble() }.toDoubleArray()
        val star = skill.star.toDouble()

        saveAndUpdateSkill(beatmap.beatmapID, mode, values, star)
    }

    fun saveAndUpdateSkill(beatmapID: Long, mode: OsuMode, values: DoubleArray, star: Double) {
        skillRepository.saveAndUpdate(beatmapID, mode.modeValue, values, star)
    }

    fun getSkill(beatmapID: Long, mode: OsuMode): Pair<DoubleArray, Double>? {
        val lite = skillRepository.getSkillByBeatmapIDAndMode(beatmapID, mode.modeValue) ?: return null

        return lite.values to lite.star
    }
}