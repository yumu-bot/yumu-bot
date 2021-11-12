package com.now.nowbot;

import com.now.nowbot.config.NowbotConfig;
import net.mamoe.mirai.Bot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
    private static Logger log = LoggerFactory.getLogger(NowbotApplication.class);
    static Bot bot;
    @Autowired
    public void setbot(Bot bot){ NowbotApplication.bot = bot; }
    public static void main(String[] args) {
        SpringApplication.run(NowbotApplication.class, args);
        log.info("启动成功");
        if (NowbotConfig.QQ_LOGIN) {
            bot.getGroup(746671531L).sendMessage("启动完成");
        }
    }
}
