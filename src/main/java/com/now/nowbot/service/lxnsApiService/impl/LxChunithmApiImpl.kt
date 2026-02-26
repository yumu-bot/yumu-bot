package com.now.nowbot.service.lxnsApiService.impl

import com.fasterxml.jackson.databind.JsonNode
import com.now.nowbot.dao.MaiDao
import com.now.nowbot.model.maimai.LxChuBestScore
import com.now.nowbot.model.maimai.LxChuUser
import com.now.nowbot.service.lxnsApiService.LxChunithmApiService
import com.now.nowbot.throwable.TipsException
import com.now.nowbot.throwable.botRuntimeException.NetworkException
import com.now.nowbot.util.DataUtil.findCauseOfType
import com.now.nowbot.util.JacksonUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.client.body
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeoutException

@Service
class LxChunithmApiImpl(
    private val base: LxnsBaseService,
    private val maiDao: MaiDao
) : LxChunithmApiService {
    override fun getChunithmBests(friendCode: Long): LxChuBestScore {
        return request { client ->
            client.get()
                .uri("api/v0/chunithm/player/${friendCode}/bests")
                .headers(base::insertDeveloperHeader)
                .retrieve()
                .body<JsonNode>()!!
                .let { parse<LxChuBestScore>(it, "data", "玩家中二节奏最好成绩") }
        }
    }

    override fun getUser(qq: Long): LxChuUser {
        return request { client ->
            client.get()
                .uri("api/v0/chunithm/player/qq/${qq}")
                .headers(base::insertDeveloperHeader)
                .retrieve()
                .body<JsonNode>()!!
                .let { parse<LxChuUser>(it, "data", "玩家中二节奏信息") }
        }
    }

    /**
     * 错误包装
     */
    @Throws(NetworkException::class)
    private fun <T : Any> request(request: (RestClient) -> T): T {
        return try {
            request(base.lxnsApiRestClient)
        } catch (e: Throwable) {
            val ex = e.findCauseOfType<RestClientResponseException>()

            if (ex != null) {
                when (ex.statusCode.value()) {
                    502 -> throw NetworkException.LxnsException.BadGateway()
                    500 -> throw NetworkException.LxnsException.InternalServerError()
                    401 -> throw NetworkException.LxnsException.Unauthorized()
                    403 -> throw NetworkException.LxnsException.Forbidden()
                    404 -> throw NetworkException.LxnsException.NotFound()
                    429 -> throw NetworkException.LxnsException.TooManyRequests()
                    503 -> throw NetworkException.LxnsException.ServiceUnavailable()
                    else -> {
                        log.error("落雪咖啡屋：获取失败", e)
                        throw NetworkException.LxnsException.Undefined(e)
                    }
                }
            } else {
                if (e.findCauseOfType<SocketTimeoutException>() != null || e.findCauseOfType<TimeoutException>() != null) {
                    throw NetworkException.LxnsException.RequestTimeout()
                } else if (e.findCauseOfType<IOException>() != null) {
                    throw NetworkException.LxnsException.GatewayTimeout()
                } else {
                    log.error("落雪咖啡屋：获取失败", e)
                    throw NetworkException.LxnsException.Undefined(e)
                }
            }
        }
    }

    companion object {

        private inline fun <reified T> parse(node: JsonNode, field: String, name: String): T {
            val success = node.get("success").asText("未知")

            if (success != "true") {
                throw TipsException(
                    """
                    获取${name}失败。
                    失败代码：${node.get("code").asInt(-1)}
                    失败原因：${node.get("message").asText("未知")}
                    """.trimIndent()
                )
            } else try {
                return JacksonUtil.parseObject(node[field]!!, T::class.java)
            } catch (e: Exception) {
                log.error("生成${name}失败。", e)
                return T::class.objectInstance!!
            }
        }

        private val log: Logger = LoggerFactory.getLogger(LxChunithmApiImpl::class.java)

    }
}