package com.now.nowbot.config

import com.fasterxml.jackson.databind.JsonNode
import com.now.nowbot.aop.CheckAspect
import com.now.nowbot.listener.LocalCommandListener
import com.now.nowbot.permission.PermissionImplement
import com.now.nowbot.qq.tencent.YumuServer
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.PerformancePlusService
import com.now.nowbot.service.messageServiceImpl.MatchListenerService
import com.now.nowbot.service.messageServiceImpl.SystemInfoService
import com.now.nowbot.service.osuApiService.OsuUserApiService
import com.now.nowbot.util.CmdUtil
import com.now.nowbot.util.JacksonUtil
import com.now.nowbot.util.QQMsgUtil
import com.now.nowbot.util.UserIDUtil
import jakarta.annotation.Resource
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.system.ApplicationHome
import org.springframework.boot.web.context.WebServerApplicationContext
import org.springframework.boot.web.embedded.tomcat.TomcatWebServer
import org.springframework.context.ApplicationContext
import org.springframework.core.env.Environment
import org.springframework.core.io.DefaultResourceLoader
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.stereotype.Component
import java.io.IOException
import java.util.concurrent.Executor

@Component
class IocAllReadyRunner(
    private val applicationContext: ApplicationContext,
    private val check: CheckAspect,
    private val permission: Permission,
    private val permissionImplement: PermissionImplement
) : CommandLineRunner {

    companion object {
        var APP_ALIVE = true
    }

    private val log = LoggerFactory.getLogger("IocAllReadyRunner")

    @Resource
    private lateinit var webServerApplicationContext: WebServerApplicationContext

    @Resource(name = "mainExecutor")
    private lateinit var executor: Executor

    @Resource
    private lateinit var env: Environment

    init {
        val services = applicationContext.getBeansOfType(MessageService::class.java)
        YumuServer.userApiService = applicationContext.getBean(OsuUserApiService::class.java)
        CmdUtil.init(applicationContext)
        UserIDUtil.init(applicationContext)

        LocalCommandListener.setHandler(services)
    }

    /**
     * ioc容器加载完毕运行
     */
    override fun run(vararg args: String) {
        QQMsgUtil.init(applicationContext.getBean(YumuConfig::class.java))
        val services = applicationContext.getBeansOfType(MessageService::class.java)

        permissionImplement.init(services)
        permission.init(applicationContext)

        //        initFountWidth()

        (webServerApplicationContext.webServer as TomcatWebServer)
            .tomcat
            .connector
            .protocolHandler
            .executor = executor

        Runtime.getRuntime().addShutdownHook(Thread({
            APP_ALIVE = false
            // check.doEnd()
            (executor as ThreadPoolTaskExecutor).shutdown()
            //MatchListenerServiceLegacy.stopAllListener()
            MatchListenerService.stopAllListenerFromReboot()
        }, "endThread"))

        log.info("新人群配置: {}", env.getProperty("spring.datasource.newbie.enable", "false"))

        try {
            val debuging = ApplicationHome(NowbotConfig::class.java).source?.parentFile?.toString()?.contains("target") ?: false
            if (debuging) {
                PerformancePlusService.runDevelopment()
                startCommandListener()
            }
        } catch (_: Exception) {
            log.info("非 debug 环境, 停止加载命令行输入")
        }

        val resource = DefaultResourceLoader().getResource("classpath:build-info.json")
        if (resource.exists()) {
            try {
                val b = resource.inputStream.readAllBytes()
                val node = JacksonUtil.parseObject(b, JsonNode::class.java)
                    ?: throw IOException()
                val map = SystemInfoService.INFO_MAP
                map["最近构建时间"] = node.get("git.build.time").asText("未知")
                map["最近代码版本"] = node.get("git.commit.id.abbrev").asText("未知")
            } catch (e: Exception) {
                log.error("解析 git json 出错", e)
            }
        }
        log.info("启动成功")
    }

    private fun startCommandListener() {
        LocalCommandListener.startListener()
        log.info("命令行输入已启动!")
    }
}