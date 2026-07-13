package com.now.nowbot.entity

import com.now.nowbot.model.osu.Beatmap
import com.now.nowbot.util.IntArrayCompressor
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.annotations.Type
import org.hibernate.type.SqlTypes
import java.time.LocalDateTime

/**
 * 鉴于现在的 Beatmap 各种类缺失太多太多数据，先开设一个新类，用来存可以把 Beatmap 变成 BeatmapExtended 的数据
 */

@Entity
@Table(name = "osu_extend_beatmap", indexes = [
    Index(name = "index_beatmapset_id", columnList = "beatmapset_id"),
    Index(name = "index_updated_at", columnList = "updated_at")
])
class BeatmapExtendLite(
    @Id
    @Column(name = "beatmap_id")
    val beatmapID: Long = -1,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "beatmapset_id")
    val beatmapset: BeatmapsetExtendLite,

    @Column(name = "lazer_only", nullable = false, updatable = true)
    var lazerOnly: Boolean = false,

    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(name = "fails", nullable = true, updatable = true)
    var fails: ByteArray? = null,

    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(name = "exits", nullable = true, updatable = true)
    var exits: ByteArray? = null,

    @Column(name = "max_combo", nullable = false, updatable = true)
    val maxCombo: Int = 0,

    @Type(JsonBinaryType::class)
    @Column(name = "owners", columnDefinition = "JSONB", nullable = true, updatable = true)
    var owners: String? = null,

    @Column(name = "created_at", updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false, updatable = true)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {

    fun readFails(): IntArray? {
        return fails?.let { IntArrayCompressor.byteArrayToIntArray(it) }
    }

    fun writeFails(ints: IntArray?) {
        this.fails = ints?.let { IntArrayCompressor.intArrayToByteArray(it) }
    }

    fun readExits(): IntArray? {
        return exits?.let { IntArrayCompressor.byteArrayToIntArray(it) }
    }

    fun writeExits(ints: IntArray?) {
        this.exits = ints?.let { IntArrayCompressor.intArrayToByteArray(it) }
    }

    fun readFailTimesAsData(): Beatmap.FailTimesData? {
        val fail = readFails()
        val exit = readExits()

        if (fail == null || exit == null) return null

        return Beatmap.FailTimesData(fail, exit)
    }

    fun writeFailTimesFromData(data: Beatmap.FailTimesData?) {
        if (data == null) return

        writeFails(data.retries)
        writeExits(data.fails)
    }
}


