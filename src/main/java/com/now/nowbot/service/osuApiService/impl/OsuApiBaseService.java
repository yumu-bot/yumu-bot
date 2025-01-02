package com.now.nowbot.service.osuApiService.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.RateLimiter;
import com.now.nowbot.config.OSUConfig;
import com.now.nowbot.config.YumuConfig;
import com.now.nowbot.dao.BindDao;
import com.now.nowbot.model.BinUser;
import com.now.nowbot.throwable.serviceException.BindException;
import com.now.nowbot.util.ContextUtil;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.now.nowbot.config.IocAllReadyRunner.APP_ALIVE;

@Service
public class OsuApiBaseService {
    private static final Logger log         = LoggerFactory.getLogger(OsuApiBaseService.class);
    private static       String accessToken = null;
    private static       long   time        = System.currentTimeMillis();
    protected final      int    oauthId;
    protected final      String redirectUrl;
    protected final      String oauthToken;

    private static final String      THREAD_LOCAL_ENVIRONMENT = "osu-api-priority";
    private static final int         DEFAULT_PRIORITY         = 5;
    private static final int         MAX_RETRY                = 3;
    private static final RateLimiter limiter                  = RateLimiter.create(10, Duration.ofSeconds(1));

    private static final PriorityBlockingQueue<RequestTask<?>> TASKS = new PriorityBlockingQueue<>();

    @Lazy
    @Resource
    BindDao bindDao;

    @Resource
    WebClient osuApiWebClient;

    public OsuApiBaseService(OSUConfig osuConfig, YumuConfig yumuConfig) {
        String url;
        oauthId = osuConfig.getId();
        if (!StringUtils.hasText(url = osuConfig.getCallbackUrl())) {
            url = STR."\{yumuConfig.getPublicUrl()}\{osuConfig.getCallbackPath()}";
        }
        redirectUrl = url;
        oauthToken = osuConfig.getToken();
    }

    /**
     * 借助线程变量设置后续请求的优先级, 如果使用线程池, 务必在请求结束后调用 {@link #clearPriority()} 方法
     *
     * @param priority 默认为 5, 越低越优先, 相同优先级先来后到
     */
    public static void setPriority(int priority) {
        ContextUtil.setContext(THREAD_LOCAL_ENVIRONMENT, priority);
    }

    private boolean isPassed() {
        return System.currentTimeMillis() > time;
    }

    public static void clearPriority() {
        ContextUtil.setContext(THREAD_LOCAL_ENVIRONMENT, null);
    }

    @PostConstruct
    public void init() {
        Thread.startVirtualThread(this::runTask);
    }

    protected String getBotToken() {
        if (!isPassed()) {
            return accessToken;
        }
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", String.valueOf(oauthId));
        body.add("client_secret", oauthToken);
        body.add("grant_type", "client_credentials");
        body.add("scope", "public");

        setPriority(0);
        var s = request(client -> client
                .post()
                .uri("https://osu.ppy.sh/oauth/token")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(body)).retrieve().bodyToMono(JsonNode.class)
        );
        clearPriority();

