package com.now.nowbot.service.sbApiService.impl

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import com.now.nowbot.dao.BindDao
import com.now.nowbot.model.ppysb.SBClan
import com.now.nowbot.model.ppysb.SBStatistics
import com.now.nowbot.model.ppysb.SBUser
import com.now.nowbot.service.sbApiService.SBUserApiService
import com.now.nowbot.throwable.TipsException
import com.now.nowbot.throwable.botRuntimeException.NetworkException
import com.now.nowbot.util.JacksonUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import java.util.*

@Service
class SBUserApiImpl(private val base: SBBaseService, private val bindDao: BindDao): SBUserApiService {
    override fun getUserID(username: String): Long? {
        data class Result(
            @JsonProperty("id")
            val id: Long,

            @JsonProperty("name")
            val username: String
        )

        val id = try {
            request { client ->
                client.get().uri { it
                    .path("/v1/search_players")
                    .queryParam("q", username)
                    .build()
                }.retrieve()
                    .bodyToMono(JsonNode::class.java)
                    .map { parseList<Result>(it, "result", "玩家结果") }
            }.firstOrNull()?.id
        } catch (e: NetworkException.UserException) {
            return null
        }

        id?.let { bindDao.updateSBNameToID(it, username) }

        return id
    }

    override fun getUserOnlineCount(): Pair<Long, Long> {
        data class Count(
            @JsonProperty("online")
            val online: Long,

            @JsonProperty("total")
            val total: Long
        )

        val count = request { client ->
            client.get().uri { it
                .path("/v1/search_players")
                .build()
            }.retrieve()
                .bodyToMono(JsonNode::class.java)
                .map { parse<Count>(it, "counts", "玩家在线数量") }
        }

        return count.online to count.total

    }

    override fun getUser(id: Long?, username: String?, scope: String): SBUser? {
        data class User(
            @JsonProperty("info")
            val info: SBUser,

            @JsonProperty("clan")
            val clan: SBClan?,

            @JsonProperty("stats")
            val stats: Map<String, SBStatistics>
        )

        val u = try {
            request { client ->
                client.get().uri {
                    it.path("/v1/get_player_info")
                        .queryParamIfPresent("id", Optional.ofNullable(id))
                        .queryParamIfPresent("name", Optional.ofNullable(username))
                        .queryParam("scope", scope.ifEmpty { "all" })
                        .build()
                }.retrieve()
                    .bodyToMono(JsonNode::class.java)
                    .map { parse<User>(it, "player","玩家信息") }

            }
        } catch (e: NetworkException.UserException) {
            return null
        }

        val user = u.info

        u.clan?.let { user.clan = it }
        user.statistics = u.stats.values.toList()

        bindDao.updateSBNameToID(user)

        return user
    }

    override fun getUserOnlineStatus(id: Long?, username: String?): Pair<Boolean, Long> {
        data class Status(
            @JsonProperty("online")
            val online: Boolean,

            @JsonProperty("last_seen")
            val lastSeen: Long
        )

        val count = try {
            request { client ->
                client.get().uri { it
                    .path("/v1/get_player_status")
                    .queryParamIfPresent("id", Optional.ofNullable(id))
                    .queryParamIfPresent("name", Optional.ofNullable(username))
                    .build()
                }.retrieve()
                    .bodyToMono(JsonNode::class.java)
                    .map { parse<Status>(it, "player_status", "玩家在线状态")
                    }
            }
        } catch (e: NetworkException.UserException) {
            return false to 0L
        }

        return count.online to count.lastSeen
    }
        /**
         * 错误包装
         */
        private fun <T> request(request: (WebClient) -> Mono<T>): T {
            return try {
                request(base.sbApiWebClient).block()!!
            } catch (e: Throwable) {
                when (e.cause) {
                    is WebClientResponseException.BadRequest -> {
                        throw NetworkException.UserException.BadRequest()
                    }

                    is WebClientResponseException.Unauthorized -> {
                        throw NetworkException.UserException.Unauthorized()
                    }

                    is WebClientResponseException.Forbidden -> {
                        throw NetworkException.UserException.Forbidden()
                    }

                    is WebClientResponseException.NotFound -> {
                        throw NetworkException.UserException.NotFound()
                    }

                    is WebClientResponseException.UnprocessableEntity -> {
                        throw NetworkException.UserException.UnprocessableEntity()
                    }

                    is WebClientResponseException.TooManyRequests -> {
                        throw NetworkException.UserException.TooManyRequests()
                    }

                    is WebClientResponseException.ServiceUnavailable -> {
                        throw NetworkException.UserException.ServiceUnavailable()
                    }

                    else -> throw NetworkException.UserException(e.message)
                }
            }
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(SBUserApiService::class.java)

        private inline fun <reified T> parse(node: JsonNode, field: String, name: String): T {
            val status = node.get("status").asText("未知")

            if (status != "success") {
                throw TipsException("获取${name}失败。失败提示：${status}")
            } else try {
                return JacksonUtil.parseObject(node[field]!!, T::class.java)
            } catch (e : Exception) {
                log.error("生成${name}失败。", e)
                return T::class.objectInstance!!
            }
        }

        private inline fun <reified T> parseList(node: JsonNode, field: String, name: String): List<T> {
            val status = node.get("status").asText("未知")

            if (status != "success") {
                throw TipsException("获取${name}失败。失败提示：${status}")
            } else try {
                return JacksonUtil.parseObjectList(node[field], T::class.java)
            } catch (e : Exception) {
                log.error("生成${name}失败。", e)
                return listOf()
            }
        }
    }
}