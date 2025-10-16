package com.now.nowbot.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity @Table(name = "service_call")
class ServiceCallLite {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column(name = "service")
    var name: String = ""

    @Column(name = "time")
    var duration: Long = 0L

    @Column(name = "ctime")
    var createTime: LocalDateTime = LocalDateTime.now()

    companion object {
        fun ServiceCallLite.toStatistic(): ServiceCallStatistic {
            return ServiceCallStatistic(
                id = id,
                name = name,
                duration = duration,
                createTime = createTime,
            )
        }
    }
}
