package com.now.nowbot.mapper

import com.now.nowbot.entity.UserGlobalRankKey
import com.now.nowbot.entity.UserGlobalRankLite
import com.now.nowbot.entity.UserInfoLite
import com.now.nowbot.entity.UserPlayCountProjection
import com.now.nowbot.entity.UserRankModeProjection
import com.now.nowbot.entity.UserRankPercentKey
import com.now.nowbot.entity.UserRankPercentLite
import com.now.nowbot.entity.UserStatisticsLite
import jakarta.persistence.QueryHint
import org.hibernate.jpa.HibernateHints.HINT_FETCH_SIZE
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.QueryHints
import org.springframework.data.repository.query.Param
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.stream.Stream

interface UserInfoRepository : JpaRepository<UserInfoLite, Long> {
    @Query("""
        select * from user_info
        where user_id = :userID and mode = :mode
        order by updated_at desc limit 1
    """, nativeQuery = true)
    fun getLatest(
        userID: Long,
        mode: Byte
    ): UserInfoLite?

    @Query("""
        select * from user_info
        where user_id = :userID and mode = :mode and updated_at between :from and :to
        order by updated_at desc limit 1
    """, nativeQuery = true)
    fun getLatestBetween(
        userID: Long,
        mode: Byte,
        from: LocalDate,
        to: LocalDate
    ): UserInfoLite?

    @Query(
        value = """
    SELECT DISTINCT ON (user_id, mode) *
    FROM user_info
    WHERE updated_at BETWEEN :from AND :to
    ORDER BY user_id, mode, updated_at DESC
""",
        nativeQuery = true
    )
    fun getLatestBetween(
        from: LocalDate,
        to: LocalDate
    ): List<UserInfoLite>

    @Modifying
    @Transactional
    @Query(
        """
    INSERT INTO user_info
    (id, user_id, beatmap_playcount, achievements_count, created_at, updated_at, mode)
    VALUES
    (:#{#entity.id}, :#{#entity.userID}, :#{#entity.beatmapPlaycount}, :#{#entity.achievementsCount}, :#{#entity.createdAt}, :#{#entity.updatedAt}, :#{#entity.mode})
    ON CONFLICT (id) 
    DO UPDATE SET
        user_id = EXCLUDED.user_id,
        beatmap_playcount = EXCLUDED.beatmap_playcount,
        achievements_count = EXCLUDED.achievements_count,
        updated_at = EXCLUDED.updated_at,
        mode = EXCLUDED.mode
        -- 注意：这里同样不写 created_at = EXCLUDED.created_at，从而保留该自增 ID 被首次创建时的时间
    """, nativeQuery = true
    )
    fun upsert(@Param("entity") entity: UserInfoLite): Int

    @Transactional
    @Modifying
    @Query("""
        UPDATE user_info SET updated_at = :updatedAt WHERE id = :entityID
    """, nativeQuery = true)
    fun update(@Param("entityID") entityID: Long, @Param("updatedAt") updatedAt: LocalDate): Int

    @Query(
        value = """
        SELECT * FROM user_info 
        WHERE user_id = :userID AND mode = :mode
        ORDER BY 
            CASE 
                -- 情况 1: targetDate 在区间内，距离为 0
                WHEN CAST(:targetDate AS DATE) BETWEEN created_at AND updated_at THEN 0
                -- 情况 2: targetDate 在区间左侧，计算到 created_at 的距离
                WHEN CAST(:targetDate AS DATE) < created_at THEN ABS(created_at - CAST(:targetDate AS DATE))
                -- 情况 3: targetDate 在区间右侧，计算到 updated_at 的距离
                ELSE ABS(updated_at - CAST(:targetDate AS DATE))
            END
        LIMIT 1
        """,
        nativeQuery = true
    )
    fun getClosestFromDateRange(
        userID: Long,
        mode: Byte,
        targetDate: LocalDate
    ): UserInfoLite?
}

interface UserStatisticsRepository: JpaRepository<UserStatisticsLite, Long> {

    @Query("""
        select * from user_statistics
        where user_id = :userID and mode = :mode
        order by updated_at desc limit 1
    """, nativeQuery = true)
    fun getLatest(
        userID: Long,
        mode: Byte
    ): UserStatisticsLite?

    @Query("""
        select * from user_statistics
        where user_id = :userID and mode = :mode and updated_at between :from and :to
        order by updated_at desc limit 1
    """, nativeQuery = true)
    fun getLatestBetween(
        userID: Long,
        mode: Byte,
        from: LocalDate,
        to: LocalDate
    ): UserStatisticsLite?

