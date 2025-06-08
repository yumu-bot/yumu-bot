package com.now.nowbot.service.divingFishApiService.impl

import com.now.nowbot.config.DivingFishConfig
import com.now.nowbot.service.RequestService
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.nio.file.Path
import java.util.concurrent.ExecutionException

@Service
class DivingFishBaseService(val webClient: WebClient, @Qualifier("divingFishApiWebClient") val divingFishApiWebClient: WebClient, fishConfig: DivingFishConfig) {
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
    /*

    private lateinit var requestService: RequestService

    @PostConstruct fun init() {
        requestService = RequestService(divingFishApiWebClient, "diving-api-priority")
        Thread.startVirtualThread {
            requestService.runTask()
        }
    }

    @Throws(ExecutionException::class)
    fun <T> request(request: (WebClient) -> Mono<T>): T {
        return requestService.request(request)
    }

     */

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
        private var accessToken: String? = null

    }
}
