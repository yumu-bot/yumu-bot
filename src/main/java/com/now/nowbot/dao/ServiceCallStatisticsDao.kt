package com.now.nowbot.dao

import com.now.nowbot.entity.ServiceCallStatisticLite
import com.now.nowbot.mapper.ServiceCallStatisticRepository
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component class ServiceCallStatisticsDao(
    private val serviceCallStatisticRepository: ServiceCallStatisticRepository
) {
    fun saveService(
        data: ServiceCallStatisticLite
    ) = serviceCallStatisticRepository.save(data)

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