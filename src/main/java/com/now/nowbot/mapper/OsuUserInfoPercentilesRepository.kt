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
         beatmap_playcount, replays_watched, maximum_combo) 
        VALUES (:userID, :mode, :updatedAt, :globalRank, :countryRank, :level, :rankCountScore, 
                :playCount, :totalHits, :playTime, :rankedScore, :totalScore, 
                :beatmapPlaycount, :replaysWatched, :maximumCombo) 
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
            maximum_combo = EXCLUDED.maximum_combo
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
        maximumCombo: Int
    ): Int

    @Query(value = """
    WITH filtered_data AS (
        SELECT * FROM osu_user_info_percent 
        WHERE mode = :mode
        AND global_rank IS NOT NULL AND global_rank > 0
        AND country_rank IS NOT NULL AND country_rank > 0
        AND level > 0
        AND rank_count_score > 0
        AND play_count > 0
        AND total_hits > 0
        AND play_time > 0
        AND ranked_score > 0
        AND total_score > 0
        AND beatmap_playcount > 0
        AND replays_watched > 0
        AND maximum_combo > 0
    ),
    percentile_data AS (
        SELECT 
            ARRAY[
                (1.0 - PERCENT_RANK() OVER (ORDER BY global_rank)),
                (1.0 - PERCENT_RANK() OVER (ORDER BY country_rank)),
                PERCENT_RANK() OVER (ORDER BY level),
                PERCENT_RANK() OVER (ORDER BY rank_count_score),
                PERCENT_RANK() OVER (ORDER BY play_count),
                PERCENT_RANK() OVER (ORDER BY total_hits),
                PERCENT_RANK() OVER (ORDER BY play_time),
                PERCENT_RANK() OVER (ORDER BY ranked_score),
                PERCENT_RANK() OVER (ORDER BY total_score),
                PERCENT_RANK() OVER (ORDER BY beatmap_playcount),
                PERCENT_RANK() OVER (ORDER BY replays_watched),
                PERCENT_RANK() OVER (ORDER BY maximum_combo)
            ] as percentile_array
        FROM filtered_data
        WHERE user_id = :userID
    )
    SELECT UNNEST(percentile_array) 
    FROM percentile_data
""", nativeQuery = true)
    fun getUserPercentileList(userID: Long, mode: Byte): List<Double>
}