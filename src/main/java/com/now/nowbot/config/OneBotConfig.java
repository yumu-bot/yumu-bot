package com.now.nowbot.config;


import com.mikuac.shiro.core.BotContainer;
import com.mikuac.shiro.core.BotFactory;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.annotation.Order;

/**
 * 用于延迟加载shiro框架, 非必要勿动
 */
@Configuration
@Order(6)
@ComponentScan("com.mikuac.shiro")
public class OneBotConfig {
    static BotContainer BOT_CONTAINER;

    @Lazy
    @Autowired
    public void setBotFactory(BotContainer factory) {
        BOT_CONTAINER = factory;
    }

    public static BotContainer getBotContainer() {
        return BOT_CONTAINER;
    }
}
