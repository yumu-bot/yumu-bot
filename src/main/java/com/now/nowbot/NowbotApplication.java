package com.now.nowbot;

import com.now.nowbot.config.NowbotConfig;
import com.now.nowbot.config.Permission;
import net.mamoe.mirai.utils.MiraiLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.annotation.PostConstruct;

@SpringBootApplication
@EnableConfigurationProperties(NowbotConfig.class)
@EnableAspectJAutoProxy(exposeProxy = true, proxyTargetClass = true)
@EnableScheduling
public class NowbotApplication {
    private static Logger log = LoggerFactory.getLogger(NowbotApplication.class);
    public static void main(String[] args) {
        SpringApplication.run(NowbotApplication.class, args);
        log.info("启动成功");
    }
}
