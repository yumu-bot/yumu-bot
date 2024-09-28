package com.now.nowbot.service.divingFishApiService

import com.now.nowbot.model.enums.MaiVersion
import com.now.nowbot.model.json.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.web.reactive.function.client.WebClientResponseException

interface MaimaiApiService {
    @Throws(
            WebClientResponseException.Forbidden::class,
            WebClientResponseException.BadGateway::class)
    fun getMaimaiBest50(qq: Long): MaiBestScore

    @Throws(
            WebClientResponseException.Forbidden::class,
            WebClientResponseException.BadGateway::class)
    fun getMaimaiBest50(username: String): MaiBestScore

    @Throws(
            WebClientResponseException.Forbidden::class,
            WebClientResponseException.BadGateway::class)
    fun getMaimaiScoreByVersion(qq: Long, versions: MutableList<MaiVersion>): MaiVersionScore

    @Throws(
            WebClientResponseException.Forbidden::class,
            WebClientResponseException.BadGateway::class)
    fun getMaimaiScoreByVersion(username: String, versions: MutableList<MaiVersion>): MaiVersionScore

    fun getMaimaiCover(songID: Long): ByteArray

    fun getMaimaiCoverFromAPI(songID: Long): ByteArray

    fun updateMaimaiSongLibraryFile()

    fun updateMaimaiRankLibraryFile()

    fun updateMaimaiFitLibraryFile()

    fun updateMaimaiSongLibraryDatabase()

    fun updateMaimaiRankLibraryDatabase()

    fun updateMaimaiFitLibraryDatabase()

    fun getMaimaiPossibleSong(text: String): MaiSong

    fun getMaimaiPossibleSongs(text : String): Map<Double, MaiSong>?

    fun getMaimaiSong(songID: Long): MaiSong

    fun getMaimaiRank(): Map<String, Int>

    fun getMaimaiChartData(songID: Long): List<MaiFit.ChartData>

    fun getMaimaiDiffData(difficulty: String): MaiFit.DiffData

    // 以下需要从水鱼那里拿 DeveloperToken
    @Throws(
            WebClientResponseException.Forbidden::class,
            WebClientResponseException.BadGateway::class)
    fun getMaimaiSongScore(qq: Long, songID: Int): MaiScore

    @Throws(
            WebClientResponseException.Forbidden::class,
            WebClientResponseException.BadGateway::class)
    fun getMaimaiSongsScore(qq: Long, songIDs: List<Int>): List<MaiScore>

    @Throws(
            WebClientResponseException.Forbidden::class,
            WebClientResponseException.BadGateway::class)
    fun getMaimaiSongScore(username: String, songID: Int): MaiScore

    @Throws(
            WebClientResponseException.Forbidden::class,
            WebClientResponseException.BadGateway::class)
    fun getMaimaiSongsScore(username: String, songIDs: List<Int>): List<MaiScore>

    @Throws(
            WebClientResponseException.Forbidden::class,
            WebClientResponseException.BadGateway::class)
    fun getMaimaiFullScores(qq: Long): MaiBestScore

    @Throws(
            WebClientResponseException.Forbidden::class,
            WebClientResponseException.BadGateway::class)
    fun getMaimaiFullScores(username: String): MaiBestScore

    companion object {
        val log: Logger = LoggerFactory.getLogger(MaimaiApiService::class.java)
    }

    fun getMaimaiSongLibrary(): Map<Int, MaiSong>
}
