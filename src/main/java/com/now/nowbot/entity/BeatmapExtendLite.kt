package com.now.nowbot.entity

import com.now.nowbot.util.IntArrayCompressor
import com.now.nowbot.util.JacksonUtil
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.annotations.Type
import org.hibernate.type.SqlTypes
import tools.jackson.databind.JsonNode
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

    @Type(JsonBinaryType::class)
    @Column(name = "fail_times", columnDefinition = "JSONB", nullable = true, updatable = true)
    var failTimes: String? = null,

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

    fun readFailTimesAsNode(): JsonNode? {
        val fail = readFails()
        val exit = readExits()

        if (fail == null && exit == null) return null

        val objectMapper = JacksonUtil.mapper
        val objectNode = objectMapper.createObjectNode()

        fail?.let { objectNode.set("fail", objectMapper.valueToTree(it)) }
        exit?.let { objectNode.set("exit", objectMapper.valueToTree(it)) }

        return objectNode
    }

    fun writeFailTimesFromNode(node: JsonNode?) {
        node?.get("fail")?.let { failNode ->
            if (failNode.isArray) {
                val failArray = IntArray(failNode.size()) { failNode[it].asInt() }
                writeFails(failArray)  // 只有合法时才写入
            }
        }

        node?.get("exit")?.let { exitNode ->
            if (exitNode.isArray) {
                val exitArray = IntArray(exitNode.size()) { exitNode[it].asInt() }
                writeExits(exitArray)  // 只有合法时才写入
            }
        }
    }
}


