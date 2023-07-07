package com.now.nowbot.config;


import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.annotation.Order;

@Configuration
@Order(6)
@ComponentScan("com.mikuac.shiro")
public class OneBotConfig {
}
