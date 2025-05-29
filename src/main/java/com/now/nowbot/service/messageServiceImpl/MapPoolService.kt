package com.now.nowbot.service.messageServiceImpl

import com.fasterxml.jackson.databind.JsonNode
import com.now.nowbot.config.BeatmapMirrorConfig
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.mappool.now.Pool
import com.now.nowbot.model.mappool.old.MapPoolDto
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.messageServiceImpl.MapPoolService.PoolParam
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.service.osuApiService.OsuCalculateApiService
import com.now.nowbot.throwable.TipsException
import com.now.nowbot.util.ASyncMessageUtil
import com.now.nowbot.util.CmdUtil.getMode
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.JacksonUtil
import com.now.nowbot.util.command.FLAG_NAME
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.util.UriComponentsBuilder
import java.time.Duration
import java.util.*

@Service("MAP_POOL") class MapPoolService(
    private val imageService: ImageService,
    private val beatmapApiService: OsuBeatmapApiService,
    private val calculateApiService: OsuCalculateApiService,
    private val webClient: WebClient,
    beatmapMirrorConfig: BeatmapMirrorConfig
) : MessageService<PoolParam> {
    private val url = beatmapMirrorConfig.url
    private val token = beatmapMirrorConfig.token

    data class PoolParam(val id: Int, val name: String?, val mode: OsuMode)

    @Throws(TipsException::class) override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: DataValue<PoolParam>,
    ): Boolean {
        val m = Instruction.MAP_POOL.matcher(messageText)
        if (!m.find()) return false

        val name: String? = m.group(FLAG_NAME)
        val mode = getMode(m).data!!

        if (name.isNullOrBlank()) {
            throw TipsException("请输入正确的图池名称！")
        }

        val id = name.toIntOrNull()

        if (id != null) {
            data.value = PoolParam(id, null, mode)
        } else {
            data.value = PoolParam(0, name, mode)
        }

        return true
    }

    @Throws(Throwable::class) override fun HandleMessage(event: MessageEvent, param: PoolParam) {
        if (param.name.isNullOrBlank().not()) {
            val result = searchByName(param.name!!)
            if (result.isEmpty()) throw TipsException("未找到名称包含 ${param.name} 的图池")

            if (result.size == 1) {
                event.reply(MapPoolDto(result.first(), beatmapApiService, calculateApiService), param.mode)
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

                val n: Int = newEvent.rawMessage.toIntOrNull() ?: throw TipsException("输入错误")

                if (n !in 1..result.size) throw TipsException("输入错误")

                event.reply(MapPoolDto(result[n - 1], beatmapApiService, calculateApiService), param.mode)
            }
        } else {
            val pool = searchByID(param.id) ?: throw TipsException("未找到id为 ${param.id} 的图池")

            event.reply(MapPoolDto(pool, beatmapApiService, calculateApiService), param.mode)
        }
    }

    private fun MessageEvent.reply(data: MapPoolDto, mode: OsuMode) {
        val body = mapOf("pool" to data, "mode" to mode.shortName)

        val image = imageService.getPanel(body, "H")

        this.reply(image)
    }

    fun searchByName(name: String): List<Pool> {
        if (url == null) return emptyList()

        val nodeOpt = webClient.get().uri {
            UriComponentsBuilder.fromUriString(url).path("/api/public/searchPool").queryParam("poolName", name).build()
                .toUri()
        }.headers {
            it.addIfAbsent("AuthorizationX", token)
        }.retrieve().bodyToMono(JsonNode::class.java).block(Duration.ofSeconds(30)) ?: return emptyList()

        return nodeOpt.map { node: JsonNode ->
            JacksonUtil.parseObjectList(node["data"], Pool::class.java)
        }.flatten()
    }

    fun searchByID(id: Int): Pool? {
        if (url == null) return null

        return try {
            webClient.get().uri {
                UriComponentsBuilder.fromUriString(url).path("/api/public/searchPool").queryParam("poolId", id).build()
                    .toUri()
            }.headers {
                it.addIfAbsent("AuthorizationX", token)
            }.retrieve().bodyToMono(JsonNode::class.java).map { json: JsonNode ->
                if (json.has("data")) JacksonUtil.parseObject(json["data"], Pool::class.java)
                else null
            }.block(Duration.ofSeconds(30))
        } catch (e: HttpClientErrorException.NotFound) {
            null
        } catch (e: WebClientResponseException.NotFound) {
            null
        }
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(MapPoolService::class.java)
    }
}
