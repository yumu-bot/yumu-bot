package com.now.nowbot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "yumu.maimai", ignoreInvalidFields = true)
public class DivingFishConfig {
    /**
     * 接口路径, 一般不用改
     */
    public static final String url = "https://www.diving-fish.com/";
    public static final String api = "https://www.diving-fish.com/api/maimaidxprober/";

    public String getUrl() {
        return url;
    }

    public String getApi() {
        return api;
    }
}
