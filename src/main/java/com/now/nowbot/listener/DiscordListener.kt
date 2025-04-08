package com.now.nowbot.listener

import com.now.nowbot.aop.OpenResource
import com.now.nowbot.controller.BotWebApi
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.utils.FileUpload
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import java.lang.reflect.Method
import java.util.*

@Component
class DiscordListener(private val botWebApi: BotWebApi) : ListenerAdapter() {
    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        event.deferReply().queue()

        val first = botWebApi.javaClass.declaredMethods.firstOrNull { method: Method ->
            val annotation = method.getAnnotation(
                OpenResource::class.java
            )

            annotation?.name?.equals(event.name.substring(event.name.indexOf("-") + 1), ignoreCase = true) ?: false
        }

        if (first != null) {
            val parameters = first.parameters
            val objects = arrayOfNulls<Any>(parameters.size)
            try {
                for (i in parameters.indices) {
                    val parameter = parameters[i]
                    val parameterAnnotation =
                        parameter.getAnnotation(OpenResource::class.java) ?: continue
                    val option = event.getOption(parameterAnnotation.name.lowercase(Locale.getDefault())) ?: continue
                    val type = parameter.type

                    when (type) {
                        Int::class.javaPrimitiveType, Int::class.java -> {
                            objects[i] = option.asInt
                        }
                        Boolean::class.java, Boolean::class.javaPrimitiveType -> {
                            objects[i] = option.asBoolean
                        }
                        else -> {
                            objects[i] = option.asString
                        }
                    }
                }
                val invoke = first.invoke(botWebApi, *objects)
                if (invoke is ResponseEntity<*>) {
                    val response = invoke as ResponseEntity<ByteArray>
                    val fileUpload = FileUpload.fromData(response.body!!, event.name + ".png")
                    event.hook.sendFiles(fileUpload).queue()
                } else if (invoke is String) {
                    event.hook.sendMessage(invoke).queue()
                }
            } catch (e: Exception) {
                log.error("处理命令时发生了异常", e)
                val throwable = if (e.cause != null) {
                    e.cause
                } else {
                    e
                }
                event.hook.sendMessage("处理命令时发生了异常," + throwable!!.message).queue()
            }
        } else {
            event.hook.sendMessage("Can't find any handler to handle this command").queue()
        }
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(DiscordListener::class.java)
    }
}
