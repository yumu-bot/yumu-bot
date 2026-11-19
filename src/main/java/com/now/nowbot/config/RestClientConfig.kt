package com.now.nowbot.config


import org.apache.hc.client5.http.config.RequestConfig
import org.apache.hc.client5.http.config.TlsConfig
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder
import org.apache.hc.core5.http.HttpHost
import org.apache.hc.core5.http.io.SocketConfig
import org.apache.hc.core5.http2.HttpVersionPolicy
import org.apache.hc.core5.util.Timeout
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpRequest
import org.springframework.http.MediaType
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.io.IOException
import java.util.concurrent.ThreadLocalRandom
import kotlin.math.pow

@Component
@Configuration
class RestClientConfig(
    osuConfig: OsuConfig,
    httpConfig: HttpConfig
) {

    val useProxy = osuConfig.useProxy != false

    /**
     * `yumu.http.version` 的全局默认值，作用于除 ppy.sb / eliteronix 之外的 RestClient。
     */
    private val defaultHttpVersion: HttpVersion = httpConfig.resolvedVersion.also {
        require(it != HttpVersion.HTTP_2) {
            "yumu.http.version=http2 暂不支持：RestClient 基于 Apache HttpClient 5 同步传输，只能使用 HTTP/1.1，" +
                    "请改为 auto 或 http1.1"
        }
    }

    private val otherConnectionPoolManager = createConnectionManager(
        defaultHttpVersion,
        Timeout.ofSeconds(5),
        soKeepAlive = true,
        tcpNoDelay = false
    )
    private val imageConnectionPoolManager = createConnectionManager(
        defaultHttpVersion,
        Timeout.ofSeconds(30),
        soKeepAlive = true,
        tcpNoDelay = true
    )

    private val otherRequestConfig: RequestConfig =
        createRequestConfig(defaultHttpVersion, Timeout.ofSeconds(10))

    private val imageRequestConfig: RequestConfig =
        createRequestConfig(defaultHttpVersion, Timeout.ofSeconds(20))

    private val httpClients: CloseableHttpClient = HttpClients.custom()
        .setConnectionManager(otherConnectionPoolManager)
        .setDefaultRequestConfig(otherRequestConfig)
        .build()

    @Bean("osuApiRestClient")
    @Qualifier("osuApiRestClient")
    fun osuApiRestClient(config: NowbotConfig): RestClient {
        val hasProxy = config.proxyHost != null && config.proxyPort != 0
        val connectionManager = createConnectionManager(
            defaultHttpVersion,
            Timeout.ofMilliseconds(500),
            soKeepAlive = true,
            tcpNoDelay = false,
            maxTotal = 50,
            maxPerRoute = 30
        )
        val httpClient = buildClient(
            config,
            connectionManager,
            createRequestConfig(defaultHttpVersion, Timeout.ofSeconds(30)),
            proxy = hasProxy && useProxy
        )

        val requestFactory = HttpComponentsClientHttpRequestFactory(httpClient)

        return RestClient.builder()
            .requestFactory(requestFactory)
            .baseUrl("https://osu.ppy.sh/api/v2/")
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .build()
    }

    @Bean("restClient")
    @Qualifier("restClient")
    @Primary
    fun restClient(config: NowbotConfig): RestClient =
        clientBuilder(httpClients)
            .configureMessageConverters { configurer ->
                configurer.registerDefaults()
                    .withJsonConverter(JacksonJsonHttpMessageConverter(config.objectMapper()))
            }
            .build()

    @Bean("noRetryRestClient")
    fun noRetryRestClient(config: NowbotConfig): RestClient {
        val hasProxy = config.proxyHost != null
        val connectionManager = createConnectionManager(
            defaultHttpVersion,
            Timeout.ofMilliseconds(500),
            soKeepAlive = true,
            tcpNoDelay = false,
            maxTotal = 50,
            maxPerRoute = 30
        )
        val httpClient = buildClient(
            config,
            connectionManager,
            createRequestConfig(defaultHttpVersion, Timeout.ofSeconds(30)),
            proxy = hasProxy && useProxy
        )

        val requestFactory = HttpComponentsClientHttpRequestFactory(httpClient)

        return noRetryClientBuilder(httpClient)
            .requestFactory(requestFactory)
            .baseUrl("https://osu.ppy.sh/api/v2/")
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .build()
    }

    @Bean("proxyRestClient")
    @Qualifier("proxyClient")
    fun proxyRestClient(config: NowbotConfig): RestClient =
        clientBuilder(proxyClient(config))
            .configureMessageConverters { configurer ->
                configurer.registerDefaults()
                    .withJsonConverter(JacksonJsonHttpMessageConverter(config.objectMapper()))
            }
            .build()

    // 水鱼应该是国内服务器, 不需要代理
    // 如果要换成走代理, 就替换 clientBuilder 参数中 httpClients -> proxyClient(config)
    @Bean("divingFishApiRestClient")
    @Qualifier("divingFishApiRestClient")
    fun divingFishApiRestClient(
        divingFishConfig: DivingFishConfig,
        config: NowbotConfig
    ): RestClient =
        clientBuilder(httpClients)
            .baseUrl(divingFishConfig.url)
            .configureMessageConverters { configurer ->
                configurer.registerDefaults()
                    .withJsonConverter(JacksonJsonHttpMessageConverter(config.objectMapper()))
            }
            .build()

    @Bean("lxnsApiRestClient")
    @Qualifier("lxnsApiRestClient")
    fun lxnsApiRestClient(
        lxnsConfig: LxnsConfig,
        config: NowbotConfig
    ): RestClient = clientBuilder(httpClients)
        .baseUrl(lxnsConfig.url)
        .configureMessageConverters { configurer ->
            configurer.registerDefaults()
                .withJsonConverter(JacksonJsonHttpMessageConverter(config.objectMapper()))
        }
        .build()

    @Bean("biliApiRestClient")
    @Qualifier("biliApiRestClient")
    fun biliApiRestClient(config: NowbotConfig): RestClient = clientBuilder(httpClients)
        .baseUrl("https://api.bilibili.com/")
        .configureMessageConverters { configurer ->
            configurer.registerDefaults()
                .withJsonConverter(JacksonJsonHttpMessageConverter(config.objectMapper()))
        }
        .build()

    /**
     * ppy.sb 固定使用 HTTP/1.1。
     */
    @Bean("sbApiRestClient")
    @Qualifier("sbApiRestClient")
    fun sbApiRestClient(
        config: NowbotConfig
    ): RestClient = clientBuilder(fixedHttp11Client(config, Timeout.ofSeconds(10), proxy = config.proxyPort != 0))
        .baseUrl("https://api.ppy.sb/")
        .configureMessageConverters { configurer ->
            configurer.registerDefaults()
                .withJsonConverter(JacksonJsonHttpMessageConverter(config.objectMapper()))
        }
        .build()

    /**
     * eliteronix（ETX 对决分）固定使用 HTTP/1.1。
     */
    @Bean("etxApiRestClient")
    @Qualifier("etxApiRestClient")
    fun etxApiRestClient(
        config: NowbotConfig
    ): RestClient = clientBuilder(
        fixedHttp11Client(config, Timeout.ofSeconds(30), proxy = config.proxyPort != 0 && useProxy)
    )
        .baseUrl("https://www.eliteronix.de/")
        .configureMessageConverters { configurer ->
            configurer.registerDefaults()
                .withJsonConverter(JacksonJsonHttpMessageConverter(config.objectMapper()))
        }
        .build()

    @Bean("imageRestClient")
    @Qualifier("imageRestClient")
    fun imageRestClient(
        config: NowbotConfig
    ): RestClient = clientBuilder(
        HttpClients
            .custom()
            .setDefaultRequestConfig(imageRequestConfig)
            .setConnectionManager(imageConnectionPoolManager)
            .build()
    )
        .configureMessageConverters { configurer ->
            configurer.registerDefaults()
                .withJsonConverter(JacksonJsonHttpMessageConverter(config.objectMapper()))
        }
        .build()

    private fun proxyClient(config: NowbotConfig): CloseableHttpClient = buildClient(
        config,
        otherConnectionPoolManager,
        otherRequestConfig,
        proxy = config.proxyPort != 0
    )

    /**
     * 固定 HTTP/1.1 的客户端，不受 `yumu.http.version` 影响。
     */
    private fun fixedHttp11Client(
        config: NowbotConfig,
        responseTimeout: Timeout,
        proxy: Boolean
    ): CloseableHttpClient = buildClient(
        config,
        createConnectionManager(
            HttpVersion.HTTP_1_1,
            Timeout.ofSeconds(5),
            soKeepAlive = true,
            tcpNoDelay = false
        ),
        createRequestConfig(HttpVersion.HTTP_1_1, responseTimeout),
        proxy
    )

    private fun buildClient(
        config: NowbotConfig,
        connectionManager: PoolingHttpClientConnectionManager,
        requestConfig: RequestConfig,
        proxy: Boolean
    ): CloseableHttpClient {
        val clientBuilder = HttpClients.custom()
            .setConnectionManager(connectionManager)
            .setDefaultRequestConfig(requestConfig)

        if (proxy) {
            clientBuilder.setProxy(HttpHost(config.proxyHost, config.proxyPort))
        }

        return clientBuilder.build()
    }

    companion object {

        private fun createConnectionManager(
            version: HttpVersion,
            socketTimeout: Timeout,
            tcpNoDelay: Boolean = false,
            soKeepAlive: Boolean = true,
            maxTotal: Int = 1000,
            maxPerRoute: Int = 200
        ): PoolingHttpClientConnectionManager =
            PoolingHttpClientConnectionManagerBuilder.create()
                // FORCE_HTTP_1 会把 TLS ALPN 限制为 http/1.1，避免服务端协商到 h2
                .setDefaultTlsConfig(
                    TlsConfig.custom()
                        .setVersionPolicy(version.toVersionPolicy())
                        .build()
                )
                .setMaxConnTotal(maxTotal)
                .setMaxConnPerRoute(maxPerRoute)
                .setDefaultSocketConfig(
                    SocketConfig.custom()
                        .setSoTimeout(socketTimeout)
                        .setSoKeepAlive(soKeepAlive)
                        .setTcpNoDelay(tcpNoDelay)
                        .build()
                )
                .build()

        private fun createRequestConfig(version: HttpVersion, responseTimeout: Timeout): RequestConfig =
            RequestConfig.custom()
                .setResponseTimeout(responseTimeout)
                // 关闭 h2c 明文升级；HTTPS 的 ALPN 由 connectionManager 的 TlsConfig 控制
                .setProtocolUpgradeEnabled(version != HttpVersion.HTTP_1_1)
                .build()

        /**
         * 使用默认参数预先构造好的请求
         * 1000 个并发连接, 200 个相同路由连接
         * 100 ms 的 tcp 握手超时
         * 10s 的请求超时
         * 默认失败重试 3 次(非 500, 429 错误)
         * json 数据发送 / 响应
         */
        fun clientBuilder(clients: CloseableHttpClient): RestClient.Builder {
            val requestFactory = HttpComponentsClientHttpRequestFactory(clients)
            return RestClient
                .builder()
                .requestFactory(requestFactory)
                .requestInterceptor(RestClientRetryInterceptor(3))
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        }

        fun noRetryClientBuilder(clients: CloseableHttpClient): RestClient.Builder {
            val requestFactory = HttpComponentsClientHttpRequestFactory(clients)
            return RestClient
                .builder()
                .requestFactory(requestFactory)
                // 注意：这里不添加 RestClientRetryInterceptor
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        }
    }

    class RestClientRetryInterceptor(
        private val maxAttempts: Int = 3,
        private val initialBackoffMs: Long = 2000,
        private val jitterFactor: Double = 0.1
    ) : ClientHttpRequestInterceptor {

        private val log = LoggerFactory.getLogger(javaClass)

        private val shouldRetryCode = setOf(504, 503, 502, 429, 408)

        override fun intercept(
            request: HttpRequest,
            body: ByteArray,
            execution: ClientHttpRequestExecution
        ): ClientHttpResponse {
            var lastException: Exception? = null
            var attempt = 0
            do {
                performBackoff(attempt)
                try {
                    val response = execution.execute(request, body)
                    if (shouldRetry(response.statusCode.value())) {
                        log.warn("Attempt $attempt: Received ${response.statusCode}, retrying ${request.uri}")
                        response.close()
                    } else {
                        return response
                    }
                } catch (e: IOException) {
                    lastException = e
                    log.warn("Attempt $attempt: Network error ${e.message}, retrying ${request.uri}")
                }
                attempt++
            } while (attempt < maxAttempts)

            throw lastException ?: RuntimeException("Retry exhausted after $maxAttempts attempts")
        }

        private fun shouldRetry(code: Int) = shouldRetryCode.contains(code)

        private fun performBackoff(attempt: Int) {
            if (attempt == 0) return
            val backoff = initialBackoffMs * 2.0.pow((attempt - 1).toDouble()).toLong()

            val jitter = (backoff * jitterFactor).toLong()
            val sleepTime = backoff + ThreadLocalRandom.current().nextLong(-jitter, jitter)

            Thread.sleep(maxOf(0, sleepTime))
        }
    }
}

