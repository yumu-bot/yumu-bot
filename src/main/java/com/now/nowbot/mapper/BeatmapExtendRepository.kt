package com.now.nowbot.mapper

import com.now.nowbot.entity.BeatmapExtendLite
import com.now.nowbot.entity.BeatmapsetExtendLite
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Repository
interface BeatmapExtendRepository: JpaRepository<BeatmapExtendLite, Long> {

    // 使用 JOIN FETCH 同时加载 beatmapset
    @Query("SELECT b FROM BeatmapExtendLite b LEFT JOIN FETCH b.beatmapset WHERE b.beatmapID = :beatmapID")
    fun findByBeatmapID(beatmapID: Long): BeatmapExtendLite?

    @Query("SELECT DISTINCT bs FROM BeatmapsetExtendLite bs LEFT JOIN FETCH bs.beatmaps WHERE bs.beatmapsetID IN :beatmapsetIDs")
    fun findByBeatmapsetID(beatmapsetID: Long): List<BeatmapExtendLite>

    @Query(value = "SELECT EXISTS(SELECT 1 FROM osu_extend_beatmap WHERE beatmap_id = :beatmapID)", nativeQuery = true)
    fun existsByBeatmapID(@Param("beatmapID") beatmapID: Long): Boolean

    // 按更新时间升序获取需要更新的记录
    @Query(value = "SELECT e FROM BeatmapExtendLite e WHERE e.updatedAt < :time ORDER BY e.updatedAt ASC LIMIT :limit")
    fun findByUpdateAtAscend(time: LocalDateTime, limit: Int): List<BeatmapExtendLite>

    // 批量更新字段
    @Modifying
    @Transactional
    @Query("UPDATE osu_extend_beatmap SET lazer_only = :lazerOnly, fail_times = CAST(:fail AS jsonb), owners = CAST(:owners AS jsonb), updated_at = CURRENT_TIMESTAMP WHERE beatmap_id = :beatmapID", nativeQuery = true)
    fun updateFailTimeByBeatmapID(@Param("beatmapID") beatmapID: Long, @Param("lazerOnly") lazerOnly: Boolean, @Param("fail") fail: String?, @Param("owners") owners: String?): Int

    @Modifying
    @Transactional
    @Query("DELETE FROM osu_extend_beatmap b WHERE beatmap_id = :beatmapID", nativeQuery = true)
    fun deleteByBeatmapID(@Param("beatmapID") beatmapID: Long)
}

@Repository
interface BeatmapsetExtendLiteRepository : JpaRepository<BeatmapsetExtendLite, Long> {

    @Query(value = "SELECT * FROM osu_extend_beatmapset WHERE beatmapset_id = :beatmapsetID LIMIT 1", nativeQuery = true)
    fun findByBeatmapsetID(beatmapsetID: Long): BeatmapsetExtendLite?

    @Query(value = "SELECT EXISTS(SELECT 1 FROM osu_extend_beatmapset WHERE beatmapset_id = :beatmapsetID)", nativeQuery = true)
    fun existsByBeatmapsetID(@Param("beatmapsetID") beatmapsetID: Long): Boolean

    // 按创建时间升序获取需要更新的记录
    @Query(value = "SELECT e FROM BeatmapsetExtendLite e WHERE e.updatedAt < :time ORDER BY e.updatedAt ASC LIMIT :limit")
    fun findByUpdateAtAscend(time: LocalDateTime, limit: Int): List<BeatmapsetExtendLite>

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM BeatmapsetExtendLite WHERE beatmapsetID IN :beatmapIDs")
    fun deleteAllByBeatmapIDs(beatmapIDs: Iterable<Long>)

    // 批量更新字段
    @Modifying
    @Transactional
    @Query("""
        UPDATE osu_extend_beatmapset SET 
        anime_cover = :animeCover,
        favourite_count = :favouriteCount,
        recommend_offset = :recommendOffset,
        play_count = :playCount,
        spotlight = :spotlight,
        track_id = :trackID,
        discussion_locked = :discussionLocked,
        rating = :rating,
        ratings = :ratings,
        updated_at = CURRENT_TIMESTAMP
        WHERE beatmapset_id = :beatmapsetID
        """, nativeQuery = true)
    fun updateFailTimeByBeatmapsetID(@Param("beatmapsetID") beatmapsetID: Long, animeCover: Boolean, favouriteCount: Long, recommendOffset: Short, playCount: Long, spotlight: Boolean, trackID: Int?, discussionLocked: Boolean, rating: Float, ratings: Array<Int>): Int

    @Modifying
    @Transactional
    @Query(
        value = """
            INSERT INTO osu_extend_beatmapset (
                beatmapset_id, anime_cover, artist, artist_unicode, cover_cache, creator,
                favourite_count, genre_id, user_id, language_id, nsfw, recommend_offset,
                play_count, source, status, spotlight, title, title_unicode, track_id,
                video, bpm, discussion_locked, last_updated, legacy_thread_url_id, nominations_current,
                nominations_rulesets, nominations_required_main, nominations_required_secondary,
                ranked, ranked_date, rating, storyboard, submitted_date, tags,
                availability_download_disabled, availability_more_information, ratings
            ) VALUES (
                :#{#s.beatmapsetID}, :#{#s.animeCover}, :#{#s.artist}, :#{#s.artistUnicode}, :#{#s.coverID}, :#{#s.creator},
                :#{#s.favouriteCount}, :#{#s.genreID}, :#{#s.creatorID}, :#{#s.languageID}, :#{#s.nsfw}, :#{#s.recommendOffset},
                :#{#s.playCount}, :#{#s.source}, :#{#s.status}, :#{#s.spotlight}, :#{#s.title}, :#{#s.titleUnicode}, :#{#s.trackID},
                :#{#s.video}, :#{#s.bpm}, :#{#s.discussionLocked}, :#{#s.lastUpdated}, :#{#s.threadID}, :#{#s.nominationsCurrent},
                :#{#s.nominationsRulesets}, :#{#s.nominationsRequiredMain}, :#{#s.nominationsRequiredSecondary},
                :#{#s.ranked}, :#{#s.rankedDate}, :#{#s.rating}, :#{#s.storyboard}, :#{#s.submittedDate}, :#{#s.tags},
                :#{#s.downloadDisabled}, :#{#s.moreInformation}, :#{#s.ratings}
            )
            ON CONFLICT (beatmapset_id) DO NOTHING
        """,
        nativeQuery = true
    )
    fun insertIfNotExists(@Param("s") s: BeatmapsetExtendLite): Int

}