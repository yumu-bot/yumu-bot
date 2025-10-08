package com.now.nowbot.entity

import com.fasterxml.jackson.databind.JsonNode
import com.now.nowbot.util.JacksonUtil
import jakarta.persistence.*
import org.spring.core.getItem
import java.time.LocalDateTime

@Entity @Table(
    name = "service_call_stat",
    indexes = [Index(name = "index_time", columnList = "time"), Index(name = "index_group", columnList = "time, group_id")]
) data class ServiceCallStatisticLite(
    @Id @GeneratedValue(strategy = GenerationType.SEQUENCE) var id: Long? = null,

    @Column(name = "name") var name: String = "",

    @Column(name = "user_id") var userID: Long = -1L,

    @Column(name = "group_id") var groupID: Long = -1L,

    @Column(name = "time") var createTime: LocalDateTime = LocalDateTime.now(),

    @Column(name = "duration") var duration: Long = -1L,

    @Column(name = "param", columnDefinition = "JSON", nullable = true) var param: String? = null,

    ) {

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
