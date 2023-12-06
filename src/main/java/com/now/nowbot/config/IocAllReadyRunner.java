package com.now.nowbot.config;

import com.now.nowbot.aop.CheckAspect;
import com.now.nowbot.dao.QQMessageDao;
import com.now.nowbot.listener.OneBotListener;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.MessageServiceImpl.MatchListenerService;
import com.now.nowbot.util.MoliUtil;
import com.now.nowbot.util.PanelUtil;
import com.now.nowbot.util.QQMsgUtil;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.boot.web.embedded.tomcat.TomcatWebServer;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.Executor;

@Component
public class IocAllReadyRunner implements CommandLineRunner {
    Logger log = LoggerFactory.getLogger("IocAllReadyRunner");
    ApplicationContext applicationContext;
    CheckAspect check;
    Permission permission;
    @Resource
    WebServerApplicationContext webServerApplicationContext;
    @Resource(name = "mainExecutor")
    Executor executor;

    @Autowired
    public IocAllReadyRunner(OneBotListener oneBotListener, ApplicationContext applicationContext, CheckAspect check, Permission permission){
        this.applicationContext = applicationContext;

//        serviceMap.putAll(applicationContext
//                .getBeansOfType(MessageService.class)
//                .values()
//                .stream()
//                .collect(Collectors.toMap(s -> s.getClass(), s->s, (s1,s2) -> s2))
//        );

        var services = applicationContext.getBeansOfType(MessageService.class);
        oneBotListener.init(services);
        this.check = check;
        this.permission = permission;
    }
    @Override
    /*
      ioc容器加载完毕运行
     */
    public void run(String... args) throws Exception {
        PanelUtil.init();
        QQMsgUtil.init(applicationContext.getBean(QQMessageDao.class));
        MoliUtil.init(applicationContext.getBean("template",RestTemplate.class));
        permission.init(applicationContext);
//        initFountWidth();
//        ((LoggerContext)LoggerFactory.getILoggerFactory()).getLogger("com.mikuac.shiro.handler").setLevel(Level.DEBUG);

        ((TomcatWebServer) webServerApplicationContext.getWebServer())
                .getTomcat()
                .getConnector()
                .getProtocolHandler()
                .setExecutor(executor);


        Runtime.getRuntime().addShutdownHook(new Thread(() -> { //jvm结束钩子
            check.doEnd();
            ((ThreadPoolTaskExecutor)executor).shutdown();
            MatchListenerService.stopAllListener();
        }, "endThread"));
        log.info("启动成功");
        DiscordConfig discordConfig = applicationContext.getBean(DiscordConfig.class);
        log.info("dc conf: [{}]", discordConfig.getToken());
        /*
        if (NowbotConfig.QQ_LOGIN) {
            //登录
            bot.login();
            if (bot != null && bot.getGroup(746671531L) != null) {
                bot.getGroup(746671531L).sendMessage("启动完成");
            }
        }

         */

    }
}