    @Query("""
        SELECT DISTINCT ON (mode) 
               user_id AS userID, 
               play_count AS playCount, 
               mode 
        FROM user_statistics 
        WHERE user_id = :userID 
        AND updated_at BETWEEN :from AND :to
        ORDER BY mode, updated_at DESC
    """, nativeQuery = true
    )
    fun getLatestPlayCounts(
        userID: Long,
        from: LocalDate,
        to: LocalDate
    ): List<UserPlayCountProjection>

    @Query(
        value = """
    SELECT DISTINCT ON (user_id, mode) 
           user_id AS userID, 
           play_count AS playCount, 
           mode 
    FROM user_statistics 
    WHERE user_id = :userID 
      AND updated_at BETWEEN :from AND :to
    ORDER BY user_id, mode, updated_at DESC
""", nativeQuery = true
    )
    fun getLatestPlayCounts(
        userID: Long,
        mode: Byte
    ): List<UserPlayCountProjection>

    @Query(
        value = """
    SELECT DISTINCT ON (user_id, mode) *
    FROM user_statistics
    WHERE updated_at BETWEEN :from AND :to
    ORDER BY user_id, mode, updated_at DESC
""",
        nativeQuery = true
    )
    fun getLatestBetween(
        from: LocalDate,
        to: LocalDate
    ): List<UserStatisticsLite>

    @QueryHints(value = [QueryHint(name = HINT_FETCH_SIZE, value = "1000")])
    @Query(value = """
        SELECT DISTINCT ON (user_id, mode) * 
        FROM user_statistics 
        WHERE updated_at BETWEEN :from AND :to
        ORDER BY user_id, mode, updated_at DESC
    """, nativeQuery = true)
    fun streamByUpdatedAtBetween(from: LocalDate, to: LocalDate): Stream<UserStatisticsLite>

    @Query(
        value = """
        (
            SELECT * FROM user_statistics 
            WHERE user_id = :userID AND mode = :mode AND pp >= :target
            ORDER BY pp
            LIMIT 1
        )
        UNION ALL
        (
            SELECT * FROM user_statistics 
            WHERE user_id = :userID AND mode = :mode AND pp < :target
            ORDER BY pp DESC
            LIMIT 1
        )
        ORDER BY ABS(pp - :target)
        LIMIT 1
    """,
        nativeQuery = true
    )
    fun getClosestFromTarget(
        userID: Long,
        mode: Byte,
        target: Double
    ): UserStatisticsLite?

    @Modifying
    @Transactional
    @Query("""
    INSERT INTO user_statistics
    (id, user_id, mode, ranked_score, total_score, total_hits, play_time, play_count, pp, accuracy, 
     grade_ssh, grade_ss, grade_sh, grade_s, grade_a, 
     replays_watched, max_combo, level_current, level_progress, 
     created_at, updated_at) 
    VALUES 
    (:#{#entity.id}, :#{#entity.userID}, :#{#entity.mode}, :#{#entity.rankedScore}, :#{#entity.totalScore}, :#{#entity.totalHits}, :#{#entity.playTime}, :#{#entity.playCount}, :#{#entity.pp}, :#{#entity.accuracy}, 
     :#{#entity.countSSH}, :#{#entity.countSS}, :#{#entity.countSH}, :#{#entity.countS}, :#{#entity.countA}, 
     :#{#entity.replaysWatched}, :#{#entity.maxCombo}, :#{#entity.levelCurrent}, :#{#entity.levelProgress}, 
     :#{#entity.createdAt}, :#{#entity.updatedAt}) 
    ON CONFLICT (id)
    DO UPDATE SET 
        ranked_score = EXCLUDED.ranked_score,
        total_score = EXCLUDED.total_score,
        total_hits = EXCLUDED.total_hits,
        play_time = EXCLUDED.play_time,
        play_count = EXCLUDED.play_count,
        pp = EXCLUDED.pp,
        accuracy = EXCLUDED.accuracy,
        grade_ssh = EXCLUDED.grade_ssh,
        grade_ss = EXCLUDED.grade_ss,
        grade_sh = EXCLUDED.grade_sh,
        grade_s = EXCLUDED.grade_s,
        grade_a = EXCLUDED.grade_a,
        replays_watched = EXCLUDED.replays_watched,
        max_combo = EXCLUDED.max_combo,
        level_current = EXCLUDED.level_current,
        level_progress = EXCLUDED.level_progress,
        updated_at = EXCLUDED.updated_at
    """, nativeQuery = true
    )
    fun upsert(@Param("entity") entity: UserStatisticsLite): Int

