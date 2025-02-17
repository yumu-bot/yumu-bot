package com.now.nowbot.config;

import com.now.nowbot.util.JacksonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.function.client.*;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import reactor.netty.transport.ProxyProvider;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Collections;

@Component
@Configuration
public class WebClientConfig implements WebFluxConfigurer {
    private static final Logger log = LoggerFactory.getLogger(WebClientConfig.class);

    public void configureHttpMessageCodecs(ServerCodecConfigurer configurer) {
        configurer.defaultCodecs().maxInMemorySize(20 * 1024 * 1024);
    }

    @Bean("divingFishApiWebClient")
    public WebClient DivingFishApiWebClient(WebClient.Builder builder) {
        ConnectionProvider connectionProvider = ConnectionProvider.builder("connectionProvider")
                .maxIdleTime(Duration.ofSeconds(30))
                .build();
        HttpClient httpClient = HttpClient.create(connectionProvider) // 国内访问即可，无需设置梯子
                .followRedirect(true)
                .responseTimeout(Duration.ofSeconds(30));
        ReactorClientHttpConnector connector = new ReactorClientHttpConnector(httpClient);
        ExchangeStrategies strategies = ExchangeStrategies
                .builder()
                .codecs(clientDefaultCodecsConfigurer -> {
                    clientDefaultCodecsConfigurer.defaultCodecs().jackson2JsonEncoder(new Jackson2JsonEncoder(JacksonUtil.mapper, MediaType.APPLICATION_JSON));
                    clientDefaultCodecsConfigurer.defaultCodecs().jackson2JsonDecoder(new Jackson2JsonDecoder(JacksonUtil.mapper, MediaType.APPLICATION_JSON));
                }).build();

        return builder
                .clientConnector(connector)
                .exchangeStrategies(strategies)
                .defaultHeaders((headers) -> {
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
                })
                .baseUrl(DivingFishConfig.url)
                .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(Integer.MAX_VALUE))
                .filter(this::doRetryFilter)
                .build();
    }

    @Bean("osuApiWebClient")
    @Primary
    public WebClient OsuApiWebClient(WebClient.Builder builder) {
        /*
         * Setting maxIdleTime as 30s, because servers usually have a keepAliveTimeout of 60s, after which the connection gets closed.
         * If the connection pool has any connection which has been idle for over 10s, it will be evicted from the pool.
         * Refer https://github.com/reactor/reactor-netty/issues/1318#issuecomment-702668918
         */
        ConnectionProvider connectionProvider = ConnectionProvider.builder("connectionProvider")
                .maxIdleTime(Duration.ofSeconds(30))
                .build();
        HttpClient httpClient = HttpClient.create(connectionProvider)
//                .proxy(proxy ->
//                        proxy.type("HTTP".equalsIgnoreCase(config.proxyType) ? ProxyProvider.Proxy.HTTP : ProxyProvider.Proxy.SOCKS5)
//                                .host(config.proxyHost)
//                                .port(config.proxyPort)
//                )
                .followRedirect(true)
                .responseTimeout(Duration.ofSeconds(15));
        ReactorClientHttpConnector connector = new ReactorClientHttpConnector(httpClient);
        ExchangeStrategies strategies = ExchangeStrategies
                .builder()
                .codecs(clientDefaultCodecsConfigurer -> {
                    clientDefaultCodecsConfigurer.defaultCodecs().jackson2JsonEncoder(new Jackson2JsonEncoder(JacksonUtil.mapper, MediaType.APPLICATION_JSON));
                    clientDefaultCodecsConfigurer.defaultCodecs().jackson2JsonDecoder(new Jackson2JsonDecoder(JacksonUtil.mapper, MediaType.APPLICATION_JSON));
                }).build();

        return builder
                .clientConnector(connector)
                .exchangeStrategies(strategies)
                .defaultHeaders((headers) -> {
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
                })
                .baseUrl("https://osu.ppy.sh/api/v2/")
                .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(Integer.MAX_VALUE))
                .build();
    }

    private Mono<ClientResponse> doRetryFilter(ClientRequest request, ExchangeFunction next) {
        return next
                .exchange(request)
                .flatMap(response -> switch (response.statusCode().value()) {
                    case 504, 503, 502, 429, 408 -> response.createException().flatMap(Mono::error);
                    default -> Mono.just(response);
                })
                .retryWhen(Retry
                        .backoff(3, Duration.ofSeconds(2))
                        .jitter(0.1)
                        .doBeforeRetry(a -> log.warn("Retrying request {}", request.url()))
                )
                .onErrorResume(RuntimeException.class, e -> {
                    if (Exceptions.isRetryExhausted(e)) {
                        return Mono.error(e.getCause());
                    }
                    return Mono.error(e);
                });
    }

    @Bean("proxyClient")
    public WebClient proxyClient(WebClient.Builder builder, NowbotConfig config) {
        HttpClient httpClient = HttpClient.newConnection()
                .proxy(proxy ->
                        proxy.type("HTTP".equalsIgnoreCase(config.proxyType) ? ProxyProvider.Proxy.HTTP : ProxyProvider.Proxy.SOCKS5)
                                .host(config.proxyHost)
                                .port(config.proxyPort)
                )
                .responseTimeout(Duration.ofSeconds(30));
        return builder
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    @Bean("webClient")
    public WebClient webClient(WebClient.Builder builder) {
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofSeconds(30));
        ReactorClientHttpConnector connector = new ReactorClientHttpConnector(httpClient);
        ExchangeStrategies strategies = ExchangeStrategies
                .builder()
                .codecs(clientDefaultCodecsConfigurer -> {
                    clientDefaultCodecsConfigurer.defaultCodecs().jackson2JsonEncoder(new Jackson2JsonEncoder(JacksonUtil.mapper, MediaType.APPLICATION_JSON));
                    clientDefaultCodecsConfigurer.defaultCodecs().jackson2JsonDecoder(new Jackson2JsonDecoder(JacksonUtil.mapper, MediaType.APPLICATION_JSON));
                }).build();
        return builder
                .clientConnector(connector)
                .exchangeStrategies(strategies)
                .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(Integer.MAX_VALUE))
                .build();
    }
}
