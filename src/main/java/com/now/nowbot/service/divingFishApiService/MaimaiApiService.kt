package com.now.nowbot.service.divingFishApiService

import com.now.nowbot.model.enums.MaiVersion
import com.now.nowbot.model.maimai.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory

interface MaimaiApiService {
    fun getMaimaiBest50(qq: Long): MaiBestScore

    fun getMaimaiBest50(username: String): MaiBestScore

    fun getMaimaiScoreByVersion(qq: Long, versions: List<MaiVersion>): MaiVersionScore

    fun getMaimaiScoreByVersion(username: String, versions: List<MaiVersion>): MaiVersionScore

    fun getMaimaiCover(songID: Long): ByteArray

    fun getMaimaiCoverFromAPI(songID: Long): ByteArray

    fun updateMaimaiSongLibraryFile()

    fun updateMaimaiRankLibraryFile()

    fun updateMaimaiFitLibraryFile()

    fun updateMaimaiSongLibraryDatabase()

    fun updateMaimaiRankLibraryDatabase()

    fun updateMaimaiFitLibraryDatabase()

    fun updateMaimaiAliasLibraryDatabase()

    fun getMaimaiPossibleSong(text: String): MaiSong?

    fun getMaimaiPossibleSongs(text: String): List<MaiSong>

    fun getMaimaiAliasSong(text: String): MaiSong?

    fun getMaimaiAliasSongs(text: String): List<MaiSong>?

    fun getMaimaiSong(songID: Long): MaiSong?

    fun getMaimaiSongLibrary(): List<MaiSong>

    fun getMaimaiRank(): Map<String, Int>

    fun getMaimaiSurroundingRank(rating: Int = 15000): Map<String, Int>

    fun getMaimaiChartData(songID: Long): List<MaiFit.ChartData>

    fun getMaimaiDiffData(level: String): MaiFit.DiffData

    fun getMaimaiAlias(songID: Long): MaiAlias?

    fun getMaimaiAlias(songID: Int): MaiAlias?

    fun getMaimaiAliasLibrary(): Map<Int, List<String>>?

    /**
     * 一条龙服务
     */
    fun insert(charts: MaiBestScore.Charts) {
        insert(charts.deluxe, 35)
        insert(charts.standard, 0)
    }

    /**
     * 一条龙服务
     */
    fun insert(scores: List<MaiScore>, offset: Int = 0) {
        insertPosition(scores, offset)
        insertMaimaiAliasForScore(scores)
        insertSongData(scores)
    }

    fun insertMaimaiAlias(song: MaiSong?)

    fun insertMaimaiAlias(songs: List<MaiSong>?)

    fun insertMaimaiAliasForScore(score: MaiScore?)

    fun insertMaimaiAliasForScore(scores: List<MaiScore>?)

    fun insertSongData(scores: List<MaiScore>)

    fun insertPosition(scores: List<MaiScore>, offset: Int = 0)

    fun insertSongData(score: MaiScore, song: MaiSong)

    // 以下需要从水鱼那里拿 DeveloperToken
    fun getMaimaiSongScore(qq: Long, songID: Int): MaiScore

    fun getMaimaiSongsScore(qq: Long, songIDs: List<Int>): List<MaiScore>

    fun getMaimaiSongScore(username: String, songID: Int): MaiScore

    fun getMaimaiSongsScore(username: String, songIDs: List<Int>): List<MaiScore>

    fun getMaimaiFullScores(qq: Long): MaiBestScore

    fun getMaimaiFullScores(username: String): MaiBestScore

    companion object {
        val log: Logger = LoggerFactory.getLogger(MaimaiApiService::class.java)
    }
}