    @Transactional
    @Modifying
    @Query("""
        UPDATE user_statistics SET updated_at = :updatedAt WHERE id = :entityID
    """, nativeQuery = true)
    fun update(@Param("entityID") entityID: Long, @Param("updatedAt") updatedAt: LocalDate): Int

    @Query(value = """
        SELECT * FROM (
            SELECT s.*, 
                   ROW_NUMBER() OVER(PARTITION BY s.user_id ORDER BY s.updated_at DESC) as rn
            FROM user_statistics s
            WHERE s.user_id IN (:userIDs) AND s.mode = :mode
        ) t WHERE t.rn = 1
    """, nativeQuery = true)
    fun getLatestBatch(userIDs: Collection<Long>, mode: Byte): List<UserStatisticsLite>

    @Query(value = """
    SELECT s.* 
    FROM user_statistics s
    JOIN (
        SELECT id,
               ROW_NUMBER() OVER(
                   PARTITION BY user_id, mode 
                   ORDER BY updated_at DESC, id DESC
               ) as rn
        FROM user_statistics
        WHERE user_id IN (:userIDs) 
          AND updated_at >= :from
    ) t ON s.id = t.id
    WHERE t.rn = 1 
      AND s.updated_at <= :to
""", nativeQuery = true)
    fun getLatestBatchBetween(userIDs: Collection<Long>, from: LocalDate, to: LocalDate): List<UserStatisticsLite>


    /**
     * 这里的逻辑是，如果有早于 from 的数据，就不返回
     * 如果没有，就返回位于最新的那一条
     */
    @Query(value = """
    SELECT s.* 
    FROM user_statistics s
    JOIN (
        SELECT id,
               ROW_NUMBER() OVER(
                   PARTITION BY user_id, mode 
                   ORDER BY 
                       -- 规则 1：符合 >= from 的未来数据权重最高（0），过去的数据权重低（1）
                       CASE WHEN updated_at >= :from THEN 0 ELSE 1 END ,
                       -- 规则 2：如果是未来数据（权重0），按时间从小到大（ASC）排，谁离 from 近谁在前面
                       --        如果是过去数据（权重1），按时间从大到小（DESC）排，谁离 from 近谁在前面
                       CASE WHEN updated_at >= :from THEN updated_at END NULLS LAST,
                       CASE WHEN updated_at < :from THEN updated_at END DESC NULLS LAST,
                       id DESC
               ) as rn
        FROM user_statistics
        WHERE user_id IN (:userIDs)
    ) t ON s.id = t.id
    WHERE t.rn = 1
""", nativeQuery = true)
    fun getLatestBatchFrom(userIDs: Collection<Long>, from: LocalDate): List<UserStatisticsLite>

    // 2. 批量更新日期：用于 totalHits 没有变化，只需更新 updatedAt 的场景
    @Transactional
    @Modifying
    @Query("UPDATE UserStatisticsLite SET updatedAt = :today WHERE id IN :ids")
    fun batchUpdateTime(ids: Collection<Long>, today: LocalDate)
}

interface UserGlobalRankRepository: JpaRepository<UserGlobalRankLite, UserGlobalRankKey> {
    @Query("""
        select * from user_rank
        where user_id = :userID and mode = :mode
        order by date desc limit 1
    """, nativeQuery = true)
    fun getLatest(
        userID: Long,
        mode: Byte
    ): UserGlobalRankLite?

    @Query("""
        select * from user_rank
        where user_id = :userID and mode = :mode and date between :from and :to
        order by date desc
    """, nativeQuery = true)
    fun getBetween(
        userID: Long,
        mode: Byte,
        from: LocalDate,
        to: LocalDate
    ): List<UserGlobalRankLite>

    @Query(value = """
    SELECT * FROM user_rank r
    WHERE r.user_id IN :userIDs AND r.mode = :mode
    AND (r.user_id, r.date) IN (
        SELECT user_id, MAX(date) 
        FROM user_rank 
        WHERE user_id IN :userIDs AND mode = :mode 
        GROUP BY user_id
    )
""", nativeQuery = true)
    fun getLatest(
        @Param("userIDs") userIDs: Collection<Long>,
        @Param("mode") mode: Byte
    ): List<UserGlobalRankLite>

