package com.now.nowbot;

import com.now.nowbot.config.NowbotConfig;
import com.now.nowbot.config.Permission;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties(NowbotConfig.class)
@EnableAspectJAutoProxy(exposeProxy = true, proxyTargetClass = true)
@EnableScheduling
public class NowbotApplication {
    public static void main(String[] args) {
        SpringApplication.run(NowbotApplication.class, args);
    }
}
