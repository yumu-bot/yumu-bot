package com.now.nowbot.service.messageServiceImpl

import com.mikuac.shiro.core.BotContainer
import com.now.nowbot.dao.BindDao
import com.now.nowbot.dao.ScoreDao
import com.now.nowbot.qq.event.GroupMessageEvent
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.NewbieService
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.text.DecimalFormat

@Service("NEWBIE_PLAY_STATISTICS")
//@DependsOn("newbieService")
//@ConditionalOnBean(NewbieService::class)
class NewbiePlayStatisticsService(
//    private val newbieService: NewbieService,
    private val bindDao: BindDao,
    private val scoreDao: ScoreDao,
    private val beatmapApiService: OsuBeatmapApiService,
    private val newbieService: NewbieService,
    private val botContainer: BotContainer,
) : MessageService<NewbiePlayStatisticsService.SearchType> {
    private val log = LoggerFactory.getLogger(NewbiePlayStatisticsService::class.java)
    private val ppFormat = DecimalFormat("#.##")

    enum class SearchType {
        day, history, rank;

        companion object {
            fun fromString(str: String): SearchType? {
                return when (str) {
                    "!!pc" -> day
                    "!!pca" -> history
                    "!!pl" -> rank
                    else -> null
                }
            }
        }
    }

    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: MessageService.DataValue<SearchType?>
    ): Boolean {
        val type = SearchType.fromString(messageText) ?: return false

        data.value = type
        return true
    }

    //    @CheckPermission(isSuperAdmin = true)
    override fun HandleMessage(event: MessageEvent, data: SearchType) {
        if (event !is GroupMessageEvent) return
        val bind = bindDao.getUserFromQQ(event.sender.id, true)

        val result = newbieService.getToday(bind.osuID)
        val message = getToday(result.name ?: "???", result.playCount, result.totalHit, result.pp ?: 0f)
        event.reply(message)
    }

    private fun getToday(
        name: String,
        pc: Int,
        tth: Int,
        pp: Float,
    ): String = """
        特别活动数据如下:
        玩家：$name
        今日新增PC: $pc
        今日新增TTH: $tth
        今日新增PP: ${ppFormat.format(pp)}
        """.trimIndent()

    private fun getHistory(
        name: String,
        pc: Int,
        tth: Int,
        pp: Float,
    ): String = """
        特别活动数据如下:
        玩家：$name
        活动累计新增PC: $pc
        活动累计新增TTH: $tth
        活动累计新增PP: ${ppFormat.format(pp)}
        """.trimIndent()

    private fun getRank(
        name: String,
        pc: Int,
        tth: Int,
        pp: Int,
    ): String = """
        特别活动数据如下:
        玩家：$name
        活动排名 PC: $pc 名
        活动排名 TTH: $tth 名
        活动排名 PP: $pp 名
        """.trimIndent()

}
