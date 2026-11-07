package com.now.nowbot.image.markdown

import org.slf4j.LoggerFactory
import java.net.URI
import java.nio.file.Path
import java.util.Locale

internal class MarkdownAssetResolver(
    private val remote: (String) -> String?,
    private val local: (String) -> String?,
    private val fallbackImage: () -> String?,
) : MarkdownImageResolver {
    private val log = LoggerFactory.getLogger(MarkdownAssetResolver::class.java)

    override fun resolve(source: String): String? {
        val value = source.trim()
        require(value.isNotEmpty()) { "Markdown 图片地址为空" }

        if (value.startsWith("data:", ignoreCase = true)) {
            require(value.startsWith("data:image/", ignoreCase = true)) {
                "不支持的 data image"
            }
            return null
        }
        require(!value.startsWith("//")) { "不支持协议相对 Markdown 图片" }

        val scheme = schemeOf(value)
        if (scheme != null) {
            require(scheme == "http" || scheme == "https") {
                "不支持的 Markdown 图片协议: $scheme"
            }
            return remote(value) ?: fallback(value)
        }

        validateRelative(value)
        return local(value) ?: fallback(value)
    }

    private fun fallback(source: String): String {
        log.warn("Markdown 图片加载失败，使用默认图: {}", safeLabel(source))
        return fallbackImage()
            ?: throw IllegalStateException("Markdown 图片不可用且缺少默认图")
    }

    private fun validateRelative(value: String) {
        require(!value.startsWith("/") && !value.startsWith("\\")) {
            "不支持绝对 Markdown 图片路径"
        }
        require(!value.contains('\\') && !value.contains('\u0000')) {
            "非法 Markdown 图片路径"
        }
        require(!value.contains('?') && !value.contains('#')) {
            "Markdown 本地图片不支持 query/fragment"
        }
        val path = runCatching { Path.of(value) }.getOrNull()
            ?: throw IllegalArgumentException("非法 Markdown 图片路径")
        require(!path.isAbsolute && path.none { it.toString() == ".." }) {
            "Markdown 图片路径越界"
        }
    }

    private fun safeLabel(source: String): String {
        val value = source.trim()
        if (value.isEmpty()) return "empty"
        if (value.startsWith("data:", ignoreCase = true)) return "data image"
        if (value.startsWith("//")) return "protocol-relative URL"
        val scheme = schemeOf(value)
        if (scheme != null) {
            if (scheme != "http" && scheme != "https") return "scheme: $scheme"
            val host = runCatching { URI(value).host }.getOrNull()
            return if (host.isNullOrBlank()) "$scheme URL" else "$scheme://$host"
        }
        return "local relative image"
    }

    private fun schemeOf(value: String): String? {
        val colon = value.indexOf(':')
        if (colon <= 0) return null
        val slash = value.indexOf('/')
        if (slash in 0 until colon) return null
        return value.substring(0, colon).lowercase(Locale.ROOT)
    }
}
