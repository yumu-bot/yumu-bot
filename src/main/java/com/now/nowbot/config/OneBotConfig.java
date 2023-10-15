package com.now.nowbot.config;


import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.annotation.Order;

/**
 * 用于延迟加载shiro框架, 非必要勿动
 */
@Configuration
@Order(6)
@ComponentScan("com.mikuac.shiro")
public class OneBotConfig {
}
