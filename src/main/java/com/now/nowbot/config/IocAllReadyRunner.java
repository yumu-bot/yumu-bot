package com.now.nowbot.config;

import com.now.nowbot.aop.CheckAspect;
import com.now.nowbot.dao.QQMessageDao;
import com.now.nowbot.listener.MessageListener;
import com.now.nowbot.service.MessageService.MessageService;
import com.now.nowbot.util.MoliUtil;
import com.now.nowbot.util.PanelUtil;
import com.now.nowbot.util.QQMsgUtil;
import net.mamoe.mirai.Bot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class IocAllReadyRunner implements CommandLineRunner {
    Logger log = LoggerFactory.getLogger("IocAllReadyRunner");
    Bot bot;
    ApplicationContext applicationContext;
    CheckAspect check;
    Permission permission;

    @Autowired
    public IocAllReadyRunner(Bot bot, MessageListener messageListener, ApplicationContext applicationContext, CheckAspect check, Permission permission){
        this.bot = bot;
        this.applicationContext = applicationContext;
        messageListener.init(applicationContext.getBeansOfType(MessageService.class));
        this.check = check;
        this.permission = permission;
    }
    @Override
    /**
     * ioc容器加载完毕运行
     */
    public void run(String... args) throws Exception {
        PanelUtil.init();
        QQMsgUtil.init(applicationContext.getBean(QQMessageDao.class));
        MoliUtil.init(applicationContext.getBean(RestTemplate.class));
        permission.init(applicationContext);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> { //jvm结束钩子
            check.doEnd();
            if (bot != null && bot.getGroup(746671531L) != null) {
                bot.getGroup(746671531L).sendMessage("程序关闭");
            }
        }, "endThread"));
        log.info("启动成功");
        if (NowbotConfig.QQ_LOGIN) {
            //登录
            bot.login();
            if (bot != null && bot.getGroup(746671531L) != null) {
                bot.getGroup(746671531L).sendMessage("启动完成");
            }
        }

    }
}
