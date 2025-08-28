package com.now.nowbot.model.maimai

import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
import java.time.OffsetDateTime

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class LxChuUser(
    val name: String,
    val level: Byte,
    val rating: Double,
    val ratingPossession: String,
    val friendCode: Long,
    val classEmblem: ClassEmblem,
    val rebornCount: Int,
    val overPower: Double,
    val overPowerProgress: Double,
    val currency: Int,
    val totalPlayCount: Int,
    val trophy: LxCollection,
    val character: LxCollection?,
    val namePlate: LxCollection?,
    val mapIcon: LxCollection?,
    val uploadTime: OffsetDateTime,
) {
    data class ClassEmblem(
        // 缎带
        val base: Int,

        // 勋章
        val medal: Int,
    )
}
