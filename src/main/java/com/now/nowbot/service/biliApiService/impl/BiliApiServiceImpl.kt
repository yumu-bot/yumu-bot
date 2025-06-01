package com.now.nowbot.service.biliApiService.impl

import com.fasterxml.jackson.databind.JsonNode
import com.now.nowbot.model.bili.BiliDanmaku
import com.now.nowbot.model.bili.BiliStreamer
import com.now.nowbot.model.bili.BiliUser
import com.now.nowbot.service.biliApiService.BiliApiService
import com.now.nowbot.throwable.TipsException
import com.now.nowbot.util.JacksonUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class BiliApiServiceImpl(
    private val base: BiliApiBaseService,
): BiliApiService {
    override fun getStreamer(id: Long): BiliStreamer {
        val node = base.biliApiWebClient.get().uri { it
            .scheme("https")
            .host("api.live.bilibili.com")
            .path("live_user/v1/Master/info")
            .queryParam("uid", id)
            .build()
        }
            .headers { base.insertJSONHeader(it) }
            .retrieve()
            .bodyToMono(JsonNode::class.java).block()!!

        return parse(node, id, "直播主")
    }


    override fun getUser(id: Long): BiliUser {
        val node = base.biliApiWebClient.get().uri { it
            .scheme("https")
            .path("x/space/acc/info")
            .queryParam("mid", id)
            .build()
        }
            .headers { base.insertJSONHeader(it) }
            .retrieve()
            .bodyToMono(JsonNode::class.java)
            .block()!!

        return parse(node, id, "账号信息")
    }

    override fun getDanmaku(roomID: Long): BiliDanmaku {
        val node = base.biliApiWebClient.get().uri { it
            .scheme("https")
            .host("api.live.bilibili.com")
            .path("xlive/web-room/v1/dM/gethistory")
            .queryParam("roomID", roomID)
            .build()
        }
            .headers { base.insertJSONHeader(it) }
            .retrieve()
            .bodyToMono(JsonNode::class.java).block()!!

        return parse(node, roomID, "直播间最近弹幕")
    }

    override fun getImage(url: String): ByteArray {
        val avatar: ByteArray? = base.webClient.get()
            .uri(url)
            .retrieve()
            .bodyToMono(ByteArray::class.java)
            .block()

        return avatar ?: throw TipsException("获取图片失败。")
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(BiliApiService::class.java)

        private inline fun <reified T> parse(node: JsonNode, param: Any?, name: String): T {
            val code = node.get("code").asInt(-1)
            val message = node.get("message").asText("未知")

            if (code == -400) {
                throw TipsException("找不到${if (param != null) " $param " else ""}对应的${name}。")
            } else if (code != 0) {
                throw TipsException("获取${name}信息失败。失败代码：${code}，失败原因：${message}")
            } else try {
                return JacksonUtil.parseObject(node, T::class.java)
            } catch (e : Exception) {
                log.error("生成${name}失败。", e)
                return T::class.objectInstance!!
            }
        }
    }
}