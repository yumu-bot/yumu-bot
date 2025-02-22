package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.messageServiceImpl.FriendService.Companion.SortDirection.*
import com.now.nowbot.service.messageServiceImpl.FriendService.Companion.SortType.*
import com.now.nowbot.service.osuApiService.OsuUserApiService
import com.now.nowbot.util.CmdObject
import com.now.nowbot.util.CmdUtil.getUserWithOutRange
import com.now.nowbot.util.Instruction
import org.springframework.stereotype.Service
import java.util.*

@Service("TEAM")
class TeamService(
    private val userApiService: OsuUserApiService,
    private val imageService: ImageService,
) : MessageService<Int> {


    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: MessageService.DataValue<Int>
    ): Boolean {
        val m = Instruction.TEAM.matcher(messageText)
        if (!m.find()) {
            return false
        }
        val osuUser = getUserWithOutRange(event, m, CmdObject(OsuMode.DEFAULT))
        val tid = osuUser.team?.id ?: return false
        data.value = tid
        return true
    }

    @Throws(Throwable::class)
    override fun HandleMessage(event: MessageEvent, teamId: Int) {
        val teamUsers = userApiService.getTeamUsers(teamId)
        // todo
    }

}
