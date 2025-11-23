package com.now.nowbot.mapper

import com.now.nowbot.entity.OsuUserInfoArchiveLite
import com.now.nowbot.entity.OsuUserInfoArchiveLite.InfoArchive
import com.now.nowbot.model.enums.OsuMode
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

interface OsuUserInfoRepository : JpaRepository<OsuUserInfoArchiveLite, Long>,
    JpaSpecificationExecutor<OsuUserInfoArchiveLite?> {

    @Query(
        value = """
            SELECT country_rank FROM osu_user_info_archive 
            WHERE osu_id = :userID
                AND mode = :mode
                AND country_rank IS NOT NULL 
            ORDER BY time DESC
            LIMIT 1;
        """, nativeQuery = true
    )
    fun getLastCountryRank(userID: Long, mode: OsuMode): Long?

    @Query(
        value = """
                select distinct on (osu_id, mode)
                osu_id, mode as mode_short, play_count
                from (
                    select osu_id, mode, play_count from osu_user_info_archive
                    where osu_id in (:userIDs) and time between :from and :to and pp > 0
                    and osu_id is not null
                    and mode is not null
                    and play_count is not null
                    order by osu_id, mode, play_count asc
                ) as shadow
                """, nativeQuery = true
    ) fun getFromUserIDs(
        userIDs: List<Long>,
        from: LocalDateTime,
        to: LocalDateTime
    ): List<InfoArchive>

    @Query(
        "select o from OsuUserInfoArchiveLite o where o.userID = :userID and o.mode = :mode and (o.time between :from and :to) order by o.time desc limit 1"
    ) fun getLastBetween(
        userID: Long,
        mode: OsuMode,
        from: LocalDateTime,
        to: LocalDateTime
    ): OsuUserInfoArchiveLite?

    @Query(
        "SELECT o FROM OsuUserInfoArchiveLite o " +
                "WHERE (o.time BETWEEN :from AND :to) " +
                "AND o.time = (SELECT MAX(o2.time) FROM OsuUserInfoArchiveLite o2 " +
                "WHERE o2.userID = o.userID AND o2.mode = o.mode " +
                "AND o2.time BETWEEN :from AND :to)"
    ) fun getLastBetween(
        from: LocalDateTime,
        to: LocalDateTime
    ): List<OsuUserInfoArchiveLite>

    @Query("select o from OsuUserInfoArchiveLite o where o.userID = :userID and o.mode = :mode order by o.time desc limit 1")
    fun getLast(userID: Long, mode: OsuMode): OsuUserInfoArchiveLite?

    @Modifying
    @Transactional
    @Query(
        value = """
            DELETE FROM osu_user_info_archive 
            WHERE osu_id = :userID 
                AND mode = :mode 
                AND time >= :from 
                AND time < :to
        """, nativeQuery = true
    )
    fun removeBetween(userID: Long, mode: OsuMode, from: LocalDateTime, to: LocalDateTime)
}
