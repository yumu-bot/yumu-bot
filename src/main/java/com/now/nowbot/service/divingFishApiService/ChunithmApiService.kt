package com.now.nowbot.service.divingFishApiService

import com.now.nowbot.model.json.ChuBestScore
import com.now.nowbot.model.json.ChuSong

interface ChunithmApiService {

    fun getChunithmBest30Recent10(qq: Long): ChuBestScore
    
    fun getChunithmBest30Recent10(probername: String): ChuBestScore
    // 因为 Chunithm 的封面是另一个 API 给的，所以这里要存一份去本地
    fun downloadChunithmCover(songID: Long)

    fun getChunithmCover(songID: Long): ByteArray

    fun getChunithmCoverFromAPI(songID: Long): ByteArray

    fun getChunithmSongLibrary(): Map<Int, ChuSong>

    fun updateChunithmSongLibraryFile()
}
