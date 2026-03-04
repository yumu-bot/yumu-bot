package com.now.nowbot.entity

import com.now.nowbot.model.osu.PPPlus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.io.Serializable

@Entity
@Table(
    name = "pp_plus",
    indexes = [
        // 1. 优化 beatmapID 的查询，常用于 scoreID=0 的谱面信息检索
        Index(name = "idx_pp_plus_score_beatmap", columnList = "score_id, id"),

        // 2. 优化针对用户 ID 的成绩统计查询
        Index(name = "idx_pp_plus_user_score", columnList = "user_id, score_id")
    ]
)
@IdClass(PerformancePlusStatsLite.PerformancePlusStatsKey::class)
class PerformancePlusStatsLite(
    @Id
    @Column(name = "id", nullable = false)
    var beatmapID: Long,

    /**
     *  -1 是 difficulty
     *  -2 是 performance
     *
     *  >0，则是对应成绩是 performance
     */
    @Id
    @Column(name = "score_id", nullable = false)
    var scoreID: Long,

    @Column(name = "user_id", nullable = true)
    var userID: Long? = null,

    var aim: Float = 0f,

    var jump: Float = 0f,

    var flow: Float = 0f,

    var precision: Float = 0f,

    var speed: Float = 0f,

    var stamina: Float = 0f,

    var accuracy: Float = 0f,

    var total: Float = 0f,
) {
    data class PerformancePlusStatsKey(
        val beatmapID: Long = 0L,
        val scoreID: Long = 0L,
    ) : Serializable

    fun toModel(): PPPlus.Stats {
        return PPPlus.Stats(
            aim.toDouble(),
            jump.toDouble(),
            flow.toDouble(),
            precision.toDouble(),
            speed.toDouble(),
            stamina.toDouble(),
            accuracy.toDouble(),
            total.toDouble()
        )
    }

    companion object {
        fun fromScore(userID: Long, scoreID: Long, beatmapID: Long, performance: PPPlus.Stats): PerformancePlusStatsLite {
            return PerformancePlusStatsLite(
                beatmapID = beatmapID,
                scoreID = scoreID,
                userID = userID,
            ).apply {
                applyStats(performance)
            }
        }

        fun fromBeatmap(beatmapID: Long, performance: PPPlus.Stats, difficulty: PPPlus.Stats): List<PerformancePlusStatsLite> {
            val entities = ArrayList<PerformancePlusStatsLite>(2)

            entities.add(PerformancePlusStatsLite(
                beatmapID = beatmapID,
                scoreID = -1L,
                userID = null
            ).apply {
                applyStats(difficulty)
            })

            entities.add(PerformancePlusStatsLite(
                beatmapID = beatmapID,
                scoreID = -2L,
                userID = null
            ).apply {
                applyStats(performance)
            })

            return entities
        }

        fun PerformancePlusStatsLite.applyStats(stats: PPPlus.Stats) {
            this.aim = stats.aim.toFloat()
            this.jump = stats.jumpAim.toFloat()
            this.flow = stats.flowAim.toFloat()
            this.precision = stats.precision.toFloat()
            this.speed = stats.speed.toFloat()
            this.stamina = stats.stamina.toFloat()
            this.accuracy = stats.accuracy.toFloat()
            this.total = stats.total.toFloat()
        }
    }
}