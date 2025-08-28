package com.now.nowbot.service.lxnsApiService.impl

import com.now.nowbot.dao.MaiDao
import com.now.nowbot.model.maimai.LxChuBestScore
import com.now.nowbot.model.maimai.LxChuUser
import com.now.nowbot.service.divingFishApiService.impl.LxnsBaseService
import com.now.nowbot.service.lxnsApiService.LxChunithmApiService
import com.now.nowbot.throwable.botRuntimeException.NetworkException
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
            .bodyToMono(LxChuBestScore::class.java)
        }
    }

    override fun getUser(qq: Long): LxChuUser {
        return request { client -> client.get()
            .uri {
                it.path("api/v0/chunithm/player/qq/${qq}")
                    .build() }
            .headers(base::insertDeveloperHeader)
            .retrieve()
            .bodyToMono(LxChuUser::class.java)
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
        private val log: Logger = LoggerFactory.getLogger(LxChunithmApiImpl::class.java)

    }
}