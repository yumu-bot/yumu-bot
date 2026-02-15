package com.now.nowbot.entity

import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.util.JacksonUtil
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType
import jakarta.persistence.*
import org.hibernate.annotations.Type
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

@Entity
@Table(
    name = "service_call_stat",
    indexes = [Index(name = "index_time", columnList = "time"), Index(
        name = "index_group",
        columnList = "time, group_id"
    )]
)
class ServiceCallStatistic(
    @Id @GeneratedValue(strategy = GenerationType.SEQUENCE) var id: Long? = null,

    @Column(name = "name") var name: String = "",

    @Column(name = "user_id") var userID: Long = -1L,

    @Column(name = "group_id") var groupID: Long = -1L,

    @Column(name = "time") var createTime: LocalDateTime = LocalDateTime.now(),

    @Column(name = "duration") var duration: Long = -1L,

    @Type(JsonBinaryType::class)
    @Column(name = "param", columnDefinition = "JSONB", nullable = true) var param: String? = null,

    ) {

    companion object {

        /**
         * 写了个更方便的转换方法
         */
        fun build(
            event: MessageEvent,
            beatmapID: Long? = null,
            beatmapsetID: Long? = null,
            userID: Long? = null,
            mode: OsuMode? = null,
        ): ServiceCallStatistic {
            return builds(event,
                beatmapID?.let { listOf(it) },
                beatmapsetID?.let { listOf(it) },
                userID?.let { listOf(it) },
                mode?.let { listOf(it) }
            )
        }

        /**
         * 写了个更方便的转换法
         */
        fun builds(
            event: MessageEvent,
            beatmapIDs: List<Long>? = null,
            beatmapsetIDs: List<Long>? = null,
            userIDs: List<Long>? = null,
            modes: List<OsuMode>? = null
        ): ServiceCallStatistic {
            val map = mapOf(
                "bids" to beatmapIDs,
                "sids" to beatmapsetIDs,
                "uids" to userIDs,
                "modes" to modes?.map { it.modeValue }
            ).filterNot { it.value.isNullOrEmpty() }

            return if (map.isEmpty()) {
                building(event, null)
            } else {
                building(event, map)
            }
        }

        /**
         * 最原始的记录方法。
         * 如果你需要自由记录数据，或是完全不用记录任何数据，请使用此方法。
         */
        fun building(
            event: MessageEvent,
            data: Any? = null,
            other: ServiceCallStatistic.() -> Unit = {}
        ): ServiceCallStatistic {
            val param = if (data != null) {
                JacksonUtil.toJson(data)
            } else {
                null
            }
            val result = ServiceCallStatistic(
                param = param,
                userID = event.sender.contactID,
                groupID = event.subject.contactID,
            )
            result.other()
            return result
        }

        private val ZONE: ZoneId = ZoneId.systemDefault()
    }

    fun setOther(name: String, time: Long, duration: Long) {
        createTime = Instant.ofEpochMilli(time)
            .atZone(ZONE)
            .toLocalDateTime()
        this.name = name
        this.duration = duration
    }

    fun setParam(data: Any?) {
        if (data != null) {
            param = JacksonUtil.toJson(data)
        }
    }
}

class ServiceHeritage(
    val bids: List<Long>? = null,
    val sids: List<Long>? = null,
    val uids: List<Long>? = null,
    val mids: List<Long>? = null,
)