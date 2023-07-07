package com.now.nowbot;


import com.now.nowbot.config.OneBotConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableRetry
@EnableScheduling
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableAspectJAutoProxy(exposeProxy = true, proxyTargetClass = true)
@Import(OneBotConfig.class)
public class NowbotApplication {
    public static Logger log = LoggerFactory.getLogger(NowbotApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(NowbotApplication.class, args);
    }
}
