package com.now.nowbot.service.messageServiceImpl

import com.fasterxml.jackson.databind.JsonNode
import com.now.nowbot.aop.CheckPermission
import com.now.nowbot.dao.BindDao
import com.now.nowbot.model.BindUser
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.messageServiceImpl.PrivateMessageService.PMParam
import com.now.nowbot.service.osuApiService.OsuUserApiService
import com.now.nowbot.throwable.TipsException
import com.now.nowbot.util.JacksonUtil
import jakarta.annotation.Resource
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.util.*
import java.util.regex.Pattern

@Service("PRIVATE_MESSAGE")
class PrivateMessageService : MessageService<PMParam> {
    @Resource
    var userApiService: OsuUserApiService? = null

    @Resource
    var imageService: ImageService? = null

    @Resource
    var bindDao: BindDao? = null
    @Throws(Throwable::class) override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: DataValue<PMParam>
    ): Boolean {
        if (!messageText.startsWith("!testmsg")) return false
        val m = pattern.matcher(messageText)
        if (!m.matches()) return false
        val type = Type.valueOf(m.group("type"))
        var s: String
        val id: Long?
        if (Objects.isNull(m.group("id").also { s = it })) {
            id = null
        } else {
            id = s.toLong()
        }
        s = m.group("msg")

        data.value = PMParam(type, id, s)
        return true
    }

    @CheckPermission(isSuperAdmin = true) @Throws(Throwable::class) override fun HandleMessage(
        event: MessageEvent,
        param: PMParam
    ) {
        val bin = bindDao!!.getBindFromQQ(event.sender.id, true)
        val json: JsonNode
        try {
            json = getJson(param, bin)
        } catch (e: WebClientResponseException.Forbidden) {
            throw TipsException("权限不足")
        }
        event.reply(getCodeImage(JacksonUtil.objectToJsonPretty(json)))
    }

    enum class Type {
        send, get, act
    }

    @JvmRecord
    data class PMParam(val type: Type, val id: Long?, val message: String)

    @Throws(TipsException::class) private fun getJson(param: PMParam, bin: BindUser): JsonNode {
        val hasParam = Objects.isNull(param.id) || Objects.isNull(param.message)
        return when (param.type) {
            Type.send -> {
                if (hasParam) throw TipsException("参数缺失")
                userApiService!!.sendPrivateMessage(bin, param.id, param.message)
            }

            Type.get -> {
                if (hasParam) throw TipsException("参数缺失")
                userApiService!!.getPrivateMessage(bin, param.id, param.message.toLong())
            }

            Type.act -> {
                if (Objects.isNull(param.id)) {
                    userApiService!!.acknowledgmentPrivateMessageAlive(bin)
                } else {
                    userApiService!!.acknowledgmentPrivateMessageAlive(bin, param.id)
                }
            }
        }
    }

    private fun getCodeImage(code: String): ByteArray {
        val codeStr: String =
            """
                ```
                $code
                ```
            """.trimMargin()
        return imageService!!.getPanelA6(codeStr, "NO NAME")
    }

    companion object {
        private val pattern: Pattern = Pattern
            .compile("^!testmsg (?<type>send|get|act)\\s*(?<id>\\d+)?\\s*(?<msg>.*)?$")
    }
}
