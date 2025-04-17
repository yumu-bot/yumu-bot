package com.now.nowbot.service.divingFishApiService

import com.now.nowbot.model.json.*

interface ChunithmApiService {

    fun getChunithmBest30Recent10(qq: Long): ChuBestScore
    
    fun getChunithmBest30Recent10(probername: String): ChuBestScore
    // 因为 Chunithm 的封面是另一个 API 给的，所以这里要存一份去本地
    fun downloadChunithmCover(songID: Long)

    fun getChunithmCover(songID: Long): ByteArray

    fun getChunithmCoverFromAPI(songID: Long): ByteArray

    fun getChunithmSongLibrary(): Map<Int, ChuSong>

    fun getChunithmSong(songID: Long): ChuSong?

    fun getChunithmAlias(songID: Long): ChuAlias?

    fun getChunithmAlias(songID: Int): ChuAlias?

    fun getChunithmAliasLibrary(): Map<Int, List<String>>?

    fun insertChunithmAlias(song: ChuSong?)

    fun insertChunithmAlias(songs: List<ChuSong>?)

    fun insertChunithmAliasForScore(score: ChuScore?)

    fun insertChunithmAliasForScore(scores: List<ChuScore>?)

    fun insertSongData(scores: List<ChuScore>)

    fun insertSongData(score: ChuScore, song: ChuSong)

    fun insertPosition(scores: List<ChuScore>, isBest30: Boolean)

    fun updateChunithmSongLibraryDatabase()

    fun updateChunithmAliasLibraryDatabase()

    fun updateChunithmSongLibraryFile()
}