        if (s != null) {
            accessToken = s.get("access_token").asText();
            time = System.currentTimeMillis() + s.get("expires_in").asLong() * 1000;
        } else {
            throw new RuntimeException("更新 Oauth 令牌 请求失败");
        }
        return accessToken;
    }

    String refreshUserToken(BinUser user, boolean first) {
        LinkedMultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", String.valueOf(oauthId));
        body.add("client_secret", oauthToken);
        body.add("redirect_uri", redirectUrl);
        body.add("grant_type", first ? "authorization_code" : "refresh_token");
        body.add(first ? "code" : "refresh_token", user.getRefreshToken());
        setPriority(1);
        JsonNode s = request((client) -> client
                .post()
                .uri("https://osu.ppy.sh/oauth/token")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(body))
                .retrieve().bodyToMono(JsonNode.class)
        );
        clearPriority();
        String accessToken;
        String refreshToken;
        long time;
        if (s != null) {
            accessToken = s.get("access_token").asText();
            user.setAccessToken(accessToken);
            refreshToken = s.get("refresh_token").asText();
            user.setRefreshToken(refreshToken);
            time = user.setTimeToAfter(s.get("expires_in").asLong() * 1000);
        } else {
            throw new RuntimeException("更新 Oauth 令牌, 接口格式错误");
        }
        if (!first) {
            // 第一次更新需要在外面更新去更新数据库
            bindDao.updateToken(user.getOsuID(), accessToken, refreshToken, time);
        }
        return accessToken;
    }

    void insertHeader(HttpHeaders headers) {
        headers.setAll(
                Map.of("Authorization", "Bearer " + getBotToken(),
                        "x-api-version", "20240529"
                )
        );
    }

    Consumer<HttpHeaders> insertHeader(BinUser user) {
        final String token;
        if (!user.isAuthorized()) {
            token = getBotToken();
        } else if (user.isPassed()) {
            try {
                token = refreshUserToken(user, false);
            } catch (HttpClientErrorException.Unauthorized | WebClientResponseException.Unauthorized e) {
                bindDao.backupBind(user.getOsuID());
                log.info("令牌过期 绑定丢失: [{}], 已退回到 id 绑定", user.getOsuID(), e);
                throw new BindException(BindException.Type.BIND_Me_TokenExpiredButBindID);
            } catch (HttpClientErrorException.Forbidden | WebClientResponseException.Forbidden e) {
                log.info("更新令牌失败：账号封禁", e);
                throw new BindException(BindException.Type.BIND_Me_Banned);
            } catch (HttpClientErrorException.NotFound | WebClientResponseException.NotFound e) {
                log.info("更新令牌失败：找不到账号", e);
                throw new BindException(BindException.Type.BIND_Player_NotFound);
            } catch (HttpClientErrorException.TooManyRequests | WebClientResponseException.TooManyRequests e) {
                log.info("更新令牌失败：API 访问太频繁", e);
                throw new BindException(BindException.Type.BIND_API_TooManyRequests);
            }
        } else {
            token = user.getAccessToken();
        }

        return (headers) -> headers.setAll(
                Map.of("Authorization", "Bearer " + token,
                        "x-api-version", "20241101"
                )
        );
    }

    public <T> T request(Function<WebClient, Mono<T>> request) {
        var future = new CompletableFuture<T>();
        int priority = ContextUtil.getContext(THREAD_LOCAL_ENVIRONMENT, DEFAULT_PRIORITY, Integer.class);
        var task = new RequestTask<>(priority, future, request);
        TASKS.offer(task);
        try {
            return future.get();
        } catch (ExecutionException e) {
            if (e.getCause() instanceof RuntimeException r) {
                throw r;
            }
            throw new RuntimeException(e.getCause());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void runTask() {
        while (APP_ALIVE) {
            try {
                limiter.acquire();
                var task = TASKS.take();
                Thread.startVirtualThread(() -> task.run(osuApiWebClient));
            } catch (InterruptedException e) {
                log.error("请求队列异常", e);
            }
        }
    }

    static class RequestTask<T> implements Comparable<RequestTask<?>> {
        int                          priority;
        int                          time;
        short                        retry = 0;
        CompletableFuture<T>         future;
        Function<WebClient, Mono<T>> request;

        RequestTask(int priority, CompletableFuture<T> future, Function<WebClient, Mono<T>> request) {
            this.priority = priority;
            this.future = future;
            this.request = request;
            this.time = getNowTime();
        }

        private static int getNowTime() {
            // seconds since 2025-01-01
            return (int) (System.currentTimeMillis() / 1000 - 1735660800);
        }

        public void run(WebClient client) {
            request.apply(client).subscribe(future::complete, this::onError);
        }

        private void onError(Throwable e) {
            if (retry >= MAX_RETRY) {
                future.completeExceptionally(e);
            }

            if (e instanceof WebClientResponseException.TooManyRequests ||
                    e instanceof WebClientRequestException
            ) {
                retry++;
                TASKS.add(this);
            } else {
                future.completeExceptionally(e);
            }
        }

        @Override
        public int compareTo(@NotNull RequestTask<?> o) {
            return getPriority() - o.getPriority();
        }

        private int getPriority() {
            // 用于对比, 优先级 * (一个大数 n + 时间), 这个数字大到不可能存在两个请求的时间超过 n 秒
            return (3600 * priority) + time;
        }
    }
}
