package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.qq.tencent.TencentMessageService
import com.yumu.qq.message.Keyboard
import org.springframework.stereotype.Service

@Service("TEST_MARKDOWN")
class TestMarkdownService : TencentMessageService<Unit> {
    override fun accept(event: MessageEvent, messageText: String): Unit? {
        if (messageText.startsWith("/test md")) {
            return Unit
        } else {
            return null
        }
    }

    override fun reply(
        event: MessageEvent,
        param: Unit
    ): MessageChain? {
        val md = """
            # 标题
            
            > 123
            > 456
            > 789
            
            ```
            interface MessageService<T> {
                @Throws(Throwable::class) fun isHandle(
                    event: MessageEvent,
                    messageText: String,
                    data: DataValue<T>
                ): Boolean

                @Throws(Throwable::class) fun handleMessage(event: MessageEvent, param: T): ServiceCallStatistic?

                class DataValue<T> {
                    var value: T? = null
                }
            }

            ```
        """.trimIndent()

        val buttons = listOf(
            Keyboard.Button("上一页", "/up").action { enter = true },
            Keyboard.Button("首页", "/home").action { enter = true },
            Keyboard.Button("下一页", "/down").action { enter = true }
        )

        val kb = Keyboard(
            keyboards = listOf(buttons)
        )

        return MessageChain
            .builder()
            .addMarkdown(md)
            .addKyeboard(kb)
            .build()
    }
}