package com.now.nowbot;

import com.now.nowbot.config.NowbotConfig;
import com.now.nowbot.entity.FontCfg;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@SpringBootApplication
@EnableConfigurationProperties(NowbotConfig.class)
@EnableAspectJAutoProxy(exposeProxy = true, proxyTargetClass = true)
@EnableScheduling
public class NowbotApplication {
    public static void main(String[] args) {
        SpringApplication.run(NowbotApplication.class, args);
        System.out.println("Max:"+Runtime.getRuntime().maxMemory()/1024/1024+'M');
        System.out.println("Total:"+Runtime.getRuntime().totalMemory()/1024/1024+'M');
        FontCfg.init();
    }

}
