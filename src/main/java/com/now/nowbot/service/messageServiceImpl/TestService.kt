package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.model.osu.OsuUser
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.osuApiService.OsuUserApiService
import com.now.nowbot.util.DataUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

@Service("TEST")
class TestService(
    private val userApiService: OsuUserApiService
) : MessageService<String> {
    override fun isHandle(
            event: MessageEvent,
            messageText: String,
            data: MessageService.DataValue<String>,
    ): Boolean {
        /*
        if (messageText.contains("!ymtest")) {
            data.value = messageText.replace("!ymtest", "")
            return true
        } else {
            return false
        }

         */

        return false

    }

    override fun handleMessage(event: MessageEvent, param: String): ServiceCallStatistic? {

        val ids = DataUtil.splitString(param)!!

        var username: List<OsuUser> = listOf()

        val thread = Thread.startVirtualThread {
            username = ids.map {
                val u = try {
                    userApiService.getOsuUser(it.toLong())
                } catch (_: Exception) {
                    OsuUser(-1)
                }

                log.info("记录用户 ${u.userID}")

                Thread.sleep(1000)
                u
            }
        }

        thread.join()

        Files.write(Path.of("D://users.csv"),
            username.joinToString(",\n") {
                "${it.userID},${
                    (it.previousNames ?: listOf()).joinToString(
                        ",",
                        "[",
                        "]"
                    )
                }"
            }.toByteArray(StandardCharsets.UTF_8)
        )

        return null
    }

    companion object {

        private val log: Logger = LoggerFactory.getLogger(TestService::class.java)

    }
}
