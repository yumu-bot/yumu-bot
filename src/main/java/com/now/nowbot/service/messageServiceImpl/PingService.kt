package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.config.NowbotConfig
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.qq.tencent.TencentMessageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.util.DataUtil.TORUS_REGULAR
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.OfficialInstruction
import com.now.nowbot.util.QQMsgUtil
import io.github.humbleui.skija.*
import org.springframework.stereotype.Service
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.regex.Matcher

@Service("PING") class PingService : MessageService<Matcher>, TencentMessageService<Matcher> {
    override fun isHandle(event: MessageEvent, messageText: String, data: DataValue<Matcher>): Boolean {
        val m = Instruction.PING.matcher(messageText)
        if (!m.find()) {
            return false
        }

        data.value = m
        return true
    }

    @Throws(Throwable::class) override fun HandleMessage(event: MessageEvent, param: Matcher) {
        event.reply(image).recallIn(5000)
    }

    override fun accept(event: MessageEvent, messageText: String): Matcher? {
        val m = OfficialInstruction.PING.matcher(messageText)
        if (!m.find()) {
            return null
        }
        return m
    }

    @Throws(Throwable::class) override fun reply(event: MessageEvent, param: Matcher): MessageChain? {
        return QQMsgUtil.getImage(image)
    }

    val image: ByteArray
        get() {
            Surface.makeRaster(ImageInfo.makeN32Premul(648, 648)).use {
                val canvas = it.canvas
                try {
                    val file = Files.readAllBytes(
                        Path.of(NowbotConfig.EXPORT_FILE_PATH).resolve("help-ping.png")
                    )
                    val background = Image.makeDeferredFromEncodedBytes(file)
                    canvas.drawImage(background, 0f, 0f)
                } catch (ignored: IOException) {
                    throw RuntimeException("ping failed cuz no BG??!")
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
                return EncoderPNG.encode(it.makeImageSnapshot())!!.bytes
            }
        }
}
