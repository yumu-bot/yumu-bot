package com.now.nowbot.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Primary
import org.springframework.validation.annotation.Validated

@Primary @Validated @ConfigurationProperties(prefix = "yumu.osu", ignoreInvalidFields = true)
class OsuConfig {
    /**
     * 接口路径, 一般不用改
     */
    var url: String = "https://osu.ppy.sh/api/v2/"

    /**
     * 回调链接, 需要与 osu oauth 应用的callback url 完全一致
     * 默认不需要配置, 自动构造 publicDomain+callBackUrl
     * 也可以自行配置, 强制覆盖
     */
    var callbackUrl: String = ""

    /**
     * 回调的api端口
     */
    var callbackPath: String = "/bind"

    var id: Int = 0

    var token: String = "*"
}
