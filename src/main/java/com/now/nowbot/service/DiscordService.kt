package com.now.nowbot.service

import com.neovisionaries.ws.client.WebSocketFactory
import com.now.nowbot.aop.DiscordParam
import com.now.nowbot.config.AsyncSetting
import com.now.nowbot.config.DiscordConfig
import com.now.nowbot.config.NowbotConfig
import com.now.nowbot.controller.BotWebApi
import com.now.nowbot.listener.BotInviteHelper
import jakarta.annotation.PostConstruct
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.commands.Command
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import net.dv8tion.jda.api.requests.GatewayIntent
import okhttp3.OkHttpClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.DependsOn
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.stereotype.Component
import java.lang.reflect.Parameter
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@Component
@DependsOn("discordConfig")
class DiscordService(
    private val discordConfig: DiscordConfig,
    private val config: NowbotConfig,
    private val listenerAdapters: List<ListenerAdapter>,
    private val botAsyncExecutor: ThreadPoolTaskExecutor,
) {

    private fun createHttpClientWithExplicitProxy(): OkHttpClient {
        // 明确使用 HTTP 代理
        val proxy = if (config.proxyPort != 0) {
            Proxy(Proxy.Type.HTTP, InetSocketAddress(config.proxyHost, config.proxyPort))
        } else {
            Proxy.NO_PROXY
        }

        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .protocols(listOf(okhttp3.Protocol.HTTP_1_1))
            .proxy(proxy)
            .retryOnConnectionFailure(true)
            .build()
    }

    private var jdaInstance: JDA? = null

    @PostConstruct
    fun initialize() {
        log.info("开始异步初始化 Discord Bot 连接...")

        botAsyncExecutor.execute {
            var isConnected = false
            var retryCount = 0
            val maxRetries = 10 // 最大尝试 10 次

            while (!isConnected && retryCount < maxRetries) {
                try {
                    jdaInstance = createJDA()
                    if (jdaInstance != null) {
                        isConnected = true
                        log.info("✅ Discord Bot 连接成功！")
                    } else {
                        // Token 未配置等无法恢复的错误，直接中断重试
                        log.warn("⚠️ Discord Bot 配置无效，停止自动重试")
                        break
                    }
                } catch (e: Exception) {
                    retryCount++
                    log.error("❌ Discord Bot 连接失败 (尝试 $retryCount/$maxRetries): ${e.message}")

                    if (retryCount < maxRetries) {
                        log.info("⏳ 10 秒后将尝试重新连接...")
                        try {
                            Thread.sleep(30.seconds.inWholeMilliseconds) // 等待 10 秒后再试
                        } catch (_: InterruptedException) {
                            Thread.currentThread().interrupt()
                            break
                        }
                    } else {
                        log.error("💥 已达到最大重试次数，Discord Bot 放弃初始化！")
                    }
                }
            }
        }
    }

    private fun createJDA(): JDA? {
        if (discordConfig.token.isNullOrBlank()) {
            log.error("❌ Discord Token 未配置！")
            return null
        }

        log.info("🔄 尝试连接 Discord...")

        return try {
            val factory = WebSocketFactory().apply {
                connectionTimeout = 10.seconds.inWholeMilliseconds.toInt()
                serverNames = arrayOf("gateway.discord.gg")
            }

            // 统一且干净的 HTTP 代理配置
            if (config.proxyPort != 0) {
                log.info("🔌 使用 HTTP 代理: ${config.proxyHost}:${config.proxyPort}")
                factory.proxySettings.apply {
                    host = config.proxyHost
                    port = config.proxyPort
                }
            }

            val scheduledThreadPoolExecutor = ScheduledThreadPoolExecutor(0, AsyncSetting.threadFactory)

            log.info("🔄 创建 JDA 实例...")
            val jda = JDABuilder.createDefault(discordConfig.token)
                .setAutoReconnect(true)
                .setMaxReconnectDelay(10.minutes.inWholeSeconds.toInt())
                .setHttpClient(createHttpClientWithExplicitProxy())
                .setWebsocketFactory(factory)
                .addEventListeners(*listenerAdapters.toTypedArray())
                .setCallbackPool(botAsyncExecutor.threadPoolExecutor)
                .setGatewayPool(scheduledThreadPoolExecutor)
                .setRateLimitScheduler(scheduledThreadPoolExecutor)
                .setEventPool(botAsyncExecutor.threadPoolExecutor)
                .setAudioPool(scheduledThreadPoolExecutor)
                .enableIntents(
                    GatewayIntent.GUILD_MEMBERS,
                    GatewayIntent.GUILD_MESSAGES,
                    GatewayIntent.GUILD_MESSAGE_REACTIONS,
                    GatewayIntent.DIRECT_MESSAGES,
                    GatewayIntent.MESSAGE_CONTENT
                )
                .build()

            log.info("⏳ JDA 连接中...")
            jda.awaitReady()
            log.info("✅ JDA 连接成功！用户: ${jda.selfUser.name}")

            registerCommands(jda)
            val inviteHelper = BotInviteHelper()
            val inviteLink = inviteHelper.generateInviteLink(jda)
            log.info("📧 请使用此链接邀请 Bot 到服务器: $inviteLink")

            jda
        } catch (e: Exception) {
            log.error("❌ JDA 连接失败:", e)
            null
        }
    }

    @Bean
    fun getJDA(): JDA? = jdaInstance

    private fun registerCommands(jda: JDA) {
        try {
            val existingCommands = jda.retrieveCommands().complete().associateBy { it.name }
            val localCommands = buildLocalCommands()

            // 找出需要删除的命令（本地不存在但远程存在）
            val commandsToDelete = existingCommands.keys - localCommands.keys
            commandsToDelete.forEach { commandName ->
                log.debug("🗑️ 删除废弃命令: $commandName")
                existingCommands[commandName]?.delete()?.queue()
            }

            // 更新或创建命令
            localCommands.forEach { (commandName, commandData) ->
                val existingCommand = existingCommands[commandName]

                if (existingCommand == null) {
                    // 新命令 - 创建
                    jda.upsertCommand(commandData).queue(
                        { log.debug("📝 创建新命令: $commandName") },
                        { error -> log.error("创建命令失败: $commandName", error) }
                    )
                } else if (hasCommandChanged(existingCommand, commandData as SlashCommandData)) {
                    // 命令有变更 - 更新
                    jda.upsertCommand(commandData).queue(
                        { log.debug("🔄 更新命令: $commandName") },
                        { error -> log.error("更新命令失败: $commandName", error) }
                    )
                } else {
                    // 命令无变更 - 跳过
                    log.debug("⏭️ 跳过未变更命令: $commandName")
                }
            }

        } catch (e: Exception) {
            log.error("命令注册失败:", e)
        }
    }

    private fun buildLocalCommands(): Map<String, CommandData> {
        return BotWebApi::class.java.declaredMethods
            .mapNotNull { method ->
                val annotation = method.getAnnotation(DiscordParam::class.java) ?: return@mapNotNull null

                val commandName = ((discordConfig.commandSuffix ?: "") + annotation.name).lowercase()
                val commandData = Commands.slash(commandName, annotation.description)

                method.parameters.forEach { parameter ->
                    val paramAnnotation = parameter.getAnnotation(DiscordParam::class.java) ?: return@forEach
                    val optionData = createOptionData(parameter, paramAnnotation)
                    commandData.addOptions(optionData)
                }

                commandName to commandData
            }
            .toMap()
    }

    // 使用方式
    private fun hasCommandChanged(existing: Command, newData: SlashCommandData): Boolean {
        val diff = CommandComparator.compareCommands(existing, newData)
        if (diff.hasChanges) {
            log.debug("命令 '{}' 有变更: {}", existing.name, diff.differences.joinToString())
        }
        return diff.hasChanges
    }

    object CommandComparator {

        fun compareCommands(existing: Command, newData: SlashCommandData): CommandDiff {
            val differences = mutableListOf<String>()

            if (existing.name != newData.name) {
                differences.add("名称: '${existing.name}' -> '${newData.name}'")
            }
            if (existing.description != newData.description) {
                differences.add("描述变更")
            }
            if (existing.type != newData.type) {
                differences.add("类型: ${existing.type} -> ${newData.type}")
            }

            val optionDiff = compareOptions(existing.options, newData.options)
            differences.addAll(optionDiff)

            return CommandDiff(
                commandName = existing.name,
                hasChanges = differences.isNotEmpty(),
                differences = differences
            )
        }

        private fun compareOptions(
            existingOptions: List<Command.Option>,
            newOptions: List<OptionData>
        ): List<String> {
            val differences = mutableListOf<String>()

            if (existingOptions.size != newOptions.size) {
                differences.add("选项数量: ${existingOptions.size} -> ${newOptions.size}")
                return differences
            }

            existingOptions.forEachIndexed { index, existingOpt ->
                val newOpt = newOptions[index]

                if (existingOpt.name != newOpt.name) {
                    differences.add("选项${index}名称: '${existingOpt.name}' -> '${newOpt.name}'")
                }
                if (existingOpt.description != newOpt.description) {
                    differences.add("选项${index}描述变更")
                }
                if (existingOpt.type != newOpt.type) {
                    differences.add("选项${index}类型: ${existingOpt.type} -> ${newOpt.type}")
                }
                if (existingOpt.isRequired != newOpt.isRequired) {
                    differences.add("选项${index}必需性: ${existingOpt.isRequired} -> ${newOpt.isRequired}")
                }
            }

            return differences
        }

        data class CommandDiff(
            val commandName: String,
            val hasChanges: Boolean,
            val differences: List<String>
        )
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(DiscordService::class.java)

        private fun createOptionData(parameter: Parameter, annotation: DiscordParam): OptionData {
            val optionType = when (parameter.type) {

                Int::class.javaPrimitiveType,
                Int::class.java -> OptionType.INTEGER

                Boolean::class.javaPrimitiveType,
                Boolean::class.java -> OptionType.BOOLEAN

                else -> OptionType.STRING
            }

            return OptionData(optionType, annotation.name.lowercase(), annotation.description).apply {
                isRequired = annotation.required
                // 添加特定的选项
                if (annotation.name == "mode") {
                    addChoice("OSU", "OSU")
                    addChoice("TAIKO", "TAIKO")
                    addChoice("CATCH", "CATCH")
                    addChoice("MANIA", "MANIA")
                }
            }
        }
    }
}