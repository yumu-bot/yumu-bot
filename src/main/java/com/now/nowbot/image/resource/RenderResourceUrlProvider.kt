package com.now.nowbot.image.resource

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.server.context.WebServerInitializedEvent
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Component
import org.springframework.web.context.WebApplicationContext

@Component
class RenderResourceUrlProvider(
    private val config: RenderResourceConfig,
    @Value("\${server.address:}") private val serverAddress: String,
    @Value("\${server.ssl.enabled:false}") private val sslEnabled: Boolean,
    @Value("\${server.ssl.bundle:}") private val sslBundle: String,
) : ApplicationListener<WebServerInitializedEvent>, ApplicationContextAware {
    private val httpsConfigured = sslEnabled || sslBundle.isNotBlank()

    @Volatile
    private var port: Int = -1

    @Volatile
    private var contextPath: String = ""

    @Volatile
    private var applicationContext: ApplicationContext? = null

    override fun setApplicationContext(applicationContext: ApplicationContext) {
        this.applicationContext = applicationContext
    }

    override fun onApplicationEvent(event: WebServerInitializedEvent) {
        val eventContext = event.applicationContext
        if (eventContext.serverNamespace != null) return
        val ownContext = applicationContext
        if (ownContext != null && eventContext !== ownContext) return
        val path = (eventContext as? WebApplicationContext)
            ?.servletContext
            ?.contextPath
            .orEmpty()
        updateServer(event.webServer.port, path)
    }

    fun updateServer(port: Int, contextPath: String) {
        this.port = port
        this.contextPath = normalizeContextPath(contextPath)
    }

    fun urlFor(id: String): String = "${baseUrl()}${RenderResourcePaths.ENDPOINT}/$id"

    internal fun baseUrl(): String {
        val configured = config.localBaseUrl.trim()
        if (configured.isNotEmpty()) {
            return configured.trimEnd('/')
        }
        check(!httpsConfigured) { "HTTPS 已启用，请配置 render.resource.local-base-url" }
        val actualPort = port
        check(actualPort > 0) { "Web 服务器尚未就绪，无法生成渲染资源 URL" }
        return "http://${host()}:$actualPort$contextPath"
    }

    private fun host(): String {
        val address = serverAddress.trim()
        val host = when {
            address.isEmpty() -> LOOPBACK
            address == "0.0.0.0" -> LOOPBACK
            address == "::" || address == "[::]" -> LOOPBACK
            address == "0:0:0:0:0:0:0:0" -> LOOPBACK
            else -> address
        }
        return if (host.contains(':') && !host.startsWith("[")) "[$host]" else host
    }

    private fun normalizeContextPath(value: String): String {
        val path = value.trim()
        return if (path.isEmpty() || path == "/") "" else "/" + path.trim('/')
    }

    companion object {
        private const val LOOPBACK = "127.0.0.1"
    }
}
