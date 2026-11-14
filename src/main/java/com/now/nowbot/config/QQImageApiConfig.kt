package com.now.nowbot.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

/**
 * QQ 机器人素材上传（Markdown 图片）配置。
 *
 * yaml 示例：
 * ```yaml
 * yumu:
 *   qq-image:
 *     access-token: ""
 *     # 以下一般不用改
 *     api-url: "https://api.bot.qq.com"
 *     # 是否把「文字 + 图片」的普通消息自动转成带图的 Markdown 消息
 *     auto-markdown: true
 * ```
 *
 * 不配置时 QQImageApiService 会保持不可用，普通消息照常发送，不影响启动和其他功能。
 */
@Validated
@ConfigurationProperties(prefix = "yumu.qq-image", ignoreInvalidFields = true)
class QQImageApiConfig {

    /**
     * 已经换好的 access_token。
     */
    var accessToken: String = ""

    /**
     * OpenAPI 根地址, 素材相关接口都在这个域名下
     */
    var apiUrl: String = "https://api.bot.qq.com"


    /**
     * 是否把「文字 + 图片」的普通富文本消息自动转成带图 Markdown 消息。
     *
     * 依赖素材接口可用（[accessToken] 或 app-id / client-secret 至少配一组）。
     */
    var autoMarkdown: Boolean = true
}
