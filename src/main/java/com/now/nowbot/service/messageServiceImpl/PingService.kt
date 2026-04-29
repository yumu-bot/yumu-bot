package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.config.NowbotConfig
import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.qq.tencent.TencentMessageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.util.DataUtil.TORUS_REGULAR
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.OfficialInstruction
import io.github.humbleui.skija.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

@Service("PING") class PingService : MessageService<Unit>, TencentMessageService<Unit> {
    override fun isHandle(event: MessageEvent, messageText: String, data: DataValue<Unit>): Boolean {
        val m = Instruction.PING.matcher(messageText)
        if (!m.find()) {
            return false
        }

        data.value = Unit
        return true
    }

    @Throws(Throwable::class) override fun handleMessage(event: MessageEvent, param: Unit): ServiceCallStatistic? {
        event.reply(getMessageChain()).recallIn(10 * 1000L)
        return ServiceCallStatistic.building(event)
    }

    override fun accept(event: MessageEvent, messageText: String): Unit? {
        val m = OfficialInstruction.PING.matcher(messageText)
        if (!m.find()) {
            return null
        }

        return Unit
    }

    @Throws(Throwable::class) override fun reply(event: MessageEvent, param: Unit): MessageChain? {
        return getMessageChain()
    }

    fun getMessageChain(): MessageChain {
        Surface.makeRaster(ImageInfo.makeN32Premul(648, 648)).use {
            val canvas = it.canvas
            val path = Path.of(NowbotConfig.EXPORT_FILE_PATH).resolve("help-ping.png")

            try {
                val file = Files.readAllBytes(path)
                val background = Image.makeDeferredFromEncodedBytes(file)
                canvas.drawImage(background, 0f, 0f)
            } catch (_: IOException) {
                log.error("""
                    没有 Ping 底图呢...
                    请确保你拥有 $path 这张底图！
                    """.trimIndent())
                return MessageChain("小沐收到！")
            }

            val textPaint = Paint().setARGB(255, 191, 193, 124)
            val millisPaint = Paint().setARGB(200, 191, 193, 124)

            var x = Font(TORUS_REGULAR, 160f)
            var t = TextLine.make("?", x)

            canvas.drawTextLine(t, (648 - t.width) / 2, 208f, textPaint)
            textPaint.close()

            x.close()
            t.close()

            x = Font(TORUS_REGULAR, 40f)
            t = TextLine.make(
                System.currentTimeMillis().toString() + "ms", x
            )

            canvas.drawTextLine(t, 10f, t.capHeight + 10, millisPaint)
            millisPaint.close()

            x.close()
            t.close()
            return MessageChain(EncoderPNG.encode(it.makeImageSnapshot())!!.bytes)
        }
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(PingService::class.java)
    }
}
