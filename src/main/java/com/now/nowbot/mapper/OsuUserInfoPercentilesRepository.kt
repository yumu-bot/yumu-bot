package com.now.nowbot.mapper

import com.now.nowbot.entity.OsuUserInfoPercentilesKey
import com.now.nowbot.entity.OsuUserInfoPercentilesLite
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

interface OsuUserInfoPercentilesLiteRepository : JpaRepository<OsuUserInfoPercentilesLite, OsuUserInfoPercentilesKey> {

    @Modifying
    @Transactional
    @Query(
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
        """,
        nativeQuery = true
    )
    fun upsert(
        userID: Long, mode: Byte, updatedAt: LocalDateTime, globalRank: Long?, countryRank: Long?,
        level: Int, rankCountScore: Int, playCount: Long, totalHits: Long, playTime: Long,
        rankedScore: Long, totalScore: Long, beatmapPlaycount: Int,
        replaysWatched: Int, maximumCombo: Int
    ): Int

    // 方法1：在计算时排除无效数据
    @Query(value = """
        WITH valid_data AS (
            SELECT 
                user_id,
                mode,
                global_rank,
                country_rank,
                level,
                rank_count_score,
                play_count,
                total_hits,
                play_time,
                ranked_score,
                total_score,
                beatmap_playcount,
                replays_watched,
                maximum_combo
            FROM osu_user_info_percent
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
        )
        SELECT 
            user_id,
            mode,
            (1.0 - PERCENT_RANK() OVER (ORDER BY global_rank)) as global_rank_percentile,
            (1.0 - PERCENT_RANK() OVER (ORDER BY country_rank)) as country_rank_percentile,
            PERCENT_RANK() OVER (ORDER BY level) as level_percentile,
            PERCENT_RANK() OVER (ORDER BY rank_count_score) as rank_count_score_percentile,
            PERCENT_RANK() OVER (ORDER BY play_count) as play_count_percentile,
            PERCENT_RANK() OVER (ORDER BY total_hits) as total_hits_percentile,
            PERCENT_RANK() OVER (ORDER BY play_time) as play_time_percentile,
            PERCENT_RANK() OVER (ORDER BY ranked_score) as ranked_score_percentile,
            PERCENT_RANK() OVER (ORDER BY total_score) as total_score_percentile,
            PERCENT_RANK() OVER (ORDER BY beatmap_playcount) as beatmap_playcount_percentile,
            PERCENT_RANK() OVER (ORDER BY replays_watched) as replays_watched_percentile,
            PERCENT_RANK() OVER (ORDER BY maximum_combo) as maximum_combo_percentile
        FROM valid_data
    """, nativeQuery = true)
    fun calculatePercentilesWithValidDataOnly(@Param("mode") mode: Byte): List<Array<Any>>
}