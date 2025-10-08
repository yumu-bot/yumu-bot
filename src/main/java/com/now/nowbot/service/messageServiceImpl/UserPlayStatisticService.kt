package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.dao.BindDao
import com.now.nowbot.dao.ScoreDao
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.osuApiService.OsuScoreApiService
import com.now.nowbot.util.Instruction
import org.springframework.stereotype.Service
import java.time.LocalTime
import java.time.ZoneOffset

@Service("USER_PLAY_STATISTICS")
class UserPlayStatisticService(
    val bindDao: BindDao,
    val scoreApi: OsuScoreApiService,
    val scoreDao: ScoreDao,
) : MessageService<Long> {
    val systemTimeZone: ZoneOffset = ZoneOffset.ofHours(8)
    override fun isHandle(event: MessageEvent, messageText: String, data: MessageService.DataValue<Long>): Boolean {
        val matcher = Instruction.PLAY_STATISTIC.matcher(messageText)
        if (!matcher.find()) {
            return false
        }
        data.value = event.sender.id
        return true
    }

    override fun handleMessage(event: MessageEvent, param: Long) {
        val user = bindDao.getBindFromQQ(param, true)
        scoreApi.getRecentScore(user, OsuMode.DEFAULT, 0, 999)
        val time = scoreDao.getUserAllScoreTime(user.userID)
        val intervalCount = IntArray(5) { 0 }
        var all = 0
        time.map { getPlayTimeInterval(it.withOffsetSameInstant(systemTimeZone).toLocalTime()) }
            .forEach {
                all++
                when (it) {
                    PlayTimeInterval.Early -> intervalCount[0]++
                    PlayTimeInterval.Morning -> intervalCount[1]++
                    PlayTimeInterval.Afternoon -> intervalCount[2]++
                    PlayTimeInterval.Evening -> intervalCount[3]++
                    PlayTimeInterval.Night -> intervalCount[4]++
                }
            }
        if (all < 30) {
            event.reply("根据本 bot 对你的计算: 计算个毛你都没怎么玩!")
            return
        }
        val max = intervalCount
            .mapIndexed { i, v -> i to v }
            .sortedByDescending { it.second }
            .take(3)
            .map { it.first }
        val message = when(max[0]) {
            0 -> "你是一个修仙人士"
            1 -> "好稀有, 就像早上吃早餐的人一样"
            2 -> "你居然有大把时间打 osu, 难道是大学生?"
            3 -> "我猜你是社畜!"
            else -> "恭喜, 没有特点就是你最大的特点!"
        }

        event.reply("根据本 bot 对你的计算: $message")
    }

    companion object {
        private val time1: LocalTime = LocalTime.of(2, 0)
        private val time2: LocalTime = LocalTime.of(7, 0)
        private val time3: LocalTime = LocalTime.of(11, 0)
        private val time4: LocalTime = LocalTime.of(16, 0)
        private val time5: LocalTime = LocalTime.of(20, 0)

        enum class PlayTimeInterval {
            Early, Morning, Afternoon, Evening, Night
        }

        fun getPlayTimeInterval(time: LocalTime): PlayTimeInterval {
            return when (time) {
                in time1..time2 -> PlayTimeInterval.Early
                in time2..time3 -> PlayTimeInterval.Morning
                in time3..time4 -> PlayTimeInterval.Afternoon
                in time4..time5 -> PlayTimeInterval.Evening
                else -> PlayTimeInterval.Night
            }
        }
    }
}