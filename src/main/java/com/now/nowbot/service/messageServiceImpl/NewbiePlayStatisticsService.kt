package com.now.nowbot.service.messageServiceImpl

import com.mikuac.shiro.core.BotContainer
import com.now.nowbot.aop.CheckPermission
import com.now.nowbot.newbie.mapper.NewbieService
import com.now.nowbot.qq.event.GroupMessageEvent
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.MessageService
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.context.annotation.DependsOn
import org.springframework.stereotype.Service
import java.nio.file.Files
import kotlin.io.path.Path

@Service("NIWBIE_PLAY_STATISTICS")
@DependsOn("newbieService")
@ConditionalOnBean(NewbieService::class)
class NewbiePlayStatisticsService(
    private val newbieService: NewbieService,
    private val botContainer: BotContainer,
) : MessageService<Any?> {
    private val log = LoggerFactory.getLogger(NewbiePlayStatisticsService::class.java)
    override fun isHandle(event: MessageEvent, messageText: String, data: MessageService.DataValue<Any?>): Boolean {
        if (messageText.startsWith("统计打图数据")) {
            data.value = messageText.substringAfter("统计打图数据").trim()
            return true
        }

        return false
    }

    @CheckPermission(isSuperAdmin = true)
    override fun HandleMessage(event: MessageEvent, data: Any?) {
        if (event !is GroupMessageEvent) return
        val ng = event.bot.getGroup(595985887L)
        val userIds = ng.allUser.map { it.id }
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

        event.group.sendMessage("统计完成, 文件在 /home/spring/result.csv")
    }
}