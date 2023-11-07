package com.now.nowbot.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Primary;
import org.springframework.validation.annotation.Validated;

@Primary
@Validated
@ConfigurationProperties(prefix = "discord", ignoreInvalidFields = true)
public class DiscordConfig {
    private static final Logger log = LoggerFactory.getLogger(DiscordConfig.class);

    /**
     * dc 申请的token
     */
    private String token = "*";
    /**
     * 分割命令, 不需要改
     */
    private String commandSuffix = "ym-";

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getCommandSuffix() {
        return commandSuffix;
    }

    public void setCommandSuffix(String commandSuffix) {
        this.commandSuffix = commandSuffix;
    }
}
