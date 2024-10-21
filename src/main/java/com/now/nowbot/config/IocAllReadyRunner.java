package com.now.nowbot.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.now.nowbot.aop.CheckAspect;
import com.now.nowbot.listener.LocalCommandListener;
import com.now.nowbot.permission.PermissionImplement;
import com.now.nowbot.qq.tencent.YumuServer;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.messageServiceImpl.MatchListenerService;
import com.now.nowbot.service.messageServiceImpl.SystemInfoService;
import com.now.nowbot.service.osuApiService.OsuUserApiService;
import com.now.nowbot.service.PerformancePlusService;
import com.now.nowbot.util.*;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.system.ApplicationHome;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.boot.web.embedded.tomcat.TomcatWebServer;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.Objects;
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
    Executor    executor;
    @Resource
    Environment env;

    @Autowired
    public IocAllReadyRunner(
            ApplicationContext applicationContext,
            CheckAspect check,
            Permission permission,
            PermissionImplement permissionImplement) {
        this.applicationContext = applicationContext;
        var services = applicationContext.getBeansOfType(MessageService.class);
        YumuServer.userApiService = applicationContext.getBean(OsuUserApiService.class);
        CmdUtil.init(applicationContext);
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
        QQMsgUtil.init(applicationContext.getBean(YumuConfig.class));
        MoliUtil.init(applicationContext.getBean("template", RestTemplate.class));
        var services = applicationContext.getBeansOfType(MessageService.class);
        permissionImplement.init(services);
        permission.init(applicationContext);

//        initFountWidth();

        ((TomcatWebServer) webServerApplicationContext.getWebServer())
                .getTomcat()
                .getConnector()
                .getProtocolHandler()
                .setExecutor(executor);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> { //jvm结束钩子
            check.doEnd();
            ((ThreadPoolTaskExecutor) executor).shutdown();
            //MatchListenerServiceLegacy.stopAllListener();
            MatchListenerService.Companion.stopAllListenerFromReboot();
        }, "endThread"));

        log.info("新人群配置: {}", env.getProperty("spring.datasource.newbie.enable", "false"));

        try {
            boolean debuging = new ApplicationHome(NowbotConfig.class).getSource().getParentFile().toString().contains("target");
            if (debuging) {
                PerformancePlusService.runDevelopment();
                startCommandListener();
            }
        } catch (Exception e) {
            log.info("非 debug 环境, 停止加载命令行输入");
        }

        var resource = new DefaultResourceLoader().getResource("classpath:build-info.json");
        if (resource.exists()) try {
            var b = resource.getInputStream().readAllBytes();
            var node = JacksonUtil.parseObject(b, JsonNode.class);
            if (Objects.isNull(node)) throw new IOException();
            var map = SystemInfoService.INFO_MAP;
            map.put("最近构建时间", node.get("git.build.time").asText("未知"));
            map.put("最近代码版本", node.get("git.commit.id.abbrev").asText("未知"));
        } catch (Exception e) {
            log.error("解析 git json 出错", e);
        }
        log.info("启动成功");
    }
    

    private void startCommandListener() {
        LocalCommandListener.startListener();
        log.info("命令行输入已启动!");

    }
}