    @Query("""
        SELECT date FROM user_rank 
        WHERE user_id = :userID AND mode = :mode AND date BETWEEN :startDate AND :endDate
    """, nativeQuery = true)
    fun getDateBetween(
        userID: Long,
        mode: Byte,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<LocalDate>

    @Query(
        value = """
        SELECT * FROM (
            (
                SELECT * FROM user_rank 
                WHERE user_id = :userID AND mode = :mode AND date >= :targetDate
                ORDER BY date
                LIMIT 1
            )
            UNION ALL
            (
                SELECT * FROM user_rank 
                WHERE user_id = :userID AND mode = :mode AND date <= :targetDate
                ORDER BY date DESC
                LIMIT 1
            )
        ) AS subquery
        ORDER BY ABS(subquery.date - CAST(:targetDate AS DATE))
        LIMIT 1
        """,
        nativeQuery = true
    )
    fun getClosestFromDate(
        userID: Long,
        mode: Byte,
        targetDate: LocalDate
    ): UserGlobalRankLite?
}

interface UserRankPercentRepository: JpaRepository<UserRankPercentLite, UserRankPercentKey> {
    @Query("""
        SELECT DISTINCT ON (user_id, mode) user_id, mode, country_rank 
        FROM user_rank_percent 
        WHERE user_id IN :userIDs
        ORDER BY user_id, mode, date DESC;
    """,
        nativeQuery = true
    )
    fun getLatestCountryRanks(userIDs: Collection<Long>): List<UserRankModeProjection>

    @Query("""
        select * from user_rank_percent
        where user_id = :userID and mode = :mode
        order by date desc limit 1
    """, nativeQuery = true)
    fun getLatest(
        userID: Long,
        mode: Byte
    ): UserRankPercentLite?

    @Query("""
        select * from user_rank_percent
        where user_id = :userID and mode = :mode and date between :from and :to
        order by date desc limit 1
    """, nativeQuery = true)
    fun getLatestBetween(
        userID: Long,
        mode: Byte,
        from: LocalDate,
        to: LocalDate
    ): UserRankPercentLite?

    @Query(value = """
    SELECT * FROM user_rank_percent r
    WHERE r.user_id IN :userIDs AND r.mode = :mode
    AND (r.user_id, r.date) IN (
        SELECT user_id, MAX(date) 
        FROM user_rank_percent 
        WHERE user_id IN :userIDs AND mode = :mode 
        GROUP BY user_id
    )
""", nativeQuery = true)
    fun getLatest(
        @Param("userIDs") userIDs: Collection<Long>,
        @Param("mode") mode: Byte
    ): List<UserRankPercentLite>

    @Modifying
    @Transactional
    @Query(
        """
        INSERT INTO user_rank_percent
        (user_id, mode, date, rank_percent, country_rank)
        VALUES
        (:#{#entity.userID}, :#{#entity.mode}, :#{#entity.date}, :#{#entity.globalRankPercent}, :#{#entity.countryRank})
        ON CONFLICT (user_id, mode, date) 
        DO UPDATE SET
            rank_percent = EXCLUDED.rank_percent,
            country_rank = EXCLUDED.country_rank
        """, nativeQuery = true
    )
    fun upsert(@Param("entity") entity: UserRankPercentLite): Int

    @Query(value = """
        SELECT t.* FROM (
            SELECT s.*, 
                   ROW_NUMBER() OVER(PARTITION BY s.user_id ORDER BY s.date DESC) as rn
            FROM user_rank_percent s
            WHERE s.user_id IN (:userIDs) 
              AND s.mode = :mode
        ) t WHERE t.rn = 1
    """, nativeQuery = true)
    fun getLatestBatch(userIDs: Collection<Long>, mode: Byte): List<UserRankPercentLite>

    @Query(
        value = """
        SELECT * FROM (
            (
                SELECT * FROM user_rank_percent 
                WHERE user_id = :userID AND mode = :mode AND date >= :targetDate
                ORDER BY date
                LIMIT 1
            )
            UNION ALL
            (
                SELECT * FROM user_rank_percent 
                WHERE user_id = :userID AND mode = :mode AND date <= :targetDate
                ORDER BY date DESC
                LIMIT 1
            )
        ) AS subquery
        ORDER BY ABS(subquery.date - CAST(:targetDate AS DATE))
        LIMIT 1
        """,
        nativeQuery = true
    )
    fun getClosestFromDate(
        userID: Long,
        mode: Byte,
        targetDate: LocalDate
    ): UserRankPercentLite?
}