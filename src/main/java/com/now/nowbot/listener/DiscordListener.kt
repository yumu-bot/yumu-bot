package com.now.nowbot.listener

import com.now.nowbot.aop.DiscordParam
import com.now.nowbot.controller.BotWebApi
import com.now.nowbot.throwable.TipsException
import com.now.nowbot.throwable.TipsRuntimeException
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.ExceptionEvent
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.StatusChangeEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.session.ReadyEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.utils.FileUpload
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import java.lang.reflect.Method
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors

@Component
class DiscordListener(private val botWebApi: BotWebApi) : ListenerAdapter() {

    private val executor = Executors.newCachedThreadPool()

    // 在类初始化时预加载
    private val annotatedMethods by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        botWebApi.javaClass.declaredMethods
            .mapNotNull { method ->
                val annotation = method.getAnnotation(DiscordParam::class.java) ?: return@mapNotNull null

                val namedMethod: Pair<String, Method> = annotation.name to method.apply { isAccessible = true }
                namedMethod
            }
            .toMap()
    }


    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        event.deferReply().queue()

        // 立即提交到线程池异步处理
        CompletableFuture.runAsync({
            handleCommandAsync(event)
        }, executor).exceptionally { throwable ->
            log.error("命令处理异常", throwable)
            event.hook.sendMessage("❌ 命令处理失败: ${throwable.message}").queue()
            null
        }
    }

    private fun handleCommandAsync(event: SlashCommandInteractionEvent) {
        val methodName = event.name.substringAfter("-")

        val first = annotatedMethods.entries
            .firstOrNull { (name, _) ->
                name.equals(methodName, ignoreCase = true)
            }?.value

        if (first != null) {
            val parameters = first.parameters
            val objects = arrayOfNulls<Any>(parameters.size)
            try {
                for (i in parameters.indices) {
                    val parameter = parameters[i]
                    val parameterAnnotation =
                        parameter.getAnnotation(DiscordParam::class.java) ?: continue
                    val option = event.getOption(parameterAnnotation.name.lowercase(Locale.getDefault())) ?: continue
                    val type = parameter.type

                    objects[i] = when (type) {
                        Int::class.javaPrimitiveType, Int::class.java -> option.asInt
                        Long::class.javaPrimitiveType, Long::class.java -> option.asLong
                        Boolean::class.javaPrimitiveType, Boolean::class.java -> option.asBoolean
                        else -> option.asString
                    }
                }

                val invoke = first.invoke(botWebApi, *objects)
                handleInvokeResult(event, invoke)
            } catch (e: TipsException) {
                event.hook.sendMessage(e.message ?: "发生错误").queue()
            } catch (e: TipsRuntimeException) {
                event.hook.sendMessage(e.message ?: "发生错误").queue()
            } catch (e: RuntimeException) {
                log.error(e.message, e)
                event.hook.sendMessage(e.message ?: "发生错误").queue()
            } catch (e: Exception) {
                log.error("❌ 处理命令时发生了异常", e)
                val errorMessage = "处理命令时，发生了预料之外的异常。"
                event.hook.sendMessage(errorMessage).queue()
            }
        } else {
            event.hook.sendMessage("❌ 找不到对应的命令处理器").queue()
        }
    }

    private fun handleInvokeResult(event: SlashCommandInteractionEvent, result: Any?) {
        when (result) {
            is ResponseEntity<*> -> {
                @Suppress("UNCHECKED_CAST")
                val response = result as ResponseEntity<ByteArray>
                if (response.body != null) {
                    val fileUpload = FileUpload.fromData(response.body!!, "${event.name}.png")
                    event.hook.sendFiles(fileUpload).queue()
                } else {
                    event.hook.sendMessage("❌ 响应内容为空").queue()
                }
            }
            is String -> {
                event.hook.sendMessage(result).queue()
            }
            else -> {
                event.hook.sendMessage("✅ 操作完成").queue()
            }
        }
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(DiscordListener::class.java)
    }
}

@Component
class BotInviteHelper {

    fun generateInviteLink(jda: JDA): String {
            // 必需的权限
            val permissions = listOf(
                Permission.VIEW_CHANNEL,
                Permission.MESSAGE_SEND,
                Permission.MESSAGE_SEND_IN_THREADS,
                Permission.MESSAGE_EMBED_LINKS,
                Permission.MESSAGE_ATTACH_FILES,
                Permission.MESSAGE_HISTORY,
                Permission.MESSAGE_EXT_EMOJI,
                Permission.MESSAGE_ADD_REACTION,
                Permission.USE_APPLICATION_COMMANDS,
                Permission.MESSAGE_MANAGE
            )

            val permissionValue = permissions.sumOf { it.rawValue }

            val inviteUrl = "https://discord.com/oauth2/authorize" +
                    "?client_id=${jda.selfUser.applicationId}" +
                    "&permissions=$permissionValue" +
                    "&scope=bot%20applications.commands" +
                    "&response_type=code"
            return inviteUrl
        }

//    companion object {
//        private val log = LoggerFactory.getLogger(BotInviteHelper::class.java)
//    }
}

@Component
class GatewayMonitor : ListenerAdapter() {

    override fun onReady(event: ReadyEvent) {
        log.info("🚀 JDA 准备就绪")
        log.info("👤 用户: ${event.jda.selfUser.name}")
        log.info("📊 状态: ${event.jda.status}")
        log.info("🔗 网关: ${event.jda.gatewayIntents}")
        log.info("🏠 服务器数量: ${event.jda.guilds.size}")
    }

    override fun onStatusChange(event: StatusChangeEvent) {
        log.debug("🔄 状态变更: {} -> {}", event.oldStatus, event.newStatus)
    }

    override fun onException(event: ExceptionEvent) {
        log.error("💥 JDA 异常", event.cause)
    }

    override fun onGenericEvent(event: GenericEvent) {
        // 记录所有事件（调试用）
        when (event) {
            is MessageReceivedEvent -> log.debug("📨 收到消息事件")
            is SlashCommandInteractionEvent -> {
                log.debug("🎯 收到 SLASH 命令事件: ${event.name}")
                log.debug("🎯 命令详情: user=${event.user.name}, channel=${event.channel.name}")
            }
            else -> {
                // 不记录其他事件避免日志过多
            }
        }
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(GatewayMonitor::class.java)
    }
}
