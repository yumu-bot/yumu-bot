package com.now.nowbot.dao

import com.now.nowbot.entity.UserBestSnapshot
import com.now.nowbot.mapper.UserBestSnapshotRepository
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.osu.LazerScore
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.util.DigestUtils
import java.time.LocalDateTime

@Component
class UserSnapShotDao(private val snapshotRepository: UserBestSnapshotRepository) {

    fun upsertSnapshotAsync(scores: Collection<LazerScore>) {

        Thread.startVirtualThread {
            try {
                val ss = scores.map { it.copy() }
                upsertSnapshot(ss)
            } catch (e: Exception) {
                log.warn("玩家快照：更新失败\n${e.message}")
            }
        }
    }

    fun upsertSnapshot(scores: Collection<LazerScore>) {
        val f = scores.firstOrNull() ?: return

        val hash = getHash(scores)

        val latest = snapshotRepository.getLatest(f.userID, f.mode.modeValue)

        if (hash != latest?.contentHash && scores.size >= (latest?.scoreIDs?.size ?: 0)) {
            saveSnapshot(scores)
        }
    }

    fun saveSnapshot(scores: Collection<LazerScore>) {
        val snapshot = UserBestSnapshot.fromBests(scores) ?: return

        snapshotRepository.save(snapshot)
    }

    fun getLatestSnapshot(userID: Long, mode: OsuMode): UserBestSnapshot? {
        return snapshotRepository.getLatest(userID, mode.modeValue)
    }

    fun getHash(scores: Collection<LazerScore>): String {
        val currentDataString = scores.joinToString(",") { "${it.beatmapID}:${it.scoreID}" }
        return DigestUtils.md5DigestAsHex(currentDataString.toByteArray())
    }

    fun getCount(userID: Long, mode: OsuMode): Long {
        return snapshotRepository.getCount(userID, mode.modeValue)
    }

    fun getCreatedAt(userID: Long, mode: OsuMode, offset: Int = 0, limit: Int = 5): List<LocalDateTime> {
        return snapshotRepository.getCreatedAt(userID, mode.modeValue, offset, limit)
    }

    fun getWithOffset(userID: Long, mode: OsuMode, offset: Int = 0): UserBestSnapshot? {
        return snapshotRepository.getWithOffset(userID, mode.modeValue, offset).firstOrNull()
    }

    companion object {
        private val log = LoggerFactory.getLogger(UserSnapShotDao::class.java)
    }
}