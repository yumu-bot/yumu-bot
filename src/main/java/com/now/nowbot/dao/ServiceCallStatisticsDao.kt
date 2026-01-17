package com.now.nowbot.dao

import com.fasterxml.jackson.databind.JsonNode
import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.entity.ServiceHeritage
import com.now.nowbot.mapper.ServiceCallStatisticRepository
import com.now.nowbot.util.JacksonUtil
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component class ServiceCallStatisticsDao(
    private val serviceCallStatisticRepository: ServiceCallStatisticRepository
) {
    fun saveService(
        data: ServiceCallStatistic
    ) = serviceCallStatisticRepository.save(data)

    fun getBetween(
        from: LocalDateTime,
        to: LocalDateTime = LocalDateTime.now()
    ): List<ServiceCallStatistic> {
        return serviceCallStatisticRepository.getBetween(from, to)
    }

    fun getBetweenInGroup(
        groupID: Long,
        from: LocalDateTime,
        to: LocalDateTime = LocalDateTime.now()
    ): List<ServiceCallStatistic> {
        return serviceCallStatisticRepository.getBetweenInGroup(groupID, from, to)
    }

    fun getLastBeatmapsetID(
        groupID: Long,
        name: String?,
        from: LocalDateTime,
        to: LocalDateTime = LocalDateTime.now()
    ): Long? {
        return if (name.isNullOrEmpty()) {
            serviceCallStatisticRepository.getLastAvailableBeatmapsetIDByGroup(groupID, from, to)
        } else {
            serviceCallStatisticRepository.getLastAvailableBeatmapsetIDByGroupAndName(groupID, name, from, to)
        }
    }

    /*

    fun getLastBeatmapsetID(
        groupID: Long,
        name: String?,
        from: LocalDateTime,
        to: LocalDateTime = LocalDateTime.now()
    ): Long? {
        val calls = getBetweenInGroup(groupID, from, to)
        
        return calls
            .filter { name?.equals(it.name, ignoreCase = true) ?: true }
            .mapNotNull { getHeritage(it.param) }
            .lastOrNull { !it.sids.isNullOrEmpty() }
            ?.sids
            ?.firstOrNull()
    }

     */

    fun getLastMatchID(
        groupID: Long,
        from: LocalDateTime,
        to: LocalDateTime = LocalDateTime.now()
    ): Long? {
        return serviceCallStatisticRepository.getLastAvailableMatchIDByGroup(groupID, from, to)
    }

    fun getLastBeatmapID(
        groupID: Long,
        name: String?,
        from: LocalDateTime,
        to: LocalDateTime = LocalDateTime.now()
    ): Long? {
        return if (name.isNullOrEmpty()) {
            serviceCallStatisticRepository.getLastAvailableBeatmapIDByGroup(groupID, from, to)
        } else {
            serviceCallStatisticRepository.getLastAvailableBeatmapIDByGroupAndName(groupID, name, from, to)
        }
    }

    fun getLastMaiSongID(
        groupID: Long,
        name: String?,
        from: LocalDateTime,
        to: LocalDateTime = LocalDateTime.now()
    ): Long? {
        return if (name.isNullOrEmpty()) {
            serviceCallStatisticRepository.getLastAvailableMaiSongIDByGroup(groupID, from, to)
        } else {
            serviceCallStatisticRepository.getLastAvailableMaiSongIDByGroupAndName(groupID, name, from, to)
        }
    }


    /*

    fun getLastBeatmapID(
        groupID: Long,
        name: String?,
        from: LocalDateTime,
        to: LocalDateTime = LocalDateTime.now()
    ): Long? {
        val calls = getBetweenInGroup(groupID, from, to)

        return calls
            .filter { name?.equals(it.name, ignoreCase = true) ?: true }
            .mapNotNull { getHeritage(it.param) }
            .lastOrNull { !it.bids.isNullOrEmpty() }
            ?.bids
            ?.firstOrNull()
    }

     */

    fun getHeritage(param: String?): ServiceHeritage? {
        if (param == null) return null

        val node = JacksonUtil.parseObject(param, JsonNode::class.java)

        val bids = node.get("bids")?.mapNotNull { it.asLong() }
        val sids = node.get("sids")?.mapNotNull { it.asLong() }
        val uids = node.get("uids")?.mapNotNull { it.asLong() }
        val mids = node.get("mids")?.mapNotNull { it.asLong() }

        return ServiceHeritage(
            bids,
            sids,
            uids,
            mids,
        )
    }
}