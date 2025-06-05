package com.now.nowbot.service.sbApiService

import com.now.nowbot.model.ppysb.SBBeatmap

interface SBBeatmapApiService {
    fun getBeatmap(id: Long? = null, md5: String? = null): SBBeatmap?
}