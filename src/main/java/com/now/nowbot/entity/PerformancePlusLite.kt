package com.now.nowbot.entity

import com.now.nowbot.model.osu.PPPlus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "performance_plus")
class PerformancePlusLite {
    @Id
    @Column(name = "id", nullable = false)
    var id: Long? = null

    /**
     * 暂定 0: score pp+, 1: beatmap pp+
     */
    @Column(name = "type", nullable = false)
    private var type: Short = 0

    private var aim: Double? = null

    @Column(name = "jump")
    private var jumpAim: Double? = null

    @Column(name = "flow")
    private var flowAim: Double? = null

    private var precision: Double? = null

    private var speed: Double? = null

    private var stamina: Double? = null

    private var accuracy: Double? = null

    private var total: Double? = null

    constructor()

    constructor(id: Long?, score: PPPlus.Stats, type: Short) {
        this.id = id
        this.aim = score.aim
        this.jumpAim = score.jumpAim
        this.flowAim = score.flowAim
        this.precision = score.precision
        this.speed = score.speed
        this.stamina = score.stamina
        this.accuracy = score.accuracy
        this.total = score.total
        this.type = type
    }

    fun toStats(): PPPlus.Stats {
        return PPPlus.Stats(aim!!, jumpAim!!, flowAim!!, precision!!, speed!!, stamina!!, accuracy!!, total!!)
    }

    companion object {
        const val SCORE: Short = 0
        const val MAP: Short = 1
    }
}