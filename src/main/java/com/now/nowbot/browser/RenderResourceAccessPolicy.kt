package com.now.nowbot.browser

import java.net.URI
import java.util.Locale

internal object RenderResourceAccessPolicy {
    private val INLINE_SCHEMES = setOf("data", "blob", "about")
    private val RESOURCE_PATH = Regex(
        "^/(?:[A-Za-z0-9._~-]+/)*render-resource/[A-Za-z0-9_-]{32}${'$'}",
    )

    fun isAllowed(url: String, allowedResourceUrls: Set<String>): Boolean {
        val scheme = schemeOf(url) ?: return false
        if (scheme in INLINE_SCHEMES) return true
        if (scheme != "http" && scheme != "https") return false
        val normalized = normalize(url) ?: return false
        return normalized in allowedResourceUrls
    }

    fun isSafeResourceUrl(url: String): Boolean {
        val uri = runCatching { URI(url) }.getOrNull() ?: return false
        if (uri.isOpaque) return false
        val scheme = uri.scheme?.lowercase(Locale.ROOT) ?: return false
        if (scheme != "http" && scheme != "https") return false
        if (uri.rawUserInfo != null || uri.rawQuery != null || uri.rawFragment != null) return false
        if (uri.host.isNullOrBlank()) return false
        val path = uri.rawPath ?: return false
        if (path.contains('\\') || path.contains('%') || path.contains("//")) return false
        if (path.split('/').any { it == "." || it == ".." }) return false
        return RESOURCE_PATH.matches(path)
    }

    fun normalize(url: String): String? {
        if (!isSafeResourceUrl(url)) return null
        val uri = runCatching { URI(url) }.getOrNull() ?: return null
        val scheme = uri.scheme?.lowercase(Locale.ROOT) ?: return null
        val host = uri.host?.lowercase(Locale.ROOT) ?: return null
        val path = uri.rawPath ?: return null
        val port = when {
            scheme == "http" && uri.port == 80 -> -1
            scheme == "https" && uri.port == 443 -> -1
            else -> uri.port
        }
        val authority = if (port == -1) host else "$host:$port"
        return "$scheme://$authority$path"
    }

    fun normalizeAll(urls: Set<String>): Set<String> =
        urls.mapNotNullTo(LinkedHashSet<String>()) { normalize(it) }

    private fun schemeOf(url: String): String? {
        val separator = url.indexOf(':')
        if (separator <= 0) return null
        return url.substring(0, separator).lowercase(Locale.ROOT)
    }
}
