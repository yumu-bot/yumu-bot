package com.now.nowbot.controller

import com.fasterxml.jackson.databind.JsonNode
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/discord")
class DiscordInteractionController {

    @PostMapping("/interactions")
    fun handleInteraction(@RequestBody body: JsonNode): Map<String, Any> {
        log.info("收到 Discord 交互请求，类型: ${body.get("type")?.asInt()}")

        // Discord 会发送 type=1 的 Ping 请求来验证端点
        if (body.get("type")?.asInt() == 1) {
            log.info("响应 Discord Ping 验证")
            return mapOf("type" to 1)
        }

        // 对于其他类型的交互，返回默认响应
        log.info("未知的交互类型: ${body.get("type")?.asInt()}")
        return mapOf("type" to 1)
    }

    companion object {
        private val log = LoggerFactory.getLogger(DiscordInteractionController::class.java)
    }
}