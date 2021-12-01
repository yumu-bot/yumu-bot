package com.now.nowbot.config;

import com.now.nowbot.listener.MessageListener;
import com.now.nowbot.util.PanelUtil;
import net.mamoe.mirai.Bot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class IocAllReadyRunner implements CommandLineRunner {
    Logger log = LoggerFactory.getLogger("IocAllReadyRunner");
    Bot bot;
    @Autowired
    public IocAllReadyRunner(Bot bot, MessageListener messageListener, ApplicationContext applicationContext){
        this.bot = bot;
        messageListener.init(applicationContext);
    }
    @Override
    public void run(String... args) throws Exception {
        PanelUtil.init();
        log.info("启动成功");
        if (NowbotConfig.QQ_LOGIN) {
            if (bot != null && bot.getGroup(746671531L) != null) {
                bot.getGroup(746671531L).sendMessage("启动完成");
            }
        }
    }
}
