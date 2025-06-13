package com.now.nowbot.config

import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Primary

@Primary @ConfigurationProperties(prefix = "yumu")
class YumuConfig {
    /**
     * 访问是否需要加端口, 默认不加, 如果对外访问需要则端口则填写
     */
    private var publicPort: Int = 0

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
        get() = "${publicDomain}${if(publicPort == 0) "" else ":${publicPort}"}"

    private fun getRowDomain(s: String): String {
        val n: Int
        if ((s.indexOf(':', 7).also { n = it }) >= 0) {
            return s.substring(0, n)
        }
        return s
    }

    val privateUrl: String
        get() = "${publicDomain}${if(privatePort == 0) "" else ":${privatePort}"}"

    @PostConstruct fun init() {
        publicDomain = getRowDomain(publicDomain)
        privateDomain = getRowDomain(privateDomain)
        if (publicDomain.isBlank()) publicDomain = privateDomain
        if (publicDomain == privateDomain && publicPort == 0) publicPort = privatePort!!
    }
}
