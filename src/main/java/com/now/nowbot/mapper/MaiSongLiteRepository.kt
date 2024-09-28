package com.now.nowbot.mapper

import com.now.nowbot.entity.*
import jakarta.persistence.Transient
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface MaiSongLiteRepository : JpaRepository<MaiSongLite, Int>
interface MaiChartLiteRepository : JpaRepository<MaiChartLite, Int>
interface MaiFitChartLiteRepository : JpaRepository<MaiFitChartLite, Int> {
    fun existsMaiFitChartLiteBySongIDAndSort(songID: Int, sort: Int): Boolean

    @Transient
    @Modifying
    @Query(
        """
        update com.now.nowbot.entity.MaiFitChartLite m
        set m.count = :#{#maiFit.count},
            m.fit = :#{#maiFit.fit},
            m.achievements = :#{#maiFit.achievements},
            m.score = :#{#maiFit.score},
            m.standardDeviation = :#{#maiFit.standardDeviation},
            m.distribution = :#{#maiFit.distribution},
            m.fullComboDistribution = :#{#maiFit.fullComboDistribution}
        where m.songID = :songID and m.sort = :sort
    """
    )
    fun updateMaiFitChartLiteBySongIDAndSort(songID: Int, sort: Int, maiFit: MaiFitChartLite)

    fun findMaiFitChartLitesBySongIDOrderBySortAsc(songID: Int): List<MaiFitChartLite>
}

interface MaiFitDiffLiteRepository : JpaRepository<MaiFitDiffLite, String>
interface MaiRankLiteRepositpry : JpaRepository<MaiRankingLite, String>