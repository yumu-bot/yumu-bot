package com.now.nowbot.entity

import com.fasterxml.jackson.databind.JsonNode
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.util.JacksonUtil
import jakarta.persistence.*
import org.spring.core.getItem
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
class ServiceCallStatisticLite(
    @Id @GeneratedValue(strategy = GenerationType.SEQUENCE) var id: Long? = null,

    @Column(name = "name") var name: String = "",

    @Column(name = "user_id") var userID: Long = -1L,

    @Column(name = "group_id") var groupID: Long = -1L,

    @Column(name = "time") var createTime: LocalDateTime = LocalDateTime.now(),

    @Column(name = "duration") var duration: Long = -1L,

    @Column(name = "param", columnDefinition = "JSONB", nullable = true) var param: String? = null,

    ) {

    companion object {
        fun build(
            e: MessageEvent,
            data: Any? = null,
            other: ServiceCallStatisticLite.() -> Unit = {}
        ): ServiceCallStatisticLite {
            val param = if (data != null) {
                JacksonUtil.toJson(data)
            } else {
                null
            }
            val result = ServiceCallStatisticLite(
                param = param,
                userID = e.sender.id,
                groupID = e.subject.id,
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

    interface ServiceCall {
        val id: Long
        val name: String
        val userID: Long
        val groupID: Long
        val createTime: LocalDateTime
        val duration: Long

        /**
         * 使用 data 获取数据
         */
        val param: String?

        val heritage: ServiceHeritage?
            get() {
                val node = JacksonUtil.parseObject(param ?: return null, JsonNode::class.java)

                return ServiceHeritage(
                    node.getItem<Long>("bid"),
                    node.getItem<Long>("sid"),
                )
            }
    }
}

data class ServiceHeritage(
    val bid: Long? = null, val sid: Long? = null
)