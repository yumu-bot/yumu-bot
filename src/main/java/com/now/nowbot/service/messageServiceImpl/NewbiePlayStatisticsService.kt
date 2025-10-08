package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.config.NewbieConfig
import com.now.nowbot.config.Permission
import com.now.nowbot.dao.BindDao
import com.now.nowbot.model.BindUser
import com.now.nowbot.qq.event.GroupMessageEvent
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.NewbieService
import com.now.nowbot.throwable.TipsException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.text.DecimalFormat
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

@Service("NEWBIE_PLAY_STATISTICS")
//@DependsOn("newbieService")
//@ConditionalOnBean(NewbieService::class)
class NewbiePlayStatisticsService(
//    private val newbieService: NewbieService,
    private val bindDao: BindDao,
    private val newbieService: NewbieService,
    private val newbieConfig: NewbieConfig
) : MessageService<NewbiePlayStatisticsService.SearchType> {
    private val log = LoggerFactory.getLogger(NewbiePlayStatisticsService::class.java)
    private val ppFormat = DecimalFormat("#.##")

    enum class SearchType {
        DAY, HISTORY, RANK, LIST;

        companion object {
            fun fromString(str: String): SearchType? {
                return when (str) {
                    "!!pc" -> DAY
                    "!!pca" -> HISTORY
                    "!!pl" -> RANK
                    "!!all" -> LIST
                    else -> null
                }
            }
        }
    }

    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: MessageService.DataValue<SearchType>
    ): Boolean {
        val type = SearchType.fromString(messageText) ?: return false

        // 活动已停止
        if (Permission.isSuperAdmin(event.sender.id).not()) {
            val endTime = OffsetDateTime.of(2025, 3, 1, 0, 0, 0, 0, ZoneOffset.ofHours(8))
            val now = OffsetDateTime.now()

            val delta = ChronoUnit.DAYS.between(now, endTime)

            throw TipsException("活动已于 $delta 天前结束！感谢您的参与！")
        }

        data.value = type
        return true
    }

    //    @CheckPermission(isSuperAdmin = true)
    override fun handleMessage(event: MessageEvent, param: SearchType) {
        if (event !is GroupMessageEvent) return
        val gid = event.group.id
        if (gid != newbieConfig.newbieGroup && gid != newbieConfig.killerGroup) return

        val bind = bindDao.getBindFromQQ(event.sender.id, true)
        val message = when (param) {
            SearchType.DAY -> handleDay(bind)
            SearchType.HISTORY -> handleHistory(bind)
            SearchType.RANK -> handleRank(bind)
            SearchType.LIST -> if (gid == newbieConfig.killerGroup) {
                handleList()
            } else {
                return
            }
        }

        event.reply(message)
    }

    private fun handleDay(bind: BindUser): String {
        val userId = bind.userID
        val result = newbieService.getToday(userId)

        return getToday(result.name ?: bind.username, result.playCount, result.totalHit, result.pp ?: 0f)
    }

    private fun handleHistory(bind: BindUser): String {
        val userId = bind.userID
        val result = newbieService.getHistory(userId) ?: return "暂无历史数据, 统计数据会有一天的延迟"

        return getHistory(
            bind.username,
            result.playCount,
            result.totalHit,
            result.pp ?: 0f
        )
    }

    private fun handleRank(bind: BindUser): String {
        val userId = bind.userID
        val result = newbieService.getRank(userId)
        val pc = getRankString(result[0])
        val tth = getRankString(result[1])
        val pp = getRankString(result[2])
        return getRank(
            bind.username,
            pc, tth, pp
        )
    }

    private fun handleList(): String {
        return newbieService.getAlList(ppFormat)
    }

    private fun getRankString(i: Int) = if (i > 0) {
        "$i 名"
    } else {
        "未上榜"
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
        p.s. 新增PP会有一天的延迟, 在最终结算时不会受到影响.
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
        p.s. 新增PP会有一天的延迟, 在最终结算时不会受到影响.
        """.trimIndent()

    private fun getRank(
        name: String,
        pc: String,
        tth: String,
        pp: String,
    ): String = """
        特别活动数据如下:
        玩家：$name
        活动排名 PC: $pc
        活动排名 TTH: $tth
        活动排名 PP: $pp
        p.s. 活动排名查询有延迟，仅供当前参考，以最终结算为准.
        """.trimIndent()

}
