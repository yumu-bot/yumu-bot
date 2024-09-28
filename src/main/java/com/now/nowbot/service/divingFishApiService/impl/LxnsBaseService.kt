package com.now.nowbot.service.divingFishApiService.impl

import com.now.nowbot.config.LxnsConfig
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.Resource
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.util.StringUtils
import org.springframework.web.reactive.function.client.WebClient

@Service
class LxnsBaseService(lxnsConfig: LxnsConfig) {
    @Resource var lxnsApiWebClient: WebClient? = null

    // 这里写 token 相关的
    init {
        if (StringUtils.hasText(lxnsConfig.token)) {
            accessToken = lxnsConfig.token
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
