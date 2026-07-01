package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.dao.BindDao
import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.qq.message.MessageChain.MessageChainBuilder
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.messageServiceImpl.MutualService.MutualParam
import com.now.nowbot.service.osuApiService.OsuUserApiService
import com.now.nowbot.util.Instruction
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import kotlin.time.Duration.Companion.seconds

@Service("MUTUAL")
class MutualService(private val userApiService: OsuUserApiService, private val bindDao: BindDao) :
    MessageService<List<MutualParam>> {

    @JvmRecord data class MutualParam(val uid: Long?, val qq: Long?, val name: String)

    @Throws(Throwable::class)
    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: DataValue<List<MutualParam>>,
    ): Boolean {
        val m = Instruction.MUTUAL.matcher(messageText)
        if (!m.find()) return false

        val name = m.group("names") ?: ""

        val users =
            if (event.hasAt()) {
                event.targets.map { qq2Mutual(it) }
            } else if (name.isNotBlank()) {
                name.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    .map { this.name2Mutual(name) }
            } else {
                mutableListOf(qq2Mutual(event.sender.contactID))
            }

        data.value = users
        return true
    }

    @Throws(Throwable::class)
    override fun handleMessage(event: MessageEvent, param: List<MutualParam>): ServiceCallStatistic? {
        try {
            event.replyAndRecallAsync(mutual2MessageChain(param), 60.seconds)
        } catch (e: Exception) {
            log.error("添加好友：发送失败！", e)
        }

        return ServiceCallStatistic.builds(event, userIDs = param.mapNotNull { it.uid }.ifEmpty { null })
    }

    private fun qq2Mutual(qq: Long): MutualParam {
        val u = bindDao.getBindFromQQOrNull(qq) ?: return MutualParam(null, qq, "$qq: 未绑定或绑定状态失效！")
        return MutualParam(u.userID, qq, u.username)
    }

    private fun name2Mutual(name: String): MutualParam {
        try {
            val id = bindDao.getOsuID(name) ?: userApiService.getOsuID(name)
            return MutualParam(id, null, name)
        } catch (_: Exception) {
            return MutualParam(null, null, "$name: 找不到玩家或网络错误！")
        }
    }

    private fun mutual2MessageChain(users: List<MutualParam>): MessageChain {
        val sb = MessageChainBuilder()

        for ((index, u) in users.withIndex()) {
            val isLast = index == users.lastIndex

            if (u.uid == null) {
                sb.addText(u.name)
                if (!isLast) sb.addText("\n")
                break
            }

            if (u.qq != null) {
                sb.addAt(u.qq)
                sb.addText("\n")
            }

            sb.addText("${u.name}：https://osu.ppy.sh/users/${u.uid}")

            if (!isLast) {
                sb.addText("\n")
            }
        }

        return sb.build()
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(MutualService::class.java)
    }
}
