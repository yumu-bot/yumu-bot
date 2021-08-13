package com.now.nowbot;

import com.now.nowbot.config.NowbotConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(NowbotConfig.class)
//@EnableScheduling
public class NowbotApplication {
    public static void main(String[] args) {
        SpringApplication.run(NowbotApplication.class, args);

    }
//    @Scheduled(cron = "0/5 * * * * *") 定时任务
//    public void A(){
//        System.out.println("123");
//    }
}
