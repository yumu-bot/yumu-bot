package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.config.Permission
import com.now.nowbot.dao.BindDao
import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.model.BindUser
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.MessageService
import com.now.nowbot.throwable.botRuntimeException.BindException
import com.now.nowbot.util.Instruction
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@Service("CHECK")
class CheckService(private val bindDao: BindDao): MessageService<BindUser> {
    override fun isHandle(event: MessageEvent, messageText: String, data: MessageService.DataValue<BindUser>): Boolean {
        val matcher = Instruction.CHECK.matcher(messageText)
        if (!matcher.find()) return false

        if (!Permission.isSuperAdmin(event)) return false

        val bindUser = run {
            val qq = if (event.hasAt()) {
                event.target
            } else if (matcher.namedGroups().containsKey("qq")) {
                matcher.group("qq")?.toLongOrNull()
            } else null

            if (qq != null) {
                bindDao.getBindFromQQ(qq)
            } else {
                val name = if (matcher.namedGroups().containsKey("name")) {
                    matcher.group("name")
                } else null

                if (name.isNullOrEmpty()) {
                    bindDao.getBindFromQQ(event.sender.contactID)
                } else {
                    bindDao.getBindUser(name)
                }
            }
        }

        data.value = bindUser ?: throw BindException.NotBindException.UserNotBind()

        return true
    }

    override fun handleMessage(event: MessageEvent, param: BindUser): ServiceCallStatistic? {

        val time = Instant.ofEpochMilli(param.time ?: 0).atOffset(ZoneOffset.ofHours(8))
        val timeStr = if (time.isBefore(botCreatedTime)) {
            "无效"
        } else {
            formatter.format(time)
        }

        val qb = bindDao.getQQBindInfo(param)
        val qq = if (qb != null && qb.qq!! > 0L) qb.qq.toString() else "未知"
        val baseID = qb?.osuUser?.id ?: "未知"
        val join = qb?.osuUser?.joinDate?.format(formatter) ?: "未知"

        val available = when(param.isTokenAvailable){
            true -> "有效"
            false -> "过期，但或许有效"
            null -> "无效"
        }

        val result = """
            玩家 ${param.username} 的绑定信息：
            
            游戏 ID：${param.userID}
            基础 ID：${baseID}
            绑定 QQ：${qq}
            绑定时间：${join}
            游戏模式：${param.mode.fullName}
            
            绑定类型：${if (param.hasToken) "链接" else "玩家名"}
            令牌状态：${available}
            令牌过期时间：${timeStr}
        """.trimIndent()

        event.reply(result)

        return ServiceCallStatistic.build(event, userID = param.userID, mode = param.mode)
    }

    companion object {
        private val formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm")
        private val botCreatedTime = OffsetDateTime.of(2022, 4, 26, 0, 0, 0, 0, ZoneOffset.ofHours(8))
    }
}