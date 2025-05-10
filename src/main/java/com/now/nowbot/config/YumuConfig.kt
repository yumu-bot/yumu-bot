package com.now.nowbot.config

import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Primary
import org.springframework.validation.annotation.Validated

@Primary
@Validated
@ConfigurationProperties(prefix = "yumu")
class YumuConfig {
    /**
     * 访问是否需要加端口, 默认不加, 如果对外访问需要则端口则填写
     */
    var publicPort: Int = 0

    /**
     * 访问内网的端口, 默认与server端口保持一致
     */
    @Value("\${server.port}")
    var privatePort: Int? = null

    /**
     * 公网可以访问到的路径
     */
    private var publicDomain: String = ""

    /**
     * 内网设备可以访问到的路径
     */
    private var privateDomain: String = "http://localhost"

    var bindDomain: String = ""

    /**
     * 私域设备的qq号
     */
    var privateDevice: List<Long> = ArrayList(0)

    val publicUrl: String
        get() = "${publicDomain}${if (privatePort == null || privatePort == 0) "" else ":${privatePort}"}"

    private fun getRawDomain(s: String): String {
        return if (s.contains(':')) {
            s.substring(0, s.indexOf(':', 7))
        } else s
    }

    val privateUrl: String
        get() = "${privateDomain}${if (privatePort == null || privatePort == 0) "" else ":${privatePort}"}"

    @PostConstruct fun init() {
        publicDomain = getRawDomain(publicDomain)
        privateDomain = getRawDomain(privateDomain)
        if (publicDomain.isEmpty()) publicDomain = privateDomain
        if (publicDomain == privateDomain && publicPort == 0) publicPort = privatePort!!
    }
}
