package com.now.nowbot.config


import org.apache.hc.client5.http.config.RequestConfig
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager
import org.apache.hc.core5.http.HttpHost
import org.apache.hc.core5.http.io.SocketConfig
import org.apache.hc.core5.util.Timeout
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
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
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.io.IOException
import java.util.concurrent.ThreadLocalRandom
import kotlin.math.pow

@Component
@Configuration
class RestClientConfig {
    @Bean("osuApiRestClient")
    @Qualifier("osuApiRestClient")
    @Primary
    fun osuApiRestClient(config: NowbotConfig): RestClient {
        val hasProxy = config.proxyHost != null
        val connectionManager = PoolingHttpClientConnectionManager()

        connectionManager.apply {
            maxTotal = 50
            defaultMaxPerRoute = 30
            defaultSocketConfig = SocketConfig.custom()
                .setSoTimeout(Timeout.ofMilliseconds(500))
                .setSoKeepAlive(true)
                .setTcpNoDelay(false) // 禁用 Nagle 算法
                .build();
        }
        val requestConfig = RequestConfig.custom()
            .setResponseTimeout(Timeout.ofSeconds(30))
            .build()

        val httpClientBuilder = HttpClients.custom()
            .setConnectionManager(connectionManager)
            .setDefaultRequestConfig(requestConfig)

        if (config.proxyHost != null) {
            val proxy = HttpHost(
                config.proxyHost,
                config.proxyPort
            )
            httpClientBuilder.setProxy(proxy)
        }

        val httpClient = httpClientBuilder.build()

        val requestFactory = HttpComponentsClientHttpRequestFactory(httpClient)

        return RestClient.builder()
            .requestFactory(requestFactory)
            .baseUrl("https://osu.ppy.sh/api/v2/")
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .build()
    }

    @Bean("restClient")
    @Qualifier("rlient")
    fun restClient(): RestClient =
        clientBuilder(httpClients)
            .build()

    @Bean("proxyRestClient")
    @Qualifier("proxyClient")
    fun proxyRestClient(config: NowbotConfig): RestClient =
        clientBuilder(proxyClient(config))
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
            .build()

    @Bean("lxnsApiRestClient")
    @Qualifier("lxnsApiRestClient")
    fun lxnsApiRestClient(
        lxnsConfig: LxnsConfig,
    ): RestClient = clientBuilder(httpClients)
        .baseUrl(lxnsConfig.url)
        .build()

    @Bean("biliApiRestClient")
    @Qualifier("biliApiRestClient")
    fun biliApiRestClient(): RestClient = clientBuilder(httpClients)
        .baseUrl("https://api.bilibili.com/")
        .build()

    @Bean("sbApiRestClient")
    @Qualifier("sbApiRestClient")
    fun sbApiRestClient(
        config: NowbotConfig
    ): RestClient = clientBuilder(proxyClient(config))
        .baseUrl("https://api.ppy.sb/")
        .build()

    companion object {
        val otherConnectionPoolManager = PoolingHttpClientConnectionManager().apply {
            maxTotal = 1000
            defaultMaxPerRoute = 200
            defaultSocketConfig = SocketConfig.custom()
                .setSoTimeout(Timeout.ofMilliseconds(100))
                .setSoKeepAlive(true)
                .setTcpNoDelay(false)
                .build();
        }
        val otherRequestConfig: RequestConfig = RequestConfig.custom()
            .setResponseTimeout(Timeout.ofSeconds(10))
            .build()

        fun proxyClient(config: NowbotConfig): CloseableHttpClient {
            val proxy = HttpHost(
                config.proxyHost,
                config.proxyPort
            )
            return HttpClients.custom()
                .setConnectionManager(otherConnectionPoolManager)
                .setDefaultRequestConfig(otherRequestConfig)
                .setProxy(proxy)
                .build()
        }

        val httpClients: CloseableHttpClient = HttpClients.custom()
            .setConnectionManager(otherConnectionPoolManager)
            .setDefaultRequestConfig(otherRequestConfig)
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
                        break
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