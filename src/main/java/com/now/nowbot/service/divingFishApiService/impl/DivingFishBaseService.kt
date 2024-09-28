package com.now.nowbot.service.divingFishApiService.impl;

import com.now.nowbot.config.DivingFishConfig;
import com.now.nowbot.dao.BindDao;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class DivingFishBaseService {
    private static final Logger log         = LoggerFactory.getLogger(DivingFishBaseService.class);
    private static String accessToken = null;

    @Lazy
    @Resource
    BindDao bindDao;

    @Resource
    WebClient divingFishApiWebClient;

    // 这里写 token 相关的
    public DivingFishBaseService(DivingFishConfig fishConfig) {
        if (StringUtils.hasText(fishConfig.getToken())) {
            accessToken = fishConfig.getToken();
        }
    }

    public boolean hasToken() {
        return StringUtils.hasText(accessToken);
    }

    void insertDeveloperHeader(HttpHeaders headers) {
        headers.set("Developer-Token", accessToken);
    }

    void insertJSONHeader(HttpHeaders headers) {
        headers.set("Content-Type", "application/json");
    }

    void insertDefaultHeader(HttpHeaders headers) {
        headers.set("Content-Type", "application/raw");
    }
}
