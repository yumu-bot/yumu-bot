package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.qq.tencent.TencentMessageService
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.messageServiceImpl.FriendService.Companion.SortDirection.*
import com.now.nowbot.service.messageServiceImpl.FriendService.Companion.SortType.*
import com.now.nowbot.service.osuApiService.OsuUserApiService
import com.now.nowbot.throwable.GeneralTipsException
import com.now.nowbot.util.CmdObject
import com.now.nowbot.util.CmdUtil.getUserWithoutRange
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.OfficialInstruction
import com.now.nowbot.util.QQMsgUtil
import org.springframework.stereotype.Service
import java.util.*

@Service("TEAM")
class TeamService(
    private val userApiService: OsuUserApiService,
    private val imageService: ImageService,
) : MessageService<TeamService.TeamParam>, TencentMessageService<TeamService.TeamParam> {

    data class TeamParam(val teamID: Int, val isInputTeam: Boolean = false)

    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: MessageService.DataValue<TeamParam>
    ): Boolean {
        val m = Instruction.TEAM.matcher(messageText)
        if (!m.find()) {
            return false
        }

        if (m.group("team")?.matches("\\d+".toRegex()) == true) {
            data.value = TeamParam(
                m.group("team")?.toIntOrNull() ?: throw GeneralTipsException(GeneralTipsException.Type.G_Exceed_Param), true) // 因为是确信用户输入的是战队的编号
        } else if (m.group("name")?.matches("\\d+".toRegex()) == true) {
            data.value = TeamParam(
                m.group("name")?.toIntOrNull() ?: throw GeneralTipsException(GeneralTipsException.Type.G_Exceed_Param), false)
        } else {
            val user = getUserWithoutRange(event, m, CmdObject(OsuMode.DEFAULT))

            data.value = TeamParam(
                user.team?.id ?: throw GeneralTipsException(GeneralTipsException.Type.G_Null_PlayerTeam, user.username), true)
        }

        return true
    }

    @Throws(Throwable::class)
    override fun HandleMessage(event: MessageEvent, param: TeamParam) {
        val team = try {
            userApiService.getTeamInfo(param.teamID)
        } catch (ignored: Exception) {
            if (param.isInputTeam) {
                throw GeneralTipsException(GeneralTipsException.Type.G_Null_Team, param.teamID.toString())
            } else try {
                userApiService.getTeamInfo(
                    userApiService.getPlayerInfo(param.teamID.toLong()).team?.id ?:
                    throw GeneralTipsException(GeneralTipsException.Type.G_Null_PlayerTeam, param.teamID.toString())
                )
            } catch (ignored2: Exception) {
                throw GeneralTipsException(GeneralTipsException.Type.G_Null_PlayerTeam, param.teamID.toString())
            }
        }

        val image = try {
            imageService.getPanel(team, "A9")
        } catch (e: Exception) {
            throw GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Render, "战队信息")
        }

        try {
            event.reply(image)
        } catch (e: Exception) {
            throw GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Send, "战队信息")
        }
    }

    override fun accept(event: MessageEvent, messageText: String): TeamParam? {
        val m = OfficialInstruction.TEAM.matcher(messageText)
        if (!m.find()) {
            return null
        }

        val user = getUserWithoutRange(event, m, CmdObject(OsuMode.DEFAULT))

        return if (m.group("team")?.matches("\\d+".toRegex()) == true) {
            TeamParam(m.group("team")?.toIntOrNull() ?: throw GeneralTipsException(GeneralTipsException.Type.G_Exceed_Param), true) // 因为是确信用户输入的是战队的编号
        } else if (m.group("name")?.matches("\\d+".toRegex()) == true) {
            TeamParam(m.group("name")?.toIntOrNull() ?: throw GeneralTipsException(GeneralTipsException.Type.G_Exceed_Param), false)
        } else {
            TeamParam(user.team?.id ?: throw GeneralTipsException(GeneralTipsException.Type.G_Null_PlayerTeam, user.username), true)
        }
    }

    override fun reply(event: MessageEvent, param: TeamParam): MessageChain? {
        val team = try {
            userApiService.getTeamInfo(param.teamID)
        } catch (ignored: Exception) {
            if (param.isInputTeam) {
                throw GeneralTipsException(GeneralTipsException.Type.G_Null_Team, param.teamID.toString())
            } else try {
                userApiService.getTeamInfo(
                    userApiService.getPlayerInfo(param.teamID.toLong()).team?.id ?:
                    throw GeneralTipsException(GeneralTipsException.Type.G_Null_PlayerTeam, param.teamID.toString())
                )
            } catch (ignored2: Exception) {
                throw GeneralTipsException(GeneralTipsException.Type.G_Null_PlayerTeam, param.teamID.toString())
            }
        }

        val image = try {
            imageService.getPanel(team, "A9")
        } catch (e: Exception) {
            throw GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Render, "战队信息")
        }

        return QQMsgUtil.getImage(image)
    }
}
