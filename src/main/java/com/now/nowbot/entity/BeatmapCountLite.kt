package com.now.nowbot.entity

import com.now.nowbot.util.IntArrayCompressor
import com.now.nowbot.util.UUIDConverter
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.util.UUID

@Entity
@Table(
    name = "beatmap_count",
)
class BeatmapCountLite(
    @Id
    @Column(name = "id")
    var beatmapID: Long,

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "hash", columnDefinition = "uuid")
    var hash: UUID? = null,

    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(name = "delta")
    var delta: ByteArray? = null,

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "density", columnDefinition = "int4[]")
    var density: IntArray? = null
) {

    fun readTimestamps(): IntArray? {
        return delta?.let { IntArrayCompressor.byteArrayToIntArray(it) }
    }

    fun writeTimestamps(ints: IntArray?) {
        this.delta = ints?.let { IntArrayCompressor.intArrayToByteArray(it) }
    }

    fun readHash(): String? {
        return hash?.let { UUIDConverter.uuidToMD5(it) }
    }

    fun writeHash(hash: String?) {
        this.hash = hash?.let { UUIDConverter.md5ToUUID(it) }
    }

    interface TimeResult {
        val beatmapID: Long
        val delta: ByteArray
    }

    interface DensityResult {
        val density: IntArray?
        val hash: UUID?
    }
}