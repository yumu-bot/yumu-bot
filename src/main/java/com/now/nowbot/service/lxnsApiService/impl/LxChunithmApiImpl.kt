package com.now.nowbot.service.lxnsApiService.impl

import com.fasterxml.jackson.databind.JsonNode
import com.now.nowbot.dao.MaiDao
import com.now.nowbot.model.maimai.LxChuBestScore
import com.now.nowbot.model.maimai.LxChuUser
import com.now.nowbot.service.divingFishApiService.impl.LxnsBaseService
import com.now.nowbot.service.lxnsApiService.LxChunithmApiService
import com.now.nowbot.throwable.TipsException
import com.now.nowbot.throwable.botRuntimeException.NetworkException
import com.now.nowbot.util.JacksonUtil
import io.netty.handler.timeout.ReadTimeoutException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono

@Service
class LxChunithmApiImpl(private val base: LxnsBaseService, private val maiDao: MaiDao): LxChunithmApiService {
    override fun getChunithmBests(friendCode: Long): LxChuBestScore {
        return request { client -> client.get()
            .uri {
                it.path("api/v0/chunithm/player/${friendCode}/bests")
                .build() }
            .headers(base::insertDeveloperHeader)
            .retrieve()
            .bodyToMono(JsonNode::class.java)
            .map { parse<LxChuBestScore>(it, "data", "玩家中二节奏最好成绩") }
            //.bodyToMono(String::class.java).map { JacksonUtil.parseObject(it, LxChuBestScore::class.java) }
        }
    }

    override fun getUser(qq: Long): LxChuUser {
        return request { client -> client.get()
            .uri {
                it.path("api/v0/chunithm/player/qq/${qq}")
                    .build() }
            .headers(base::insertDeveloperHeader)
            .retrieve()
            .bodyToMono(JsonNode::class.java)
            .map { parse<LxChuUser>(it, "data", "玩家中二节奏信息")}
        }
    }

    /**
     * 错误包装
     */
    @Throws(NetworkException::class)
    private fun <T> request(request: (WebClient) -> Mono<T>): T {
        return try {
            request(base.lxnsApiWebClient).block()!!
        } catch (e: WebClientResponseException.BadGateway) {
            throw NetworkException.LxnsException.BadGateway()
        } catch (e: WebClientResponseException.Unauthorized) {
            throw NetworkException.LxnsException.Unauthorized()
        } catch (e: WebClientResponseException.Forbidden) {
            throw NetworkException.LxnsException.Forbidden()
        } catch (e: ReadTimeoutException) {
            throw NetworkException.LxnsException.RequestTimeout()
        } catch (e: WebClientResponseException.InternalServerError) {
            throw NetworkException.LxnsException.InternalServerError()
        } catch (e: Exception) {
            log.error("落雪咖啡屋：获取失败", e)
            throw NetworkException.LxnsException.Undefined(e)
        }
    }

    companion object {

        private inline fun <reified T> parse(node: JsonNode, field: String, name: String): T {
            val success = node.get("success").asText("未知")

            if (success != "true") {
                throw TipsException("""
                    获取${name}失败。
                    失败代码：${node.get("code").asInt(-1)}
                    失败原因：${node.get("message").asText("未知")}
                    """.trimIndent()
                )
            } else try {
                return JacksonUtil.parseObject(node[field]!!, T::class.java)
            } catch (e : Exception) {
                log.error("生成${name}失败。", e)
                return T::class.objectInstance!!
            }
        }

        private val log: Logger = LoggerFactory.getLogger(LxChunithmApiImpl::class.java)

    }
}