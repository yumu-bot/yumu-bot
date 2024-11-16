package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.dao.BindDao
import com.now.nowbot.model.BinUser
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.qq.tencent.TencentMessageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.osuApiService.OsuUserApiService
import com.now.nowbot.throwable.serviceException.BindException
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.OfficialInstruction
import com.now.nowbot.util.command.FLAG_MODE
import org.springframework.stereotype.Service

@Service("SET_MODE")
class SetModeService (
    private val bindDao: BindDao,
    private val osuUserApiService: OsuUserApiService,
): MessageService<String>, TencentMessageService<String> {

    override fun isHandle(event: MessageEvent, messageText: String, data: DataValue<String>): Boolean {
        val m = Instruction.SET_MODE.matcher(messageText)
        if (m.find()) {
            data.value = m.group(FLAG_MODE)
            return true
        } else return false
    }

    @Throws(Throwable::class)
    override fun HandleMessage(event: MessageEvent, modeStr: String) {
        val user = bindDao.getUserFromQQ(event.sender.id, true)
        event.reply(getReply(modeStr, user))
    }

    override fun accept(event: MessageEvent, messageText: String): String? {
        val m = OfficialInstruction.SET_MODE.matcher(messageText)
        if (m.find()) return m.group(FLAG_MODE)
        return null
    }

    @Throws(Throwable::class)
    override fun reply(event: MessageEvent, param: String): MessageChain? {
        val user = try {
            bindDao.getUserFromOsuid(-event.sender.id)
        } catch (e: BindException) {
            val userInfo = osuUserApiService.getPlayerInfo(-event.sender.id)
            val binUser = BinUser()
            with(binUser) {
                osuID = userInfo.id
                osuName = userInfo.username
                osuMode = userInfo.defaultOsuMode
            }
            bindDao.saveBind(binUser)
        }

        return getReply(param, user)
    }

    fun getReply(modeStr: String?, user: BinUser): MessageChain {
        val mode = OsuMode.getMode(modeStr)
        if (mode == OsuMode.DEFAULT) {
            return MessageChain("未知的格式,修改请使用0(osu),1(taiko),2(catch),3(mania)")
        }
        user.osuMode = mode
        bindDao.updateMod(user.osuID, mode)
        return MessageChain("已修改主模式为: " + mode.getName())
    }
}
