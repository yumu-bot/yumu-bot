package com.now.nowbot.listener

import com.now.nowbot.permission.PermissionImplement
import com.now.nowbot.qq.local.Bot
import com.now.nowbot.qq.local.Event
import com.now.nowbot.qq.local.contact.LocalGroup
import com.now.nowbot.service.MessageService
import com.now.nowbot.throwable.TipsException
import com.now.nowbot.throwable.TipsRuntimeException
import com.now.nowbot.throwable.botRuntimeException.LogException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.ExecutionException

class LocalCommandListener {
    private val bot: Bot = Bot()

    fun onMessage(message: String?) {
        val group = LocalGroup()
        val event = Event.GroupMessageEvent(bot, group, message)
        try {
            PermissionImplement.onMessage(event, ({ _, x ->
                when (x) {
                    is TipsException -> {
                        log.info("捕捉到异常提示：{}", x.message)
                        log.debug("异常详细信息: ", x)
                    }

                    is TipsRuntimeException -> {
                        log.info("捕捉到提示：{}", x.message)
                    }

                    is ExecutionException -> {
                        log.info("捕捉到并行中的提示", x)
                    }

                    is LogException -> {
                        log.info("捕捉到记录：{}", x.message)
                    }

                    else -> {
                        log.info("捕捉到异常：", x)
                    }
                }
            }))
        } catch (e: Exception) {
            log.info("捕捉到未知异常：{}", e.message)
            log.debug("异常详细信息:", e)
        }

        if (event.rawMessage.startsWith("/")) {
            try {
                PermissionImplement.onTencentMessage(event, (event::reply))
            } catch (e: Exception) {
                log.info("捕捉到腾讯异常：{}", e.message)
                log.debug("异常详细信息:", e)
            }
        }
    }

    companion object {
        val log: Logger = LoggerFactory.getLogger(LocalCommandListener::class.java)
        private var handler: MutableMap<String, out MessageService<Any>?> = mutableMapOf()

        fun setHandler(handler: Map<String, MessageService<*>>?) {
            @Suppress("UNCHECKED_CAST")
            Companion.handler = handler as? MutableMap<String, MessageService<Any>> ?: mutableMapOf()
        }

        fun startListener() {
            val listener = LocalCommandListener()
            Thread.startVirtualThread {
                Scanner(System.`in`).use { sc ->  // 使用 use 确保资源关闭
                    while (true) {
                        val input = sc.nextLine()
                        if (input.isBlank()) {
                            continue  // 跳过空输入
                        }

                        if (input.equals("exit", ignoreCase = true)) {
                            break  // 提供退出机制
                        }

                        Thread.startVirtualThread {
                            listener.onMessage(input)
                        }
                    }
                }
            }
        }
    }
}
