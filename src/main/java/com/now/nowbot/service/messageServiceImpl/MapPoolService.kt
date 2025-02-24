package com.now.nowbot.service.messageServiceImpl

import com.fasterxml.jackson.databind.JsonNode
import com.now.nowbot.config.NowbotConfig
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.mappool.now.Pool
import com.now.nowbot.model.mappool.old.MapPoolDto
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.messageServiceImpl.MapPoolService.PoolParam
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.throwable.TipsException
import com.now.nowbot.util.ASyncMessageUtil
import com.now.nowbot.util.CmdUtil.getMode
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.JacksonUtil
import com.now.nowbot.util.command.FLAG_NAME
import java.time.Duration
import java.util.*
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.util.UriBuilder
import org.springframework.web.util.UriComponentsBuilder

@Service("MAP_POOL")
class MapPoolService(
        private val imageService: ImageService,
        private val osuBeatmapApiService: OsuBeatmapApiService,
        private val webClient: WebClient,
) : MessageService<PoolParam> {

    data class PoolParam(val id: Int, val name: String?, val mode: OsuMode)

    @Throws(TipsException::class)
    override fun isHandle(
            event: MessageEvent,
            messageText: String,
            data: DataValue<PoolParam>,
    ): Boolean {
        val m = Instruction.MAP_POOL.matcher(messageText)
        if (!m.find()) return false

        val name: String = m.group(FLAG_NAME)
        val mode = getMode(m).data!!

        if (name.isBlank()) {
            throw TipsException("id 解析错误, 请确保只有数字")
        }

        try {
            val id = name.toInt()
            data.setValue(PoolParam(id, null, mode))
        } catch (e: NumberFormatException) {
            data.setValue(PoolParam(0, name, mode))
        }

        return true
    }

    @Throws(Throwable::class)
    override fun HandleMessage(event: MessageEvent, param: PoolParam) {
        val image: ByteArray
        if (param.name.isNullOrBlank().not()) {
            val result = searchByName(param.name!!)
            if (result.isEmpty()) throw TipsException("未找到名称包含 ${param.name} 的图池")
            if (result.size == 1) {
                image =
                        imageService.getPanelH(
                                MapPoolDto(result.first(), osuBeatmapApiService),
                                param.mode,
                        )
            } else {
                val sb = StringBuilder("查到了多个图池, 请确认结果:\n")
                for (i in result.indices) {
                    sb.append(i + 1).append(": ").append(result[i].name).append('\n')
                }
                sb.append("p.s. 请直接发送选项对应的数字")

                val image2 = imageService.getPanelAlpha(sb)
                event.reply(image2)

                val lock = ASyncMessageUtil.getLock(event)
                val newEvent = lock.get()
                val n: Int
                try {
                    n = newEvent.rawMessage.toInt()
                } catch (e: NumberFormatException) {
                    throw TipsException("输入错误")
                }
                if (n < 1 || n > result.size) throw TipsException("输入错误")

                image =
                        imageService.getPanelH(
                                MapPoolDto(result[n - 1], osuBeatmapApiService),
                                param.mode,
                        )
            }
        } else {
            val p = searchById(param.id)
            image =
                    imageService.getPanelH(
                            p.map { pool: Pool? -> MapPoolDto(pool, osuBeatmapApiService) }
                                    .orElseThrow { TipsException("未找到id为 ${param.id} 的图池") },
                            param.mode,
                    )
        }

        event.reply(image)
    }

    fun searchByName(name: String): List<Pool> {
        val nodeOpt =
                webClient
                        .get()
                        .uri { u: UriBuilder? ->
                            UriComponentsBuilder.fromUriString(URL)
                                    .path("/api/public/searchPool")
                                    .queryParam("poolName", name)
                                    .build()
                                    .toUri()
                        }
                        .headers { h: HttpHeaders ->
                            token.ifPresent { t: String -> h.addIfAbsent("AuthorizationX", t) }
                        }
                        .retrieve()
                        .bodyToMono(JsonNode::class.java)
                        .blockOptional(Duration.ofSeconds(30))
        return nodeOpt.map { node: JsonNode ->
                    JacksonUtil.parseObjectList(node["data"], Pool::class.java)
                }
                .orElseThrow()
    }

    fun searchById(id: Int): Optional<Pool> {
        return try {
            webClient
                    .get()
                    .uri { u: UriBuilder? ->
                        UriComponentsBuilder.fromUriString(URL)
                                .path("/api/public/searchPool")
                                .queryParam("poolId", id)
                                .build()
                                .toUri()
                    }
                    .headers { h: HttpHeaders ->
                        token.ifPresent { t: String -> h.addIfAbsent("AuthorizationX", t) }
                    }
                    .retrieve()
                    .bodyToMono(JsonNode::class.java)
                    .map { json: JsonNode ->
                        if (json.has("data"))
                                Optional.ofNullable(
                                        JacksonUtil.parseObject(json["data"], Pool::class.java)
                                )
                        else Optional.empty()
                    }
                    .block(Duration.ofSeconds(30)) ?: Optional.empty()
        } catch (e: HttpClientErrorException.NotFound) {
            Optional.empty()
        } catch (e: WebClientResponseException.NotFound) {
            Optional.empty()
        }
    }

    companion object {
        private const val URL = NowbotConfig.BEATMAP_MIRROR_URL
        private val token: Optional<String> = NowbotConfig.BEATMAP_MIRROR_TOKEN
    }
}
