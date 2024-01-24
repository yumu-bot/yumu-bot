package com.now.nowbot.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Primary;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Primary
@ConfigurationProperties(prefix = "yumu")
public class YumuConfig {
    /**
     * 访问是否需要加端口, 默认不加, 如果对外访问需要则端口则填写
     */
    Integer publicPort = 0;

    /**
     * 访问内网的端口, 默认与server端口保持一致
     */
    @Value("${server.port}")
    Integer privatePort;

    /**
     * 公网可以访问到的路径
     */
    String publicDomain  = "";
    /**
     * 内网设备可以访问到的路径
     */
    String privateDomain = "http://localhost";

    /**
     * 私域设备的qq号
     */

    List<Long> privateDevice = new ArrayList<>(0);


    public String getPrivateDomain() {
        return privateDomain;
    }

    public void setPrivateDomain(String privateDomain) {
        this.privateDomain = privateDomain;
    }

    public List<Long> getPrivateDevice() {
        return privateDevice;
    }

    public void setPrivateDevice(List<Long> privateDevice) {
        this.privateDevice = privateDevice;
    }

    public String getPublicUrl() {
        return STR."\{publicDomain}\{
                publicPort == 0 ? "" : STR.":\{publicPort}"}";
    }

    private String getRowDomain(String s) {
        int n;
        if ((n = s.indexOf(':', 7)) >= 0) {
            return s.substring(0, n);
        }
        return s;
    }

    public String getPublicDomain() {
        return publicDomain;
    }

    public Integer getPublicPort() {
        return publicPort;
    }

    public void setPublicPort(Integer publicPort) {
        this.publicPort = publicPort;
    }

    public void setPublicDomain(String publicDomain) {
        this.publicDomain = publicDomain;
    }

    public String getPrivateUrl() {
        return STR."\{privateDomain}\{
                privatePort == 0 ? "" : STR.":\{privatePort}"}";
    }

    public Integer getPrivatePort() {
        return privatePort;
    }

    public void setPrivatePort(Integer privatePort) {
        this.privatePort = privatePort;
    }

    @PostConstruct
    public void init() {
        publicDomain = getRowDomain(publicDomain);
        privateDomain = getRowDomain(privateDomain);
        if (! StringUtils.hasText(publicDomain)) publicDomain = privateDomain;
        if (publicDomain.equals(privateDomain) && publicPort == 0) publicPort = privatePort;
    }
}
