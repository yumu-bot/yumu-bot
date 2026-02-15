package com.now.nowbot.service.messageServiceImpl

import com.fasterxml.jackson.databind.JsonNode
import com.now.nowbot.aop.CheckPermission
import com.now.nowbot.dao.BindDao
import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.model.BindUser
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.messageServiceImpl.PrivateMessageService.PMParam
import com.now.nowbot.service.osuApiService.OsuUserApiService
import com.now.nowbot.throwable.TipsException
import com.now.nowbot.util.JacksonUtil
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.util.*
import java.util.regex.Pattern

@Service("PRIVATE_MESSAGE")
class PrivateMessageService(private val userApiService: OsuUserApiService, private val imageService: ImageService, private val bindDao: BindDao) : MessageService<PMParam> {
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

    @CheckPermission(isSuperAdmin = true) @Throws(Throwable::class) override fun handleMessage(
        event: MessageEvent,
        param: PMParam
    ): ServiceCallStatistic? {
        val bindUser = bindDao.getBindFromQQ(event.sender.contactID, true)
        val json: JsonNode = try {
            getJson(param, bindUser)
        } catch (_: WebClientResponseException.Forbidden) {
            throw TipsException("权限不足")
        }
        event.reply(getCodeImage(JacksonUtil.objectToJsonPretty(json)))
        return ServiceCallStatistic.building(event)
    }

    enum class Type {
        SEND, GET, ACT
    }

    @JvmRecord
    data class PMParam(val type: Type, val id: Long?, val message: String)

    @Throws(TipsException::class) private fun getJson(param: PMParam, bin: BindUser): JsonNode {
        val noParam = param.id == null
        return when (param.type) {
            Type.SEND -> {
                if (noParam) throw TipsException("参数缺失")
                userApiService.sendPrivateMessage(bin, param.id, param.message)
            }

            Type.GET -> {
                if (noParam) throw TipsException("参数缺失")
                userApiService.getPrivateMessage(bin, param.id, param.message.toLong())
            }

            Type.ACT -> {
                if (noParam) {
                    userApiService.acknowledgmentPrivateMessageAlive(bin)
                } else {
                    userApiService.acknowledgmentPrivateMessageAlive(bin, param.id)
                }
            }
        }
    }

    private fun getCodeImage(code: String): ByteArray {
        val codeStr: String = """
            ```
            $code
            ```
            """.trimIndent()
        return imageService.getPanelA6(codeStr, "NO NAME")
    }

    companion object {
        private val pattern: Pattern = Pattern
            .compile("^!testmsg (?<type>send|get|act)\\s*(?<id>\\d+)?\\s*(?<msg>.*)?$")
    }
}
