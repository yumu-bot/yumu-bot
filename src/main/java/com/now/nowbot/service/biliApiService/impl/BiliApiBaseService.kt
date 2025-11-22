package com.now.nowbot.service.biliApiService.impl

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient

@Service
class BiliApiBaseService(val webClient: WebClient, @param:Qualifier("biliApiWebClient") val biliApiWebClient: WebClient) {

    fun hasToken(): Boolean {
        return accessToken.isNullOrBlank().not()
    }

    fun insertJSONHeader(headers: HttpHeaders?) {
        if (headers == null) return
        headers["Content-Type"] = "application/json"
    }

    companion object {
        private var accessToken: String? = null

    }
}
