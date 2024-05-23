package com.now.nowbot.config;

import com.now.nowbot.aop.CheckAspect;
import com.now.nowbot.dao.QQMessageDao;
import com.now.nowbot.listener.LocalCommandListener;
import com.now.nowbot.listener.OneBotListener;
import com.now.nowbot.permission.PermissionImplement;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.MessageServiceImpl.MatchListenerService;
import com.now.nowbot.service.PerformancePlusService;
import com.now.nowbot.util.HandleUtil;
import com.now.nowbot.util.MoliUtil;
import com.now.nowbot.util.QQMsgUtil;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.system.ApplicationHome;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.boot.web.embedded.tomcat.TomcatWebServer;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.Executor;

@Component
public class IocAllReadyRunner implements CommandLineRunner {
    Logger             log = LoggerFactory.getLogger("IocAllReadyRunner");
    ApplicationContext applicationContext;
    CheckAspect        check;
    Permission         permission;
    PermissionImplement permissionImplement;
    @Resource
    WebServerApplicationContext webServerApplicationContext;
    @Resource(name = "mainExecutor")
    Executor executor;

    @Autowired
    public IocAllReadyRunner(OneBotListener oneBotListener, ApplicationContext applicationContext, CheckAspect check, Permission permission, PermissionImplement permissionImplement) {
        this.applicationContext = applicationContext;
        var services = applicationContext.getBeansOfType(MessageService.class);
        LocalCommandListener.setHandler(services);
        this.check = check;
        this.permission = permission;
        this.permissionImplement = permissionImplement;
    }

    @Override
    /*
      ioc容器加载完毕运行
     */
    public void run(String... args) {
        QQMsgUtil.init(applicationContext.getBean(QQMessageDao.class), applicationContext.getBean(YumuConfig.class));
        MoliUtil.init(applicationContext.getBean("template", RestTemplate.class));
        var services = applicationContext.getBeansOfType(MessageService.class);
        permissionImplement.init(services);
        permission.init(applicationContext);
        HandleUtil.init(applicationContext);

//        initFountWidth();

        ((TomcatWebServer) webServerApplicationContext.getWebServer())
                .getTomcat()
                .getConnector()
                .getProtocolHandler()
                .setExecutor(executor);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> { //jvm结束钩子
            check.doEnd();
            ((ThreadPoolTaskExecutor) executor).shutdown();
            MatchListenerService.stopAllListener();
        }, "endThread"));

        var rsource = applicationContext.getResource("classpath:/model/nsfw.onnx");

        DiscordConfig discordConfig = applicationContext.getBean(DiscordConfig.class);
        log.info("dc conf: [{}]", discordConfig.getToken());

        try {
            boolean debuging = new ApplicationHome(NowbotConfig.class).getSource().getParentFile().toString().contains("target");
            if (debuging) {
                PerformancePlusService.runDevelopment();
                startCommandListener();
            }
        } catch (Exception e) {
            log.info("非 debug 环境, 停止加载命令行输入");
        }
        log.info("启动成功");
    }

    private void startCommandListener() {
        LocalCommandListener.startListener();
        log.info("命令行输入已启动!");

    }
}
