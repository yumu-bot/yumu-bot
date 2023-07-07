package com.now.nowbot.config;

import com.now.nowbot.aop.CheckAspect;
import com.now.nowbot.dao.QQMessageDao;
import com.now.nowbot.listener.MessageListener;
import com.now.nowbot.listener.MiraiListener;
import com.now.nowbot.service.MessageService.MessageService;
import com.now.nowbot.util.*;
import com.now.nowbot.util.Panel.HCardBuilder;
import com.now.nowbot.util.Panel.J1CardBuilder;
import net.mamoe.mirai.Bot;
import org.jetbrains.skija.Font;
import org.jetbrains.skija.TextLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;

@Component
public class IocAllReadyRunner implements CommandLineRunner {
    Logger log = LoggerFactory.getLogger("IocAllReadyRunner");
    Bot bot;
    ApplicationContext applicationContext;
    CheckAspect check;
    Permission permission;

    @Autowired
    public IocAllReadyRunner(Bot bot, MiraiListener messageListener, ApplicationContext applicationContext, CheckAspect check, Permission permission){
        this.bot = bot;
        this.applicationContext = applicationContext;
        var serviceMap = new HashMap<Class<? extends MessageService>, MessageService>();
        for (var i : Instruction.values()){
            var iClass = i.getaClass();
            serviceMap.put(iClass, applicationContext.getBean(iClass));
        }
        messageListener.init(serviceMap);
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
        MoliUtil.init(applicationContext.getBean(RestTemplate.class));
        permission.init(applicationContext);
        initFountWidth();

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
