package com.now.nowbot.service.lxnsApiService.impl

import com.now.nowbot.config.FileConfig
import com.now.nowbot.config.LxnsConfig
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import java.nio.file.Path

@Service
class LxnsBaseService(
    @param:Qualifier("lxnsApiRestClient")
    val lxnsApiRestClient: RestClient,
    lxnsConfig: LxnsConfig,
    fileConfig: FileConfig
) {
    final val maimaiPath: Path? = Path.of(fileConfig.exportFile, lxnsConfig.maimai)

    final val chunithmPath: Path? = Path.of(fileConfig.exportFile, lxnsConfig.chunithm)

    final val assetHost: String = lxnsConfig.assetHost

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
