package com.now.nowbot

import com.now.nowbot.config.FuckOffRedisConfig
import com.now.nowbot.config.OneBotConfig
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.Banner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.context.annotation.EnableAspectJAutoProxy
import org.springframework.context.annotation.Import
import org.springframework.scheduling.annotation.EnableScheduling
import java.io.IOException

@EnableScheduling
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableAspectJAutoProxy(exposeProxy = true, proxyTargetClass = true)
@Import(OneBotConfig::class)
object NowbotApplication {
    var log: Logger = LoggerFactory.getLogger(NowbotApplication::class.java)

    @Throws(IOException::class)
    @JvmStatic
    fun main(args: Array<String>) {
        val application = SpringApplication(NowbotApplication::class.java)
        application.addListeners(FuckOffRedisConfig())
        application.setBanner { _, _, out ->
            out.println("""
 __   __                     _           _   
 \ \ / /   _ _ __ ___  _   _| |__   ___ | |_ 
  \ V / | | | '_ ` _ \| | | | '_ \ / _ \| __|
   | || |_| | | | | | | |_| | |_) | (_) | |_ 
   |_| \__,_|_| |_| |_|\__,_|_.__/ \___/ \__|
                                             
            """.trimIndent())
        }
        application.setBannerMode(Banner.Mode.CONSOLE)

        application.run(*args)
    }
}
