package com.now.nowbot.service.lxnsApiService.impl

import com.now.nowbot.config.LxnsConfig
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import java.nio.file.Path

@Service
class LxnsBaseService(val webClient: WebClient, @Qualifier("lxnsApiWebClient") val lxnsApiWebClient: WebClient, lxnsConfig: LxnsConfig) {
    // D:/App2/[Projects]/yumu-bot-run/img/ExportFileV3/Maimai
    // /home/spring/work/img/ExportFileV3/Maimai
    final val maimaiPath: Path? = lxnsConfig.maimai

    // D:/App2/[Projects]/yumu-bot-run/img/ExportFileV3/Chunithm
    // /home/spring/work/img/ExportFileV3/Chunithm
    final val chunithmPath: Path?  = lxnsConfig.chunithm

    // 这里写 token 相关的
    init {
        if (lxnsConfig.token.isNullOrBlank().not()) {
            accessToken = lxnsConfig.token
        }
    }

    fun hasToken(): Boolean {
        return accessToken.isNullOrBlank().not()
    }

    fun insertDeveloperHeader(headers: HttpHeaders?) {
        if (headers == null) return
        headers["Authorization"] = accessToken
    }

    fun insertJSONHeader(headers: HttpHeaders?) {
        if (headers == null) return
        headers["Content-Type"] = "application/json"
    }

    companion object {
        private var accessToken: String? = null

    }
}
