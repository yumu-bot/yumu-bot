package com.now.nowbot.service.messageServiceImpl

import com.mikuac.shiro.core.BotContainer
import com.now.nowbot.aop.CheckPermission
import com.now.nowbot.dao.BindDao
import com.now.nowbot.dao.ScoreDao
import com.now.nowbot.qq.event.GroupMessageEvent
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service("NIWBIE_PLAY_STATISTICS")
//@DependsOn("newbieService")
//@ConditionalOnBean(NewbieService::class)
class NewbiePlayStatisticsService(
//    private val newbieService: NewbieService,
    private val bindDao: BindDao,
    private val scoreDao: ScoreDao,
    private val beatmapApiService: OsuBeatmapApiService,
    private val botContainer: BotContainer,
) : MessageService<Any?> {
    private val log = LoggerFactory.getLogger(NewbiePlayStatisticsService::class.java)
    override fun isHandle(event: MessageEvent, messageText: String, data: MessageService.DataValue<Any?>): Boolean {
        if (messageText.startsWith("!!pc")) {
            data.value = "pc"
            return true
        }
        return false
    }

    @CheckPermission(isSuperAdmin = true)
    override fun HandleMessage(event: MessageEvent, data: Any?) {
        if (event !is GroupMessageEvent) return
        val bind = bindDao.getUserFromQQ(event.sender.id, true)
//        val ng = event.bot.getGroup(231094840L)
//        val userQQs = ng.allUser.map { it.id }
//        val allBindUser = bindDao.getAllQQBindUser(userQQs)
        val date = LocalDate.now()
        val result = scoreDao.getAllRankedScore(bind.osuID, date, beatmapApiService)
        val message = """
            统计结果:
            今日打图有效计入 ${result.playCount} pc, ${result.totalHit} tth, ${result.playTime} 秒
            """.trimIndent()
        event.reply(message)
    }
}