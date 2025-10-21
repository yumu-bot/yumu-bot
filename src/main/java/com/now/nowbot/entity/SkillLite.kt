package com.now.nowbot.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Table
import java.io.Serializable

@Entity
@Table(name = "osu_skill")
@IdClass(SkillLite.SkillKey::class)
class SkillLite(
    @Column("id")
    @Id
    val id: Long,

    @Column("mode")
    @Id
    val mode: Byte = 0,

    @Column("values", columnDefinition = "double precision[]")
    val values: DoubleArray = doubleArrayOf(),

    @Column("star")
    val star: Double = 0.0
) {
    data class SkillKey(
        var id: Long,
        var mode: Byte,
    ) : Serializable {
        constructor() : this(0, 0)
    }
}