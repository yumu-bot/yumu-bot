package com.now.nowbot.service.biliApiService

import com.now.nowbot.model.bili.BiliDanmaku
import com.now.nowbot.model.bili.BiliStreamer
import com.now.nowbot.model.bili.BiliUser

interface BiliApiService {
    fun getStreamer(id: Long): BiliStreamer

    fun getUser(id: Long): BiliUser

    fun getDanmaku(roomID: Long): BiliDanmaku

    fun getImage(url: String): ByteArray
}