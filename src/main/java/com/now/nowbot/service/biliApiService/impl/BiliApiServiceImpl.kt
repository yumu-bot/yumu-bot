package com.now.nowbot.service.biliApiService.impl

import com.fasterxml.jackson.databind.JsonNode
import com.now.nowbot.model.bili.BiliDanmaku
import com.now.nowbot.model.bili.BiliStreamer
import com.now.nowbot.model.bili.BiliUser
import com.now.nowbot.service.biliApiService.BiliApiService
import com.now.nowbot.throwable.TipsException
import com.now.nowbot.util.JacksonUtil
import org.springframework.stereotype.Service

@Service
class BiliApiServiceImpl(
    private val base: BiliApiBaseService,
): BiliApiService {
    override fun getStreamer(id: Long): BiliStreamer {
        val biliStreamer = base.biliApiWebClient.get().uri { it
            .scheme("https")
            .host("api.live.bilibili.com")
            .path("live_user/v1/Master/info")
            .queryParam("uid", id)
            .build()
        }
            .headers { base.insertJSONHeader(it) }
            .retrieve()
            .bodyToMono(BiliStreamer::class.java).block()!!

        if (biliStreamer.data == null) {
            if (biliStreamer.code == -400) {
                throw TipsException("找不到 $id 对应的直播主。")
            }

            throw TipsException("获取直播主信息失败。失败代码：${biliStreamer.code}，失败原因：${biliStreamer.message}")
        }

        return biliStreamer
    }

    override fun getUser(id: Long): BiliUser {
        val biliUser = base.biliApiWebClient.get().uri { it
            .scheme("https")
            .path("x/space/acc/info")
            .queryParam("mid", id)
            .build()
        }
            .headers { base.insertJSONHeader(it) }
            .retrieve()
            .bodyToMono(JsonNode::class.java)
            .map { println(it)
                JacksonUtil.parseObject(it, BiliUser::class.java) }.block()!!
            //.bodyToMono(BiliUser::class.java).block()!!

        if (biliUser.data == null) {
            if (biliUser.code == -400) {
                throw TipsException("找不到 $id 对应的用户。")
            }

            throw TipsException("获取账号信息失败。失败代码：${biliUser.code}，失败原因：${biliUser.message}")
        }

        return biliUser
    }

    override fun getDanmaku(roomID: Long): BiliDanmaku {
        val biliDanmaku = base.biliApiWebClient.get().uri { it
            .scheme("https")
            .host("api.live.bilibili.com")
            .path("xlive/web-room/v1/dM/gethistory")
            .queryParam("roomID", roomID)
            .build()
        }
            .headers { base.insertJSONHeader(it) }
            .retrieve()
            .bodyToMono(BiliDanmaku::class.java).block()!!

        if (biliDanmaku.data == null) {
            throw TipsException("获取直播间最近弹幕失败。失败代码：${biliDanmaku.code}，失败原因：${biliDanmaku.message}")
        }

        return biliDanmaku
    }

    override fun getImage(url: String): ByteArray {
        val avatar: ByteArray? = base.webClient.get()
            .uri(url)
            .retrieve()
            .bodyToMono(ByteArray::class.java)
            .block()

        return avatar ?: throw TipsException("获取图片失败。")
    }
}