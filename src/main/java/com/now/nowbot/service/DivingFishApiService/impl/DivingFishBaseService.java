package com.now.nowbot.service.DivingFishApiService.impl;

import com.now.nowbot.config.DivingFishConfig;
import com.now.nowbot.dao.BindDao;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class DivingFishBaseService {
    private static final Logger log = LoggerFactory.getLogger(DivingFishBaseService.class);
    private static       String accessToken = null;
    private static       long   time        = System.currentTimeMillis();

    @Lazy
    @Resource
    BindDao bindDao;

    @Resource
    WebClient divingFishApiWebClient;

    // 这里写 token 相关的
    public DivingFishBaseService(DivingFishConfig fishConfig) {

    }
}
