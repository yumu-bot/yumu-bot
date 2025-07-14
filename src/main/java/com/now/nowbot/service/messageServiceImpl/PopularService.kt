package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.dao.BindDao
import com.now.nowbot.dao.ScoreDao
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.qq.event.GroupMessageEvent
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.MessageService
import com.now.nowbot.throwable.botRuntimeException.BindException
import com.now.nowbot.throwable.botRuntimeException.NoSuchElementException
import com.now.nowbot.throwable.botRuntimeException.UnsupportedOperationException
import com.now.nowbot.util.CmdRange
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.command.FLAG_QQ_GROUP
import com.now.nowbot.util.command.FLAG_RANGE
import com.now.nowbot.util.command.REG_HYPHEN
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import kotlin.math.max
import kotlin.math.min

@Service("POPULAR")
class PopularService(private val bindDao: BindDao, private val scoreDao: ScoreDao): MessageService<CmdRange<Long>> {

    override fun isHandle(event: MessageEvent, messageText: String, data: MessageService.DataValue<CmdRange<Long>>): Boolean {
        val matcher = Instruction.POPULAR.matcher(messageText)

        if (!matcher.find()) return false

        if (event !is GroupMessageEvent) {
            throw UnsupportedOperationException.NotGroup()
        }

        val group = matcher.group(FLAG_QQ_GROUP)?.toLongOrNull()
        val ranges = matcher.group(FLAG_RANGE)?.split(REG_HYPHEN.toRegex())

        val groupID: Long
        val start: Int
        val end: Int

        if (ranges == null) {
            if (group == null || group < 1e6) {
                groupID = event.subject.id
                start = 1
                end = (group?.toInt() ?: event.subject.id.toInt()).clamp(1, Int.MAX_VALUE)
            } else {
                groupID = group
                start = 1
                end = 7
            }
        } else if (ranges.size == 1) {
            groupID = event.subject.id
            start = 1
            end = ranges.firstOrNull()?.toIntOrNull() ?: 7
        } else {
            groupID = event.subject.id
            start = ranges.firstOrNull()?.toIntOrNull() ?: 1
            end = ranges.lastOrNull()?.toIntOrNull() ?: 7
        }

        data.value = CmdRange(groupID, start, end)

        return true
    }

    override fun HandleMessage(event: MessageEvent, param: CmdRange<Long>) {
        val me = try {
            bindDao.getBindFromQQ(event.sender.id)
        } catch (e: BindException) {
            null
        }

        val group = event.bot.groups.associateBy { it.id }[param.data!!]
            ?: throw NoSuchElementException.Group()

        val mode = OsuMode.getMode(me?.mode, bindDao.getGroupModeConfig(event))

        val qqIDs = group.allUser.map { it.id }

        // 记录的玩家
        val qqUsers = bindDao.getAllQQBindUser(qqIDs)

        // 存在的玩家
        val users = qqUsers.map { bindDao.getBindFromQQ(it.qid) }

        val now = OffsetDateTime.now()

        val before = now.minusDays(1)

        val scores = users.map {
            scoreDao.scoreRepository.getUserRankedScore(it.userID, mode.modeValue, before, now)
        }

        event.reply("""
            群聊：${group.id}
            群聊人数：${group.allUser.size}
            被记录的人数：${qqUsers.size}
            绑定人数：${users.size}
            可获取的成绩数：${scores.flatten().size}
        """.trimIndent())
    }

    companion object {

        private fun Int.clamp(min: Int = 1, max: Int = 50): Int {
            return min(max(this, min), max)
        }
    }
}