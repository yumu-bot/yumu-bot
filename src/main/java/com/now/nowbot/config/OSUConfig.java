package com.now.nowbot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Primary;
import org.springframework.validation.annotation.Validated;

@Primary
@Validated
@ConfigurationProperties(prefix = "yumu.osu", ignoreInvalidFields = true)
public class OSUConfig {
    /**
     * 接口路径, 一般不用改
     */
    String url = "https://osu.ppy.sh/api/v2/";
    /**
     * 回调链接, 需要与 osu oauth 应用的callback url 完全一致
     * 默认不需要配置, 自动构造 publicDomain+callBackUrl
     * 也可以自行配置, 强制覆盖
     */
    String callbackUrl = "";
    /**
     * 回调的api端口
     */
    String callbackPath = "/bind";
    Integer id = 0;
    String token = "*";

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getCallbackPath() {
        return callbackPath;
    }

    public void setCallbackPath(String callbackPath) {
        this.callbackPath = callbackPath;
    }

    public String getCallbackUrl() {
        return callbackUrl;
    }

    public void setCallbackUrl(String callbackUrl) {
        this.callbackUrl = callbackUrl;
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
