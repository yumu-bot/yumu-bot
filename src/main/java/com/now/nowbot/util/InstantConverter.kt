package com.now.nowbot.util
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import kotlin.time.Instant
import kotlin.time.toJavaInstant
import kotlin.time.toKotlinInstant

@Converter(autoApply = true)
class KotlinxInstantConverter : AttributeConverter<Instant, java.time.Instant> {

    override fun convertToDatabaseColumn(attribute: Instant?): java.time.Instant? {
        return attribute?.toJavaInstant()
    }

    override fun convertToEntityAttribute(dbData: java.time.Instant?): Instant? {
        return dbData?.toKotlinInstant()
    }
}