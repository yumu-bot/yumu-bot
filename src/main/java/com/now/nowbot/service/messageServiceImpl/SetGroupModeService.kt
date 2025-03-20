package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.dao.BindDao
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.throwable.TipsException
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.command.FLAG_MODE
import org.springframework.stereotype.Service

@Service("SET_GROUP_MODE")
class SetGroupModeService (
    private val bindDao: BindDao,
): MessageService<String>{

    override fun isHandle(event: MessageEvent, messageText: String, data: DataValue<String>): Boolean {
        val m = Instruction.SET_GROUP_MODE.matcher(messageText)
        if (m.find()) {
            data.value = m.group(FLAG_MODE)
            return true
        } else return false
    }

    @Throws(Throwable::class)
    override fun HandleMessage(event: MessageEvent, modeStr: String) {
        val mode = OsuMode.getMode(modeStr)

        if (mode == OsuMode.DEFAULT) {
            throw TipsException("未知的游戏模式。请输入 0(osu) / 1(taiko) / 2(catch) / 3(mania)")
        }
        bindDao.saveGroupModeConfig(event.subject.id, mode)
        event.reply("已将群的默认游戏模式修改为: ${mode.fullName}")
    }
}
