package com.now.nowbot.dao

import com.now.nowbot.entity.ServiceCallStatisticLite
import com.now.nowbot.entity.ServiceHeritage
import com.now.nowbot.mapper.ServiceCallStatisticRepository
import org.spring.core.toJson
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component class ServiceCallStatisticsDao(
    private val serviceCallStatisticRepository: ServiceCallStatisticRepository
) {
    fun saveService(
        name: String,
        userID: Long,
        groupID: Long,
        createTime: LocalDateTime,
        duration: Long,
        serviceHeritage: ServiceHeritage?
    ) {

        val lite = ServiceCallStatisticLite(
            null, name, userID, groupID, createTime, duration, serviceHeritage.toJson()
        )

        serviceCallStatisticRepository.save(lite)
    }

    fun getBetween(
        from: LocalDateTime,
        to: LocalDateTime = LocalDateTime.now()
    ): List<ServiceCallStatisticLite.ServiceCall> {
        return serviceCallStatisticRepository.getBetween(from, to)
    }

    fun getBetweenInGroup(
        groupID: Long,
        from: LocalDateTime,
        to: LocalDateTime = LocalDateTime.now()
    ): List<ServiceCallStatisticLite.ServiceCall> {
        return serviceCallStatisticRepository.getBetweenInGroup(groupID, from, to)
    }

    fun getGroupLastBeatmapsetID(
        groupID: Long,
        from: LocalDateTime,
        to: LocalDateTime = LocalDateTime.now()
    ): Long? {
        val calls = getBetweenInGroup(groupID, from, to)
        
        return calls.mapNotNull { it.heritage }.mapNotNull { it.sid }.lastOrNull()
    }

    fun getGroupLastBeatmapID(
        groupID: Long,
        from: LocalDateTime,
        to: LocalDateTime = LocalDateTime.now()
    ): Long? {
        val calls = getBetweenInGroup(groupID, from, to)

        return calls.mapNotNull { it.heritage }.mapNotNull { it.bid }.lastOrNull()
    }
}