package com.now.nowbot.service.OsuApiService.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.now.nowbot.config.OSUConfig;
import com.now.nowbot.config.YumuConfig;
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
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.function.Consumer;

@Service
public class OsuApiBaseService {
    private static final Logger log         = LoggerFactory.getLogger(OsuApiBaseService.class);
    private static       String accessToken = null;
    private static       long   time        = System.currentTimeMillis();
    protected final      int    oauthId;
    protected final      String redirectUrl;
    protected final      String oauthToken;

    @Lazy
    @Resource
    BindDao bindDao;

    @Resource
    WebClient osuApiWebClient;
    @Resource
    WebClient webClient;

    public OsuApiBaseService(OSUConfig osuConfig, YumuConfig yumuConfig) {
        String url;
        oauthId = osuConfig.getId();
        if (! StringUtils.hasText(url = osuConfig.getCallbackUrl())) {
            url = STR."\{yumuConfig.getPublicUrl()}\{osuConfig.getCallbackPath()}";
        }
        redirectUrl = url;
        oauthToken = osuConfig.getToken();
    }

    private boolean isPassed() {
        return System.currentTimeMillis() > time;
    }

    protected String getBotToken() {
        if (! isPassed()) {
            return accessToken;
        }
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", String.valueOf(oauthId));
        body.add("client_secret", oauthToken);
        body.add("grant_type", "client_credentials");
        body.add("scope", "public");

        var s = osuApiWebClient.post()
                .uri("https://osu.ppy.sh/oauth/token")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(body)).retrieve().bodyToMono(JsonNode.class).block();

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
        JsonNode s = osuApiWebClient.post()
                .uri("https://osu.ppy.sh/oauth/token")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(body))
                .retrieve().bodyToMono(JsonNode.class).block();
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
        if (! first) {
            // 第一次更新需要在外面更新去更新数据库
            bindDao.updateToken(user.getOsuID(), accessToken, refreshToken, time);
        }
        return accessToken;
    }

    void insertHeader(HttpHeaders headers) {
        headers.set("Authorization", STR."Bearer \{getBotToken()}");
    }

    Consumer<HttpHeaders> insertHeader(BinUser user) {
        final String token;
        if (! user.isAuthorized()) {
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
        return (headers) -> headers.set("Authorization", STR."Bearer \{token}");
    }
}
