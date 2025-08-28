package com.now.nowbot.service.lxnsApiService

import com.now.nowbot.model.maimai.LxChuBestScore
import com.now.nowbot.model.maimai.LxChuUser

interface LxChunithmApiService {
    fun getChunithmBests(friendCode: Long): LxChuBestScore

    fun getUser(qq: Long): LxChuUser
}