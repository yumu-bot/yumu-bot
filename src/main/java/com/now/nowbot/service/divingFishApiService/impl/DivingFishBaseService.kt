package com.now.nowbot.service.divingFishApiService.impl

import com.now.nowbot.config.DivingFishConfig
import com.now.nowbot.config.FileConfig
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import java.nio.file.Path

@Service
class DivingFishBaseService(
    @param:Qualifier("divingFishApiRestClient") val divingFishApiRestClient: RestClient,
    fishConfig: DivingFishConfig,
    fileConfig: FileConfig
) {
    final val maimaiPath: Path? = Path.of(fileConfig.exportFile, fishConfig.maimai)

    final val chunithmPath: Path? = Path.of(fileConfig.exportFile, fishConfig.chunithm)

    // 这里写 token 相关的
    init {
        if (fishConfig.token.isNotBlank()) {
            accessToken = fishConfig.token
        }
    }
    /*

    private lateinit var requestService: RequestService

    @PostConstruct fun init() {
        requestService = RequestService(divingFishApiRestClient, "diving-api-priority")
        Thread.startVirtualThread {
            requestService.runTask()
        }
    }

    @Throws(ExecutionException::class)
    fun <T> request(request: (RestClient) -> T): T {
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
