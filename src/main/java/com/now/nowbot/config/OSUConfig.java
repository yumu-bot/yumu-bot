package com.now.nowbot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Primary;
import org.springframework.validation.annotation.Validated;

@Primary
@Validated
@ConfigurationProperties(prefix = "osu", ignoreInvalidFields = true)
public class OSUConfig {
    String url;
    String callbackpath;
    String callBackUrl;
    Integer id;
    String token;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getCallbackpath() {
        return callbackpath;
    }

    public void setCallbackpath(String callbackpath) {
        this.callbackpath = callbackpath;
    }

    public String getCallBackUrl() {
        return callBackUrl;
    }

    public void setCallBackUrl(String callBackUrl) {
        this.callBackUrl = callBackUrl;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
