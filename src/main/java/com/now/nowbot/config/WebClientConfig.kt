package com.now.nowbot.config

import com.now.nowbot.util.JacksonUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.http.codec.ClientCodecConfigurer
import org.springframework.http.codec.ServerCodecConfigurer
import org.springframework.http.codec.json.Jackson2JsonEncoder
import org.springframework.stereotype.Component
import org.springframework.web.reactive.config.WebFluxConfigurer
import org.springframework.web.reactive.function.client.*
import reactor.core.Exceptions
import reactor.core.publisher.Mono
import reactor.netty.http.client.HttpClient
import reactor.netty.resources.ConnectionProvider
import reactor.netty.transport.ProxyProvider
import reactor.util.retry.Retry
import reactor.util.retry.Retry.RetrySignal
import java.time.Duration
import java.util.function.Consumer
import java.util.function.Function

@Component @Configuration class WebClientConfig : WebFluxConfigurer {
    override fun configureHttpMessageCodecs(configurer: ServerCodecConfigurer) {
        configurer.defaultCodecs().maxInMemorySize(20 * 1024 * 1024)
    }

    @Bean("osuApiWebClient") @Qualifier("osuApiWebClient") @Primary fun osuApiWebClient(builder: WebClient.Builder): WebClient {/*
         * Setting maxIdleTime as 30s, because servers usually have a keepAliveTimeout of 60s, after which the connection gets closed.
         * If the connection pool has any connection which has been idle for over 10s, it will be evicted from the pool.
         * Refer https://github.com/reactor/reactor-netty/issues/1318#issuecomment-702668918
         */
        val connectionProvider = ConnectionProvider.builder("connectionProvider")
            .maxIdleTime(Duration.ofSeconds(30))
            .maxConnections(200)
            .pendingAcquireMaxCount(-1)
            .build()
        val httpClient = HttpClient.create(connectionProvider) //                .proxy(proxy ->
            //                        proxy.type("HTTP".equalsIgnoreCase(config.proxyType) ? ProxyProvider.Proxy.HTTP : ProxyProvider.Proxy.SOCKS5)
            //                                .host(config.proxyHost)
            //                                .port(config.proxyPort)
            //                )
            .followRedirect(true).responseTimeout(Duration.ofSeconds(15))
        val connector = ReactorClientHttpConnector(httpClient)
        val strategies = ExchangeStrategies.builder().codecs { clientDefaultCodecsConfigurer: ClientCodecConfigurer ->
                clientDefaultCodecsConfigurer.defaultCodecs().jackson2JsonEncoder(
                    Jackson2JsonEncoder(
                        JacksonUtil.mapper, MediaType.APPLICATION_JSON
                    )
                )
            }.build()

        return builder.clientConnector(connector).exchangeStrategies(strategies)
            .defaultHeaders { headers: HttpHeaders ->
                headers.contentType = MediaType.APPLICATION_JSON
                headers.accept = listOf(MediaType.APPLICATION_JSON)
            }.baseUrl("https://osu.ppy.sh/api/v2/")
            .codecs { codecs: ClientCodecConfigurer -> codecs.defaultCodecs().maxInMemorySize(Int.MAX_VALUE) }.build()
    }

    @Bean("divingFishApiWebClient") @Qualifier("divingFishApiWebClient") fun divingFishApiWebClient(builder: WebClient.Builder, divingFishConfig: DivingFishConfig): WebClient {
        val connectionProvider = ConnectionProvider.builder("connectionProvider2")
            .maxIdleTime(Duration.ofSeconds(30))
            .maxConnections(200)
            .pendingAcquireMaxCount(-1)
            .build()
        val httpClient = HttpClient.create(connectionProvider) // 国内访问即可，无需设置梯子
            .followRedirect(true).responseTimeout(Duration.ofSeconds(30))
        val connector = ReactorClientHttpConnector(httpClient)
        val strategies = ExchangeStrategies.builder().codecs {
            it.defaultCodecs().jackson2JsonEncoder(
                Jackson2JsonEncoder(
                    JacksonUtil.mapper, MediaType.APPLICATION_JSON
                )
            )
        }.build()

        return builder.clientConnector(connector).exchangeStrategies(strategies)
            .defaultHeaders { headers: HttpHeaders ->
                headers.contentType = MediaType.APPLICATION_JSON
                headers.accept = listOf(MediaType.APPLICATION_JSON)
            }.baseUrl(divingFishConfig.url!!)
            .codecs { it.defaultCodecs().maxInMemorySize(Int.MAX_VALUE) }
            .filter { request: ClientRequest, next: ExchangeFunction -> this.doRetryFilter(request, next)
            }.build()
    }

