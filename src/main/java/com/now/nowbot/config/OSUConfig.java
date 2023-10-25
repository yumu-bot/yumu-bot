package com.now.nowbot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Primary;
import org.springframework.validation.annotation.Validated;

@Primary
@Validated
@ConfigurationProperties(prefix = "osu", ignoreInvalidFields = true)
public class OSUConfig {
    /**
     * 接口路径, 一般不用改
     */
    String url = "https://osu.ppy.sh/api/v2/";
    /**
     * 回调的api端口
     */
    String callBackUrl = "/bind";
    /**
     * 回调链接, ${osu.callbackpath} 用callBackUrl 替换
     */
    String callbackpath = "http://localhost:8388${osu.callbackpath}";
    Integer id = 0;
    String token = "*";

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
