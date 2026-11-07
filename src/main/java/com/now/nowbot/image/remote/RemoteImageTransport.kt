package com.now.nowbot.image.remote

import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.Closeable
import java.io.IOException
import java.io.InputStream
import java.net.Proxy
import java.util.concurrent.TimeUnit

internal fun interface RemoteImageTransport {
    @Throws(IOException::class)
    fun execute(url: HttpUrl, timeoutMillis: Long): RemoteImageResponse
}

internal class RemoteImageResponse(
    val statusCode: Int,
    val location: String?,
    val contentType: String?,
    val contentLength: Long,
    val body: InputStream?,
    private val closeable: Closeable? = null,
) : Closeable {
    override fun close() {
        try {
            body?.close()
        } finally {
            closeable?.close()
        }
    }
}

internal class OkHttpRemoteImageTransport(config: RemoteImageConfig) : RemoteImageTransport {
    private val client: OkHttpClient = OkHttpClient.Builder()
        .dns(PublicOnlyDns())
        // 固定直连，避免系统/环境代理绕过逐跳地址校验。
        .proxy(Proxy.NO_PROXY)
        // 重定向由 SafeRemoteImageSource 手动逐跳校验。
        .followRedirects(false)
        .followSslRedirects(false)
        .retryOnConnectionFailure(false)
        .cookieJar(CookieJar.NO_COOKIES)
        .connectTimeout(config.connectTimeoutMillis, TimeUnit.MILLISECONDS)
        .readTimeout(config.readTimeoutMillis, TimeUnit.MILLISECONDS)
        .writeTimeout(config.readTimeoutMillis, TimeUnit.MILLISECONDS)
        .callTimeout(config.callTimeoutMillis, TimeUnit.MILLISECONDS)
        .build()

    override fun execute(url: HttpUrl, timeoutMillis: Long): RemoteImageResponse {
        val request = Request.Builder()
            .url(url)
            .get()
            .header("Accept", "image/*")
            .header("User-Agent", USER_AGENT)
            .build()
        val call = client.newCall(request)
        call.timeout().timeout(timeoutMillis.coerceAtLeast(1L), TimeUnit.MILLISECONDS)
        val response = call.execute()
        val responseBody = response.body
        return RemoteImageResponse(
            statusCode = response.code,
            location = response.header("Location"),
            contentType = responseBody?.contentType()?.toString(),
            contentLength = responseBody?.contentLength() ?: -1L,
            body = if (response.code in 200..299) responseBody?.byteStream() else null,
            closeable = object : Closeable {
                override fun close() {
                    responseBody?.close()
                }
            },
        )
    }

    private companion object {
        private const val USER_AGENT = "yumu-remote-image/1.0"
    }
}
