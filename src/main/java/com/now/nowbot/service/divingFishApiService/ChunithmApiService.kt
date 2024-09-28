package com.now.nowbot.service.divingFishApiService

import com.now.nowbot.model.json.ChuBestScore
import com.now.nowbot.model.json.ChuSong

interface ChunithmApiService {

    fun getChunithmBest30Recent10(qq: Long): ChuBestScore
    
    fun getChunithmBest30Recent10(probername: String): ChuBestScore

    fun getChunithmCover(songID: Long): ByteArray

    fun getChunithmCoverFromAPI(songID: Long): ByteArray

    fun getChunithmSongLibrary(): Map<Int, ChuSong>
}
