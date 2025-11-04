package com.now.nowbot.mapper

import com.now.nowbot.entity.OsuUserInfoPercentilesKey
import com.now.nowbot.entity.OsuUserInfoPercentilesLite
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

interface OsuUserInfoPercentilesLiteRepository : JpaRepository<OsuUserInfoPercentilesLite, OsuUserInfoPercentilesKey> {

    @Modifying @Transactional @Query(
        """
        INSERT INTO osu_user_info_percent 
        (user_id, mode, updated_at, global_rank, country_rank, level, rank_count_score, 
         play_count, total_hits, play_time, ranked_score, total_score, 
         beatmap_playcount, replays_watched, maximum_combo, achievements_count) 
        VALUES (:userID, :mode, :updatedAt, :globalRank, :countryRank, :level, :rankCountScore, 
                :playCount, :totalHits, :playTime, :rankedScore, :totalScore, 
                :beatmapPlaycount, :replaysWatched, :maximumCombo, :achievementsCount) 
        ON CONFLICT (user_id, mode) 
        DO UPDATE SET
            updated_at = EXCLUDED.updated_at,
            global_rank = EXCLUDED.global_rank,
            country_rank = EXCLUDED.country_rank,
            level = EXCLUDED.level,
            rank_count_score = EXCLUDED.rank_count_score,
            play_count = EXCLUDED.play_count,
            total_hits = EXCLUDED.total_hits,
            play_time = EXCLUDED.play_time,
            ranked_score = EXCLUDED.ranked_score,
            total_score = EXCLUDED.total_score,
            beatmap_playcount = EXCLUDED.beatmap_playcount,
            replays_watched = EXCLUDED.replays_watched,
            maximum_combo = EXCLUDED.maximum_combo,
            achievements_count = EXCLUDED.achievements_count
        """, nativeQuery = true
    ) fun upsert(
        userID: Long,
        mode: Byte,
        updatedAt: LocalDateTime,
        globalRank: Long?,
        countryRank: Long?,
        level: Int,
        rankCountScore: Int,
        playCount: Long,
        totalHits: Long,
        playTime: Long,
        rankedScore: Long,
        totalScore: Long,
        beatmapPlaycount: Int,
        replaysWatched: Int,
        maximumCombo: Int,
        achievementsCount: Int
    ): Int

    @Query(value = """
        SELECT * FROM osu_user_info_percent WHERE mode = :mode
    """, nativeQuery = true)
    fun findAllByMode(mode: Byte): List<OsuUserInfoPercentilesLite>
}