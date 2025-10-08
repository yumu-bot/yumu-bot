package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.qq.tencent.TencentMessageService
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.osuApiService.OsuUserApiService

import com.now.nowbot.throwable.botRuntimeException.IllegalArgumentException
import com.now.nowbot.throwable.botRuntimeException.IllegalStateException
import com.now.nowbot.throwable.botRuntimeException.NoSuchElementException
import com.now.nowbot.util.CmdObject
import com.now.nowbot.util.CmdUtil.getUserWithoutRange
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.OfficialInstruction
import org.springframework.stereotype.Service

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
                m.group("team")?.toIntOrNull() ?: throw IllegalArgumentException.WrongException.TeamID(), true) // 因为是确信用户输入的是战队的编号
        } else if (m.group("name")?.matches("\\d+".toRegex()) == true) {
            data.value = TeamParam(
                m.group("name")?.toIntOrNull() ?: throw IllegalArgumentException.WrongException.TeamID(), false)
        } else {
            val user = getUserWithoutRange(event, m, CmdObject(OsuMode.DEFAULT))

            data.value = TeamParam(
                user.team?.id ?: throw NoSuchElementException.PlayerTeam(user.username), true)
        }

        return true
    }

    @Throws(Throwable::class)
    override fun handleMessage(event: MessageEvent, param: TeamParam): ServiceCallStatistic? {
        val team = try {
            userApiService.getTeamInfo(param.teamID)
        } catch (ignored: Exception) {
            if (param.isInputTeam) {
                throw NoSuchElementException.TeamID(param.teamID)
            } else try {
                userApiService.getTeamInfo(
                    userApiService.getOsuUser(param.teamID.toLong()).team?.id ?:
                    throw NoSuchElementException.TeamID(param.teamID)
                )
            } catch (ignored2: Exception) {
                throw NoSuchElementException.TeamID(param.teamID)
            }
        }

        val image = imageService.getPanel(team, "A9")

        try {
            event.reply(image)
        } catch (e: Exception) {
            throw IllegalStateException.Send("战队信息")
        }

        return ServiceCallStatistic.building(event) {
            setParam(mapOf(
                "tids" to listOf(team!!.id),
                "modes" to listOf(team.ruleset.modeValue)
            ))
        }
    }

    override fun accept(event: MessageEvent, messageText: String): TeamParam? {
        val m = OfficialInstruction.TEAM.matcher(messageText)
        if (!m.find()) {
            return null
        }

        val user = getUserWithoutRange(event, m, CmdObject(OsuMode.DEFAULT))

        return if (m.group("team")?.matches("\\d+".toRegex()) == true) {
            TeamParam(m.group("team")?.toIntOrNull() ?: throw IllegalArgumentException.WrongException.Range(), true) // 因为是确信用户输入的是战队的编号
        } else if (m.group("name")?.matches("\\d+".toRegex()) == true) {
            TeamParam(m.group("name")?.toIntOrNull() ?: throw IllegalArgumentException.WrongException.Range(), false)
        } else {
            TeamParam(user.team?.id ?: throw NoSuchElementException.PlayerTeam(user.username), true)
        }
    }

    override fun reply(event: MessageEvent, param: TeamParam): MessageChain? {
        val team = try {
            userApiService.getTeamInfo(param.teamID)
        } catch (ignored: Exception) {
            if (param.isInputTeam) {
                throw NoSuchElementException.TeamID(param.teamID)
            } else try {
                val u = userApiService.getOsuUser(param.teamID.toLong())

                if (u.team == null) {
                    throw NoSuchElementException.PlayerTeam(u.username)
                }

                userApiService.getTeamInfo(u.team!!.id)
            } catch (ignored2: Exception) {
                throw NoSuchElementException.TeamID(param.teamID)
            }
        }

        val image = imageService.getPanel(team, "A9")

        return MessageChain(image)
    }
}
