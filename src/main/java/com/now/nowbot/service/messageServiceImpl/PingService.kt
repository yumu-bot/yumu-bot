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
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.awt.Color
import java.awt.RenderingHints
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.file.Path
import javax.imageio.ImageIO

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
        event.replyAndRecallAsync(getMessageChain(), )
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
        val path = Path.of(NowbotConfig.EXPORT_FILE_PATH).resolve("help-ping.png")

        // 1. 读取底图
        val image = try {
            ImageIO.read(path.toFile()) ?: throw IOException("底图为空")
        } catch (_: IOException) {
            log.error("""
            没有 Ping 底图呢...
            请确保你拥有 $path 这张底图！
        """.trimIndent())
            return MessageChain("小沐收到！")
        }

        // 2. 创建画布并配置
        val g = image.createGraphics()
        try {
            // 开启抗锯齿，保证文字平滑不锯齿
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

            // 3. 绘制居中的 "?"
            // 注意：如果 TORUS_REGULAR 是 java.awt.Font 类型可以直接用，如果是路径则用 Font.deriveFont() 或 Font.decode()
            val textFont = TORUS_REGULAR.deriveFont(160f)
            g.font = textFont
            g.color = Color(191, 193, 124, 255)

            val text = "?"
            val metrics = g.fontMetrics
            val textWidth = metrics.stringWidth(text)
            val x = (648 - textWidth) / 2

            // 绘制文本 (Graphics2D 的 y 坐标是文字基线 Baseline)
            g.drawString(text, x, 208)

            // 4. 绘制左上角时间戳
            val millisFont = TORUS_REGULAR.deriveFont(40f)
            g.font = millisFont
            g.color = Color(191, 193, 124, 200)

            val millisText = "${System.currentTimeMillis()}ms"
            val millisMetrics = g.fontMetrics
            val millisY = 10 + millisMetrics.ascent

            g.drawString(millisText, 10, millisY)

        } finally {
            g.dispose() // 释放绘图句柄
        }

        // 5. 导出为 PNG 字节流
        ByteArrayOutputStream().use { bs ->
            ImageIO.write(image, "png", bs)
            return MessageChain(bs.toByteArray())
        }
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(PingService::class.java)
    }
}
