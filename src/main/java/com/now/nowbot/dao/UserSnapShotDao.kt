package com.now.nowbot.dao

import com.now.nowbot.entity.UserBestSnapshot
import com.now.nowbot.mapper.UserBestSnapshotRepository
import com.now.nowbot.model.osu.LazerScore
import org.springframework.stereotype.Component
import org.springframework.util.DigestUtils

@Component
class UserSnapShotDao(private val snapshotRepository: UserBestSnapshotRepository) {

    fun upsertSnapshot(scores: Collection<LazerScore>) {
        val f = scores.firstOrNull() ?: return

        val currentDataString = scores.joinToString(",") { "${it.beatmapID}:${it.scoreID}" }

        val hash = DigestUtils.md5DigestAsHex(currentDataString.toByteArray())

        val latest = snapshotRepository.getLatest(f.userID, f.mode.modeValue)

        if (hash != latest?.contentHash && scores.size >= (latest?.scoreIDs?.size ?: 0)) {
            Thread.startVirtualThread {
                saveSnapshot(scores)
            }
        }
    }

    fun saveSnapshot(scores: Collection<LazerScore>) {
        val snapshot = UserBestSnapshot.fromBests(scores) ?: return

        snapshotRepository.save(snapshot)
    }
}