package com.now.nowbot.service.messageServiceImpl

import com.fasterxml.jackson.databind.JsonNode
import com.now.nowbot.config.NowbotConfig
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.throwable.botRuntimeException.LogException
import com.now.nowbot.util.JacksonUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern

class WikiService internal constructor() : MessageService<Matcher> {
    var log: Logger = LoggerFactory.getLogger(WikiService::class.java)

    init {
        var datestr = ""
        try {
            datestr = Files.readString(Path.of(NowbotConfig.RUN_PATH, "wiki.json"))
        } catch (e: IOException) {
            log.info("未找到wiki文件路径, 加载失败")
        }
        WIKI = JacksonUtil.jsonToObject(
            datestr,
            JsonNode::class.java
        )
    }

    override fun isHandle(event: MessageEvent, messageText: String, data: DataValue<Matcher>): Boolean {
        val m = Pattern.compile("^[!！]\\s*(?i)(ym)?(wiki)\\s*(?<key>\\s*)?").matcher(messageText)
        if (m.find()) {
            data.value = m
            return true
        } else return false
    }

    @Throws(Throwable::class) override fun HandleMessage(event: MessageEvent, param: Matcher) {
        val key = param.group("key")
        val msg = event.reply(getWiki(key))
        msg.recallIn((60 * 1000).toLong())
    }

    @Throws(IOException::class, LogException::class) fun getWiki(key: String?): String {
        val sb = StringBuilder()
        if (null == key || "null" == key || key.trim().isEmpty() || "index" == key) {
            if (WIKI == null) {
                val datestr = Files.readString(Path.of(NowbotConfig.RUN_PATH + "wiki.json"))
                WIKI = JacksonUtil.jsonToObject(
                    datestr,
                    JsonNode::class.java
                )
            }

            val dates = WIKI!!["index"]
            val it = dates.fields()
            while (it.hasNext()) {
                val m = it.next()
                sb.append(m.key).append(':').append('\n')
                if (m.value.isArray) {
                    for (i in 0..<m.value.size()) {
                        sb.append(" ").append(m.value[i].asText())
                    }
                }
                sb.append('\n')
            }
        } else {
            val uk = key.uppercase()
            val r = WIKI?.findValue(uk)?.asText() ?: throw LogException(
                "没有找到${key}"
            )
            sb.append(uk).append(':').append('\n')
            sb.append(r)
        }
        return sb.toString()
    }

    companion object {
        var WIKI: JsonNode? = null
    }
}
