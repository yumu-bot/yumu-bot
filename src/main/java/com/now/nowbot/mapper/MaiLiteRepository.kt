package com.now.nowbot.mapper

import com.now.nowbot.entity.*
import jakarta.transaction.Transactional
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface MaiSongLiteRepository : JpaRepository<MaiSongLite, Int> {
    fun findByQueryTitleLikeIgnoreCase(queryTitle: String): List<MaiSongLite>?
}

interface MaiChartLiteRepository : JpaRepository<MaiChartLite, Int>

interface MaiFitChartLiteRepository : JpaRepository<MaiFitChartLite, Int> {
    @Query("select f.id from com.now.nowbot.entity.MaiFitChartLite f where f.songID = :songID and f.sort = :sort")
    fun findBySongIDAndSort(songID: Int, sort: Int): Int?

    fun findMaiFitChartLitesBySongIDOrderBySortAsc(songID: Int): List<MaiFitChartLite>
}

interface MaiFitDiffLiteRepository : JpaRepository<MaiFitDiffLite, String>

interface MaiRankLiteRepository : JpaRepository<MaiRankingLite, String> {
    // @Query("INSERT INTO maimai_rank (name, rating) VALUES (:name, :rating) ON DUPLICATE KEY UPDATE name = VALUES (name), rating = VALUES (rating)", nativeQuery = true)
    // 司马了 PSQL，语法不一样
    @Transactional
    @Modifying
    @Query("""
        INSERT INTO maimai_rank (name, rating) 
        VALUES(:name, :rating) 
        ON CONFLICT (name)
        DO UPDATE SET name = :name, rating = :rating;
        """, nativeQuery = true)
    fun saveAndUpdate(name: String, rating: Int)

    @Transactional
    @Modifying
    @Query("""
        TRUNCATE TABLE maimai_rank RESTART IDENTITY;
        """, nativeQuery = true)
    fun clear()

    @Query("""
        SELECT * FROM maimai_rank WHERE rating BETWEEN :bottomRating AND :topRating
    """, nativeQuery = true)
    fun findSurrounding(bottomRating: Int, topRating: Int): List<MaiRankingLite>
}

interface MaiAliasLiteRepository : JpaRepository<MaiAliasLite, Int>

interface ChuAliasLiteRepository : JpaRepository<ChuAliasLite, Int>

interface ChuSongLiteRepository : JpaRepository<ChuSongLite, Int> {
    fun findByQueryTitleLikeIgnoreCase(queryTitle: String): List<ChuSongLite>?
}

interface ChuChartLiteRepository : JpaRepository<ChuChartLite, Int>