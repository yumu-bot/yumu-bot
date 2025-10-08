package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.dao.BindDao
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.AtMessage
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.qq.message.MessageChain.MessageChainBuilder
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.messageServiceImpl.MutualService.MutualParam
import com.now.nowbot.service.osuApiService.OsuUserApiService
import com.now.nowbot.throwable.botRuntimeException.BindException
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.QQMsgUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.*

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
        val atList = QQMsgUtil.getTypeAll(event.message, AtMessage::class.java)

        val users =
            if (atList.isEmpty().not()) {
                atList.map { this.at2Mutual(it) }
            } else if (name.isNotBlank()) {
                name.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    .map { this.name2Mutual(name) }
            } else {
                mutableListOf(qq2Mutual(event.sender.id))
            }

        data.value = users
        return true
    }

    @Throws(Throwable::class)
    override fun handleMessage(event: MessageEvent, param: List<MutualParam>) {
        try {
            event.reply(mutual2MessageChain(param)).recallIn((60 * 1000).toLong())
        } catch (e: Exception) {
            log.error("添加好友：发送失败！", e)
        }
    }

    private fun at2Mutual(at: AtMessage?): MutualParam {
        return qq2Mutual(at!!.target)
    }

    private fun qq2Mutual(qq: Long): MutualParam {
        try {
            val u = bindDao.getBindFromQQ(qq)
            return MutualParam(u.userID, qq, u.username)
        } catch (e: BindException) {
            return MutualParam(null, qq, "$qq : 未绑定或绑定状态失效！")
        }
    }

    private fun name2Mutual(name: String): MutualParam {
        try {
            val id = userApiService.getOsuID(name)
            return MutualParam(id, null, name)
        } catch (e: Exception) {
            return MutualParam(null, null, "$name : 找不到玩家或网络错误！")
        }
    }

    private fun mutual2MessageChain(users: List<MutualParam>): MessageChain {
        val sb = MessageChainBuilder()

        for (u in users) {
            if (Objects.isNull(u.uid)) {
                sb.addText("${u.name}\n")
                break
            }

            if (Objects.nonNull(u.qq)) {
                sb.addAt(u.qq!!)
                sb.addText("\n")
            }

            sb.addText("${u.name}：https://osu.ppy.sh/users/${u.uid}\n")
        }

        return sb.build()
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(MutualService::class.java)
    }
}
