package com.now.nowbot.service

import com.github.benmanes.caffeine.cache.Caffeine
import com.neovisionaries.ws.client.WebSocketFactory
import com.now.nowbot.aop.OpenResource
import com.now.nowbot.config.AsyncSetting.THREAD_FACTORY
import com.now.nowbot.config.DiscordConfig
import com.now.nowbot.config.NowbotConfig
import com.now.nowbot.controller.BotWebApi
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import okhttp3.OkHttpClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.CacheManager
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.DependsOn
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.stereotype.Component
import java.lang.reflect.Parameter
import java.util.concurrent.Executor
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

@Component
@DependsOn("discordConfig") class DiscordService(
    private val discordConfig: DiscordConfig,
    private val config: NowbotConfig,
    private val listenerAdapters: List<ListenerAdapter>,
    private val botAsyncExecutor: ThreadPoolTaskExecutor,
) {
    val client: OkHttpClient = OkHttpClient.Builder()
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .connectTimeout(60, TimeUnit.SECONDS)
        .build()

    fun getJDA(): JDA? {
        if (discordConfig.token.isNullOrBlank()) return null

        val factory = WebSocketFactory()
        val proxy = factory.proxySettings

        if (config.proxyPort != 0) {
            proxy.apply {
                host = "localhost"
                port = config.proxyPort
            }
        }

        val jda: JDA

        try {
            val scheduledThreadPoolExecutor = ScheduledThreadPoolExecutor(0, THREAD_FACTORY)

            jda = JDABuilder.createDefault(discordConfig.token)
                .setHttpClient(client)
                .setWebsocketFactory(factory)
                .addEventListeners(listenerAdapters.toTypedArray())
                .setCallbackPool(botAsyncExecutor.threadPoolExecutor)
                .setGatewayPool(scheduledThreadPoolExecutor)
                .setRateLimitPool(scheduledThreadPoolExecutor)
                .setEventPool(botAsyncExecutor.threadPoolExecutor)
                .setAudioPool(scheduledThreadPoolExecutor).build()

            jda.awaitReady()
        } catch (e: Exception) {
            log.error("create jda error:", e)
            return null
        }

        for (command in jda.retrieveCommands().complete()) {
            command.delete().complete()
        }

        for (method in BotWebApi::class.java.declaredMethods) {
            val methodAnnotation: OpenResource = method.getAnnotation(OpenResource::class.java) ?: continue

            val name = methodAnnotation.name
            val commandData =
                Commands.slash((((discordConfig.commandSuffix ?: "") + name).lowercase()), methodAnnotation.desp)

            for (parameter in method.parameters) {
                val parameterAnnotation: OpenResource = method.getAnnotation(OpenResource::class.java) ?: continue

                val optionData = getOptionData(parameter, parameterAnnotation)

                commandData.addOptions(optionData)
            }

            jda.upsertCommand(commandData).complete()

        }
        log.info("jda init ok")

        return jda
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(DiscordService::class.java)

        private fun getOptionData(parameter: Parameter, parameterAnnotation: OpenResource): OptionData {
            val optionType: OptionType
            val type = parameter.type
            optionType = if (type == Int::class.javaPrimitiveType || type == Int::class.java) {
                OptionType.INTEGER
            } else if (type == Boolean::class.javaPrimitiveType || type == Boolean::class.java) {
                OptionType.BOOLEAN
            } else {
                OptionType.STRING
            }
            val parameterName = parameterAnnotation.name
            val optionData = OptionData(optionType, parameterName.lowercase(), parameterAnnotation.desp)
            if (parameterName == "mode") {
                optionData.addChoice("OSU", "OSU")
                optionData.addChoice("TAIKO", "TAIKO")
                optionData.addChoice("CATCH", "CATCH")
                optionData.addChoice("MANIA", "MANIA")
            }
            optionData.setRequired(parameterAnnotation.required)
            return optionData
        }

        @Value("\${server.port}") fun setPORT(port: Int) {
            NowbotConfig.PORT = port
        }

        @Bean fun cacheManager(mainExecutor: Executor): CacheManager {
            val caffeine =
                Caffeine.newBuilder().executor(mainExecutor).expireAfterAccess(5, TimeUnit.SECONDS).maximumSize(60)
            val manager = CaffeineCacheManager()
            manager.setCaffeine(caffeine)
            manager.isAllowNullValues = true
            return manager
        }
    }
}