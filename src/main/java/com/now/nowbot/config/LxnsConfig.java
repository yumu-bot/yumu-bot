package com.now.nowbot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "yumu.lxns", ignoreInvalidFields = true)
public class LxnsConfig {
    /**
     * 接口路径, 一般不用改
     */
    public static final String url   = "https://assets2.lxns.net/";
    public static final String token = "BMurCyOaA0cfist6VpNvb7ZXK5h1noSE";

    public String getUrl() {
        return url;
    }

    public String getToken() {
        return token;
    }
}
