package com.now.nowbot.service.divingFishApiService

import com.now.nowbot.model.enums.MaiVersion
import com.now.nowbot.model.json.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.web.reactive.function.client.WebClientResponseException

interface MaimaiApiService {
    @Throws(
            WebClientResponseException.Forbidden::class,
            WebClientResponseException.BadGateway::class,
    )
    fun getMaimaiBest50(qq: Long): MaiBestScore

    @Throws(
            WebClientResponseException.Forbidden::class,
            WebClientResponseException.BadGateway::class,
    )
    fun getMaimaiBest50(username: String): MaiBestScore

    @Throws(
            WebClientResponseException.Forbidden::class,
            WebClientResponseException.BadGateway::class,
    )
    fun getMaimaiScoreByVersion(qq: Long, versions: List<MaiVersion>): MaiVersionScore

    @Throws(
            WebClientResponseException.Forbidden::class,
            WebClientResponseException.BadGateway::class,
    )
    fun getMaimaiScoreByVersion(
            username: String,
            versions: List<MaiVersion>,
    ): MaiVersionScore

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

    fun getMaimaiPossibleSongs(text: String): List<MaiSong>?

    fun getMaimaiAliasSong(text: String): MaiSong?

    fun getMaimaiAliasSongs(text: String): List<MaiSong>?

    fun getMaimaiSong(songID: Long): MaiSong?

    fun getMaimaiSongLibrary(): Map<Int, MaiSong>

    fun getMaimaiRank(): Map<String, Int>

    fun getMaimaiSurroundingRank(rating: Int = 15000): Map<String, Int>

    fun getMaimaiChartData(songID: Long): List<MaiFit.ChartData>

    fun getMaimaiDiffData(difficulty: String): MaiFit.DiffData

    fun getMaimaiAlias(songID: Long): MaiAlias?

    fun getMaimaiAlias(songID: Int): MaiAlias?

    fun getMaimaiAliasLibrary(): Map<Int, List<String>>?

    fun insertMaimaiAlias(song: MaiSong?)

    fun insertMaimaiAlias(songs: List<MaiSong>?)

    fun insertMaimaiAliasForScore(score: MaiScore?)

    fun insertMaimaiAliasForScore(scores: List<MaiScore>?)

    fun insertSongData(scores: List<MaiScore>)

    fun insertPosition(scores: List<MaiScore>, isBest35: Boolean)

    fun insertSongData(score: MaiScore, song: MaiSong)

    // 以下需要从水鱼那里拿 DeveloperToken
    @Throws(
            WebClientResponseException.Forbidden::class,
            WebClientResponseException.BadGateway::class,
    )
    fun getMaimaiSongScore(qq: Long, songID: Int): MaiScore

    @Throws(
            WebClientResponseException.Forbidden::class,
            WebClientResponseException.BadGateway::class,
    )
    fun getMaimaiSongsScore(qq: Long, songIDs: List<Int>): List<MaiScore>

    @Throws(
            WebClientResponseException.Forbidden::class,
            WebClientResponseException.BadGateway::class,
    )
    fun getMaimaiSongScore(username: String, songID: Int): MaiScore

    @Throws(
            WebClientResponseException.Forbidden::class,
            WebClientResponseException.BadGateway::class,
    )
    fun getMaimaiSongsScore(username: String, songIDs: List<Int>): List<MaiScore>

    @Throws(
            WebClientResponseException.Forbidden::class,
            WebClientResponseException.BadGateway::class,
    )
    fun getMaimaiFullScores(qq: Long): MaiBestScore

    @Throws(
            WebClientResponseException.Forbidden::class,
            WebClientResponseException.BadGateway::class,
    )
    fun getMaimaiFullScores(username: String): MaiBestScore

    companion object {
        val log: Logger = LoggerFactory.getLogger(MaimaiApiService::class.java)
    }
}
