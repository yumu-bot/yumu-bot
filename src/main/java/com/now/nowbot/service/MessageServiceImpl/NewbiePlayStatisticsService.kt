package com.now.nowbot.service.MessageServiceImpl

import com.mikuac.shiro.core.BotContainer
import com.now.nowbot.newbie.mapper.NewbieService
import com.now.nowbot.qq.event.GroupMessageEvent
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.MessageService
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.stereotype.Service
import java.nio.file.Files
import kotlin.io.path.Path

@Service("NIWBIE_PLAY_STATISTICS")
@ConditionalOnBean(NewbieService::class)
class NewbiePlayStatisticsService(
    private val newbieService: NewbieService,
    private val botContainer: BotContainer,
) : MessageService<Any?> {
    private val log = LoggerFactory.getLogger(NewbiePlayStatisticsService::class.java)
    @PostConstruct
    fun init() {
        log.info("===== NewbiePlayStatisticsService init success! =====")
    }

    override fun isHandle(event: MessageEvent, messageText: String, data: MessageService.DataValue<Any?>): Boolean {
        if (event.subject.id != 695600319L) return false
        if (messageText.startsWith("统计打图数据")) {
            data.value = messageText.substringAfter("统计打图数据").trim()
            return true
        }

        return false
    }

    override fun HandleMessage(event: MessageEvent, data: Any?) {
        if (event !is GroupMessageEvent) return
        val bot = botContainer.robots[1708547915] ?: throw Exception("Bot not found")
        val userIds = bot.getGroupMemberList(595985887).data.map { it.userId }.slice(500..699)
        event.group.sendMessage("正在统计打图数据，总计 ${userIds.size} 人, 请稍候")
        val fPath = Path("/home/spring/result.txt")
        val write = Files.newOutputStream(fPath)
        val buffer = write.bufferedWriter()
        buffer.write("uid,pp,playTime,playCount,tth\n")
        newbieService.countToday(userIds).forEachIndexed { i, user ->
            buffer.write("${user.id},${user.pp},${user.playTime},${user.playCount},${user.tth}")
            buffer.newLine()
        }
        buffer.flush()
        buffer.close()

        event.group.sendFile(Files.readAllBytes(fPath), "result.csv")
    }
}