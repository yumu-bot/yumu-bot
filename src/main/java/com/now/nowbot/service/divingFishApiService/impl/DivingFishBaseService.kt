package com.now.nowbot.service.divingFishApiService.impl

import com.now.nowbot.config.DivingFishConfig
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.Resource
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import java.nio.file.Path

@Service
class DivingFishBaseService(fishConfig: DivingFishConfig) {
    @Resource val divingFishApiWebClient: WebClient? = null

    @Resource val webClient: WebClient? = null

    // D:/App2/[Projects]/yumu-bot-run/img/ExportFileV3/Maimai
    // /home/spring/work/img/ExportFileV3/Maimai
    final val maimaiPath: Path? = fishConfig.maimai

    // D:/App2/[Projects]/yumu-bot-run/img/ExportFileV3/Chunithm
    // /home/spring/work/img/ExportFileV3/Chunithm
    final val chunithmPath: Path?  = fishConfig.chunithm

    // 这里写 token 相关的
    init {
        if (fishConfig.token.isNullOrBlank().not()) {
            accessToken = fishConfig.token
        }
    }

    fun hasToken(): Boolean {
        return accessToken.isNullOrBlank().not()
    }

    fun insertDeveloperHeader(headers: HttpHeaders?) {
        if (headers == null) return
        headers["Developer-Token"] = accessToken
    }

    fun insertJSONHeader(headers: HttpHeaders?) {
        if (headers == null) return
        headers["Content-Type"] = "application/json"
    }

    companion object {
        val log = KotlinLogging.logger { }
        private var accessToken: String? = null

    }
}
