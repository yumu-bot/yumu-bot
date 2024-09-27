package com.now.nowbot.service.divingFishApiService

import com.now.nowbot.model.json.ChuBestScore

interface ChunithmApiService {

    fun getChunithmBest30Recent10(qq: Long): ChuBestScore
    
    fun getChunithmBest30Recent10(probername: String): ChuBestScore

    fun getChunithmCover(songID: Long): ByteArray

    fun getChunithmCoverFromAPI(songID: Long): ByteArray
}
