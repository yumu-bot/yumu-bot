package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.config.Permission
import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.model.osu.LazerScore
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.util.JacksonUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.util.StopWatch
import java.nio.file.Files
import java.nio.file.Path

@Service("TEST")
class TestService(
    imageService: ImageService
): MessageService<String> {
    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: MessageService.DataValue<String>
    ): Boolean {
        if (messageText.contains("!yuumu") && Permission.isSuperAdmin(event.sender.contactID)) {
            data.value = messageText.replace("!yuumu", "")
            return true
        } else {
            return false
        }

        // return false
    }

    override fun handleMessage(event: MessageEvent, param: String): ServiceCallStatistic? {
        val sw = StopWatch()

        sw.start("json")

        val json = Files.readAllBytes(Path.of("E:/Apps/IDEA/Projects/osu-api-v2/best.json")).toString(Charsets.UTF_8)

        sw.stop()
        sw.start("parse")

        val parse = JacksonUtil.parseObjectList(json, LazerScore::class.java)

        sw.stop()



        log.info(sw.prettyPrint())

        return null
    }

    companion object {

        private val log: Logger = LoggerFactory.getLogger(TestService::class.java)

    }
}
