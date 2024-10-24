package com.now.nowbot.mapper

import com.now.nowbot.entity.*
import org.springframework.data.jpa.repository.JpaRepository
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

interface MaiRankLiteRepository : JpaRepository<MaiRankingLite, String>
interface MaiAliasLiteRepository : JpaRepository<MaiAliasLite, Int>