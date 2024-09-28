package com.now.nowbot.service.divingFishApiService.impl

import com.now.nowbot.config.DivingFishConfig
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.Resource
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.util.StringUtils
import org.springframework.web.reactive.function.client.WebClient

@Service
class DivingFishBaseService(fishConfig: DivingFishConfig) {
    @Resource val divingFishApiWebClient: WebClient? = null

    @Resource val webClient: WebClient? = null

    // 这里写 token 相关的
    init {
        if (StringUtils.hasText(fishConfig.token)) {
            accessToken = fishConfig.token
        }
    }

    fun hasToken(): Boolean {
        return StringUtils.hasText(accessToken)
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
