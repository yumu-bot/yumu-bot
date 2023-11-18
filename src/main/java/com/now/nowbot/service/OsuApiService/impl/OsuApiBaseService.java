package com.now.nowbot.service.OsuApiService.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.now.nowbot.config.OSUConfig;
import com.now.nowbot.dao.BindDao;
import com.now.nowbot.model.BinUser;
import com.now.nowbot.throwable.ServiceException.BindException;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

@Service
public class OsuApiBaseService {
    private static final Logger log = LoggerFactory.getLogger(OsuApiBaseService.class);
    private static String accessToken = null;
    private static long time = System.currentTimeMillis();
    protected final int oauthId;
    protected final String redirectUrl;
    protected final String oauthToken;

    @Lazy
    @Resource
    BindDao bindDao;

    @Resource
    WebClient osuApiWebClient;
    @Resource
    WebClient webClient;

    public OsuApiBaseService(OSUConfig osuConfig) {
        oauthId = osuConfig.getId();
        redirectUrl = osuConfig.getCallBackUrl();
        oauthToken = osuConfig.getToken();
    }

    private boolean isPassed() {
        return System.currentTimeMillis() > time;
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

        var s = osuApiWebClient.post()
                .uri("https://osu.ppy.sh/oauth/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        if (s != null) {
            accessToken = s.get("access_token").asText();
            time = System.currentTimeMillis() + s.get("expires_in").asLong() * 1000;
        } else {
            throw new RuntimeException("更新 Oauth 令牌 请求失败");
        }
        return accessToken;
    }

    String refreshUserToken(BinUser user, boolean first) {
        Map<String, String> body = Map.of(
                "client_id", String.valueOf(oauthId),
                "client_secret", oauthToken,
                "grant_type", first ? "authorization_code" : "refresh_token",
                "refresh_token", user.getRefreshToken(),
                "redirect_uri", redirectUrl
        );
        JsonNode s = null;
        s = osuApiWebClient.post()
                .uri("https://osu.ppy.sh/oauth/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
        String accessToken;
        String refreshToken;
        long time;
        if (s != null) {
            accessToken = s.get("access_token").asText();
            user.setAccessToken(accessToken);
            refreshToken = s.get("refresh_token").asText();
            user.setRefreshToken(refreshToken);
            time = user.nextTime(s.get("expires_in").asLong());
        } else {
            throw new RuntimeException("更新 Oauth 令牌 请求失败");
        }
        if (!first) {
            // 第一次更新需要在外面更新去更新数据库
            bindDao.updateToken(user.getOsuID(), accessToken, refreshToken, time);
        }
        return accessToken;
    }

    void insertHeader(HttpHeaders headers) {
        headers.set("Authorization", "Bearer " + getBotToken());
    }

    Consumer<HttpHeaders> insertHeader(BinUser user) {
        final String token;
        if (Objects.isNull(user.getAccessToken())) {
            token = getBotToken();
        } else if (user.isPassed()) {
            try {
                token = refreshUserToken(user, false);
            } catch (WebClientResponseException.Unauthorized e) {
                log.error("令牌过期 绑定丢失: [{}], 移除绑定信息", user.getOsuID(), e);
                throw new BindException(BindException.Type.BIND_Me_TokenExpired);
            } catch (WebClientResponseException.NotFound e) {
                log.info("更新令牌失败：账号封禁", e);
                throw new BindException(BindException.Type.BIND_Me_Banned);
            } catch (WebClientResponseException.TooManyRequests e) {
                log.info("更新令牌失败：API 访问太频繁", e);
                throw new BindException(BindException.Type.BIND_Me_TooManyRequests);
            }
        } else {
            token = user.getAccessToken();
        }
        return (headers) -> headers.set("Authorization", "Bearer " + token);
    }
}