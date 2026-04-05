package com.now.nowbot.service.lxnsApiService.impl

import com.now.nowbot.model.maimai.LxChuBestScore
import com.now.nowbot.model.maimai.LxChuUser
import com.now.nowbot.service.lxnsApiService.LxChunithmApiService
import com.now.nowbot.throwable.TipsException
import com.now.nowbot.throwable.botRuntimeException.NetworkException
import com.now.nowbot.util.DataUtil.findCauseOfType
import com.now.nowbot.util.JacksonUtil
import com.now.nowbot.util.toBody
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClient
import tools.jackson.databind.JsonNode
import java.util.concurrent.CancellationException

@Service
class LxChunithmApiImpl(
    private val base: LxnsBaseService,
) : LxChunithmApiService {
    override fun getChunithmBests(friendCode: Long): LxChuBestScore {
        return request { client ->
            val jsonString = client.get()
                .uri("api/v0/chunithm/player/${friendCode}/bests")
                .headers(base::insertDeveloperHeader)
                .toBody<String>()
            val node = JacksonUtil.toNode(jsonString)
            parse<LxChuBestScore>(node, "data", "玩家中二节奏最好成绩")
        }
    }

    override fun getUser(qq: Long): LxChuUser {
        return request { client ->
            val jsonString = client.get()
                .uri("api/v0/chunithm/player/qq/${qq}")
                .headers(base::insertDeveloperHeader)
                .toBody<String>()
            val node = JacksonUtil.toNode(jsonString)
            parse<LxChuUser>(node, "data", "玩家中二节奏信息")
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
            val ex = e.findCauseOfType<HttpClientErrorException>()

            when (ex?.statusCode?.value()) {
                502 -> throw NetworkException.LxnsException.BadGateway()
                500 -> throw NetworkException.LxnsException.InternalServerError()
                401 -> throw NetworkException.LxnsException.Unauthorized()
                403 -> throw NetworkException.LxnsException.Forbidden()
                404 -> throw NetworkException.LxnsException.NotFound()
                408 -> throw NetworkException.LxnsException.RequestTimeout()
                429 -> throw NetworkException.LxnsException.TooManyRequests()
                503 -> throw NetworkException.LxnsException.ServiceUnavailable()
                504 -> throw NetworkException.LxnsException.GatewayTimeout()

                else -> {
                    if (e !is CancellationException) {
                        log.error("落雪咖啡屋：获取失败", e)
                        throw NetworkException.LxnsException.Undefined(e)
                    } else {
                        throw e
                    }
                }
            }
        }
    }

    companion object {

        private inline fun <reified T> parse(node: JsonNode, field: String, name: String): T {
            val success = node.get("success").asString("未知")

            if (success != "true") {
                throw TipsException(
                    """
                    获取${name}失败。
                    失败代码：${node.get("code").asInt(-1)}
                    失败原因：${node.get("message").asString("未知")}
                    """.trimIndent()
                )
            } else try {
                return JacksonUtil.parseObject<T>(node[field])!!
            } catch (e: Exception) {
                log.error("生成${name}失败。", e)
                return T::class.objectInstance!!
            }
        }

        private val log: Logger = LoggerFactory.getLogger(LxChunithmApiImpl::class.java)

    }
}