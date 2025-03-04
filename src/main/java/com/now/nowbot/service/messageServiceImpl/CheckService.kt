package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.config.Permission
import com.now.nowbot.dao.BindDao
import com.now.nowbot.model.BindUser
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.MessageService
import com.now.nowbot.throwable.GeneralTipsException
import com.now.nowbot.util.Instruction
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@Service("CHECK")
class CheckService(private val bindDao: BindDao): MessageService<BindUser?> {
    override fun isHandle(event: MessageEvent, messageText: String, data: MessageService.DataValue<BindUser?>): Boolean {
        val matcher = Instruction.CHECK.matcher(messageText)
        if (!matcher.find()) return false

        if (!Permission.isSuperAdmin(event)) return false

        data.value = run {
            try {
                val qq = if (matcher.namedGroups().containsKey("qq")) {
                    matcher.group("qq")?.toLongOrNull()
                } else if (event.isAt) {
                    event.target
                } else null

                if (qq != null) {
                    return@run bindDao.getBindFromQQ(qq)
                }

                val name = if (matcher.namedGroups().containsKey("name")) {
                    matcher.group("name")
                } else null

                if (name != null) {
                    return@run bindDao.getBindUser(name)
                }

                return@run bindDao.getBindFromQQ(event.sender.id)
            } catch (e: Exception) {
                null
            }
        }

        return true
    }

    override fun HandleMessage(event: MessageEvent, param: BindUser?) {
        if (param == null) {
            throw GeneralTipsException(GeneralTipsException.Type.G_NotBind_Player)
        }

        val time = Instant.ofEpochMilli(param.time).atOffset(ZoneOffset.ofHours(8))
        val timeStr = if (time.isBefore(botCreatedTime)) {
            "无效"
        } else {
            formatter.format(time)
        }

        val qb = bindDao.getQQBindInfo(param)
        val qq = if (qb != null && qb.qq!! > 0L) qb.qq.toString() else "未知"
        val baseID = qb?.osuUser?.getID() ?: "未知"
        val join = qb?.osuUser?.joinDate?.format(formatter) ?: "未知"

        val result = """
            玩家 ${param.osuName} 的绑定信息：
            
            游戏 ID：${param.osuID}
            基础 ID：${baseID}
            绑定 QQ：${qq}
            绑定时间：${join}
            游戏模式：${param.osuMode.fullName}
            
            Oauth2 验证历史：${if (param.isAuthorized) "已验证" else "未验证"}
            令牌是否有效：${if (param.isPassed) "无效" else "有效"}
            令牌过期时间：${timeStr}
        """.trimIndent()

        event.reply(result)
    }

    companion object {
        private val formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm")
        private val botCreatedTime = OffsetDateTime.of(2022, 4, 26, 0, 0, 0, 0, ZoneOffset.ofHours(8))
    }
}