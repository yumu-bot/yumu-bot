package com.now.nowbot.config;

import com.now.nowbot.aop.CheckAspect;
import com.now.nowbot.dao.QQMessageDao;
import com.now.nowbot.listener.MiraiListener;
import com.now.nowbot.listener.OneBotListener;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.util.MoliUtil;
import com.now.nowbot.util.Panel.HCardBuilder;
import com.now.nowbot.util.PanelUtil;
import com.now.nowbot.util.QQMsgUtil;
import com.now.nowbot.util.SkiaUtil;
import io.github.humbleui.skija.Font;
import io.github.humbleui.skija.TextLine;
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
    public IocAllReadyRunner(MiraiListener messageListener, OneBotListener oneBotListener, ApplicationContext applicationContext, CheckAspect check, Permission permission){
        this.applicationContext = applicationContext;

//        serviceMap.putAll(applicationContext
//                .getBeansOfType(MessageService.class)
//                .values()
//                .stream()
//                .collect(Collectors.toMap(s -> s.getClass(), s->s, (s1,s2) -> s2))
//        );

        var services = applicationContext.getBeansOfType(MessageService.class);
        messageListener.init(services);
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
        }, "endThread"));
        log.info("启动成功");
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

    public static void initFountWidth() {
        var face = SkiaUtil.getTorusSemiBold();
        Font fontS48 = new Font(face, 48);
        Font fontS24 = new Font(face, 24);
        Font fontS36 = new Font(face, 36);
        try (face; fontS48; fontS36; fontS24){
            for (int i = 0; i < 254; i++) {
                HCardBuilder.F24L.add(TextLine.make(String.valueOf((char) i), fontS24).getWidth());
                HCardBuilder.F36L.add(TextLine.make(String.valueOf((char) i), fontS36).getWidth());
            }
        }
    }

}
