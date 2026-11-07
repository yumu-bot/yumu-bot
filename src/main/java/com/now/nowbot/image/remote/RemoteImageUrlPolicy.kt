package com.now.nowbot.image.remote

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.util.Locale

internal class RemoteImageUrlPolicy(private val config: RemoteImageConfig) {
    fun parse(value: String): HttpUrl? {
        val raw = value.trim()
        if (!isAcceptableRaw(raw)) return null
        val url = raw.toHttpUrlOrNull() ?: return null
        return validate(url)
    }

    fun resolve(base: HttpUrl, location: String): HttpUrl? {
        val raw = location.trim()
        if (!isAcceptableRaw(raw)) return null
        val next = base.resolve(raw) ?: return null
        if (base.scheme == "https" && next.scheme == "http") return null
        return validate(next)
    }

    private fun isAcceptableRaw(value: String): Boolean {
        if (value.isEmpty() || value.length > config.maxUrlLength) return false
        if (value.any { it <= '\u0020' || it == '\u007f' }) return false
        return !value.contains('\\')
    }

    private fun validate(url: HttpUrl): HttpUrl? {
        if (url.scheme != "http" && url.scheme != "https") return null
        if (url.scheme == "http" && !config.allowHttp) return null
        if (url.scheme == "http" && url.port != 80) return null
        if (url.scheme == "https" && url.port != 443) return null
        if (url.encodedUsername.isNotEmpty() || url.encodedPassword.isNotEmpty()) return null
        if (url.toString().length > config.maxUrlLength) return null

        val host = url.host.lowercase(Locale.ROOT)
        if (!RemoteImageAddresses.isValidHost(host)) return null
        if (!isAllowedHost(host)) return null

        return if (url.fragment == null) url else url.newBuilder().fragment(null).build()
    }

    private fun isAllowedHost(host: String): Boolean {
        if (config.allowedHosts.isEmpty()) return true
        return config.allowedHosts.any { entry ->
            val allowed = entry.trim().lowercase(Locale.ROOT)
            allowed.isNotEmpty() && (host == allowed || host.endsWith(".$allowed"))
        }
    }
}