/**
 * HTTP 客户端版本配置，对应 `yumu.http.version`。
 *
 * 当前 RestClient 使用 Apache HttpClient 5 的同步（classic）传输，只能处理 HTTP/1.1。
 * 强制 HTTP/1.1 会同时做两件事：
 * 1. 关闭 h2c 明文协议升级（不再发送 `Upgrade: h2c`）；
 * 2. 把 TLS ALPN 的提议限制为 `http/1.1`，避免服务端选中 `h2` 后客户端仍按 HTTP/1.1 发送数据。
 */
@ConfigurationProperties(prefix = "yumu.http", ignoreInvalidFields = true)
class HttpConfig {
    /**
     * 全局默认 HTTP 版本，可选值：
     * - `auto`：交给底层客户端协商（即 Apache 默认的 NEGOTIATE）
     * - `http1.1`：强制 HTTP/1.1
     * - `http2`：强制 HTTP/2（当前同步客户端不支持，启动时会报错）
     *
     * ppy.sb 与 eliteronix 的 RestClient 固定使用 HTTP/1.1，不受这里影响。
     */
    var version: String = "auto"

    val resolvedVersion: HttpVersion
        get() = HttpVersion.parse(version)
}

enum class HttpVersion {
    AUTO,
    HTTP_1_1,
    HTTP_2;

    fun toVersionPolicy(): HttpVersionPolicy = when (this) {
        AUTO -> HttpVersionPolicy.NEGOTIATE
        HTTP_1_1 -> HttpVersionPolicy.FORCE_HTTP_1
        HTTP_2 -> HttpVersionPolicy.FORCE_HTTP_2
    }

    companion object {
        /**
         * 兼容 `http1.1` / `http/1.1` / `http-1-1` / `h2` / `auto` 等写法。
         */
        fun parse(value: String?): HttpVersion {
            val normalized = value.orEmpty()
                .trim()
                .lowercase()
                .replace("-", "")
                .replace("_", "")
                .replace("/", "")
                .replace(".", "")
                .replace(" ", "")

            return when (normalized) {
                "", "auto", "negotiate" -> AUTO
                "http1", "http11", "1", "11" -> HTTP_1_1
                "http2", "http20", "h2", "2", "20" -> HTTP_2
                else -> throw IllegalArgumentException(
                    "无法识别的 yumu.http.version: '$value'，可选值: auto / http1.1 / http2"
                )
            }
        }
    }
}
