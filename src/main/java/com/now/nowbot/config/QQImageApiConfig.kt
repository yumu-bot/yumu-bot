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
 *     # 开放平台机器人 AppID / ClientSecret
 *     app-id: "1020xxxxx"
 *     client-secret: "xxxxxxxxxxxxxxxxxxxxxxxx"
 *     # 已经换好的 access_token, 填了就不用再去换, 直接拿它当凭证
 *     access-token: ""
 *     # 以下一般不用改
 *     api-url: "https://api.bot.qq.com"
 *     token-url: "https://api.bot.qq.com/app/getAppAccessToken"
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
     * 开放平台机器人 AppID, 用于换 access_token
     */
    var appId: String = ""

    /**
     * 开放平台机器人 ClientSecret, 用于换 access_token
     */
    var clientSecret: String = ""

    /**
     * 已经换好的 access_token。
     *
     * 填了之后就不再调用 [tokenUrl] 换取，直接把它当作所有素材接口的凭证；
     * 适合临时调试、或者不想把 app-id / client-secret 写进配置文件的情况。
     * 注意它有自己的有效期（一般 2 小时），过期后素材接口会被拒绝，长期运行建议还是配 app-id / client-secret。
     */
    var accessToken: String = ""

    /**
     * OpenAPI 根地址, 素材相关接口都在这个域名下
     */
    var apiUrl: String = "https://api.bot.qq.com"

    /**
     * access_token 接口地址
     */
    var tokenUrl: String = "https://api.bot.qq.com/app/getAppAccessToken"

    /**
     * 是否把「文字 + 图片」的普通富文本消息自动转成带图 Markdown 消息。
     *
     * 依赖素材接口可用（[accessToken] 或 app-id / client-secret 至少配一组）。
     */
    var autoMarkdown: Boolean = true

    /**
     * 是否已经填好了凭证，可以调用素材接口
     */
    fun isConfigured(): Boolean {
        if (apiUrl.isBlank()) return false
        return accessToken.isNotBlank() || (appId.isNotBlank() && clientSecret.isNotBlank())
    }

    /**
     * 是否用固定 token 而不是现换
     */
    fun hasFixedToken(): Boolean = accessToken.isNotBlank()
}
