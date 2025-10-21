package com.now.nowbot.mapper

import com.now.nowbot.entity.SkillLite
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.transaction.annotation.Transactional

interface SkillRepository: JpaRepository<SkillLite, Long> {
    @Query(
        value = """
            SELECT * FROM osu_skill WHERE id = :beatmapID AND mode = :mode;
        """, nativeQuery = true
    )

    fun getSkillByBeatmapIDAndMode(beatmapID: Long, mode: Byte): SkillLite?

    @Transactional
    @Modifying
    @Query("""
        INSERT INTO osu_skill (id, mode, values, star) 
        VALUES(:id, :mode, :values, :star) 
        ON CONFLICT (id, mode)
        DO UPDATE SET values = :values, star = :star;
        """, nativeQuery = true)
    fun saveAndUpdate(id: Long, mode: Byte, values: DoubleArray, star: Double)
}