    @Bean("biliApiWebClient") @Qualifier("biliApiWebClient") fun biliApiWebClient(builder: WebClient.Builder): WebClient {
        val connectionProvider = ConnectionProvider.builder("connectionProvider3")
            .maxIdleTime(Duration.ofSeconds(30))
            .maxConnections(5)
            .pendingAcquireMaxCount(-1)
            .build()
        val httpClient = HttpClient.create(connectionProvider) // 国内访问即可，无需设置梯子
            .followRedirect(true).responseTimeout(Duration.ofSeconds(30))
        val connector = ReactorClientHttpConnector(httpClient)
        val strategies = ExchangeStrategies.builder().codecs {
            it.defaultCodecs().jackson2JsonEncoder(
                Jackson2JsonEncoder(
                    JacksonUtil.mapper, MediaType.APPLICATION_JSON
                )
            )
        }.build()

        return builder.clientConnector(connector).exchangeStrategies(strategies)
            .defaultHeaders { headers: HttpHeaders ->
                headers.contentType = MediaType.APPLICATION_JSON
                headers.accept = listOf(MediaType.APPLICATION_JSON)
            }.baseUrl("http://api.bilibili.com/")
            .codecs { it.defaultCodecs().maxInMemorySize(Int.MAX_VALUE) }
            .filter { request: ClientRequest, next: ExchangeFunction -> this.doRetryFilter(request, next)
            }.build()
    }

    private fun doRetryFilter(request: ClientRequest, next: ExchangeFunction): Mono<ClientResponse?> {
        return next.exchange(request)
            .flatMap<ClientResponse?>(Function<ClientResponse, Mono<out ClientResponse?>> { response: ClientResponse ->
                when (response.statusCode().value()) {
                    504, 503, 502, 429, 408 -> response.createException().flatMap<ClientResponse>(
                        Function<WebClientResponseException, Mono<out ClientResponse>> { error: WebClientResponseException? ->
                            Mono.error(
                                error!!
                            )
                        })

                    else -> Mono.just<ClientResponse>(response)
                }
            }).retryWhen(
                Retry.backoff(3, Duration.ofSeconds(2)).jitter(0.1)
                    .doBeforeRetry { a: RetrySignal? -> log.warn("Retrying request {}", request.url()) })
            .onErrorResume(
                RuntimeException::class.java, { e: RuntimeException ->
                    if (Exceptions.isRetryExhausted(e)) {
                        return@onErrorResume Mono.error<ClientResponse>(e.cause!!)
                    }
                    Mono.error(e)
                })
    }

    @Bean("proxyClient") @Qualifier("proxyClient") fun proxyClient(builder: WebClient.Builder, config: NowbotConfig): WebClient {
        val httpClient = HttpClient.newConnection().proxy(Consumer { proxy: ProxyProvider.TypeSpec ->
                proxy.type(
                    if ("HTTP".equals(
                            config.proxyType, ignoreCase = true
                        )) ProxyProvider.Proxy.HTTP else ProxyProvider.Proxy.SOCKS5
                ).host(config.proxyHost).port(config.proxyPort)
            }).responseTimeout(Duration.ofSeconds(30))
        return builder.clientConnector(ReactorClientHttpConnector(httpClient)).build()
    }

    @Bean("webClient") @Qualifier("webClient") fun webClient(builder: WebClient.Builder): WebClient {
        val connectionProvider = ConnectionProvider.builder("connectionProvider3")
            .maxIdleTime(Duration.ofSeconds(30))
            .maxConnections(200)
            .pendingAcquireMaxCount(-1)
            .build()
        val httpClient = HttpClient.create(connectionProvider).responseTimeout(Duration.ofSeconds(30))
        val connector = ReactorClientHttpConnector(httpClient)
        val strategies = ExchangeStrategies.builder().codecs {
            it.defaultCodecs().jackson2JsonEncoder(
                Jackson2JsonEncoder(JacksonUtil.mapper, MediaType.APPLICATION_JSON)
            )
            }.build()
        return builder.clientConnector(connector).exchangeStrategies(strategies)
            .codecs { it.defaultCodecs().maxInMemorySize(Int.MAX_VALUE) } .build()
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(WebClientConfig::class.java)
    }
}
