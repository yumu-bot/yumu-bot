package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.qq.tencent.TencentMessageService
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.osuApiService.OsuUserApiService
import com.now.nowbot.service.web.TeamInfo

import com.now.nowbot.throwable.botRuntimeException.IllegalArgumentException
import com.now.nowbot.throwable.botRuntimeException.IllegalStateException
import com.now.nowbot.throwable.botRuntimeException.NoSuchElementException
import com.now.nowbot.util.InstructionObject
import com.now.nowbot.util.InstructionUtil.getUserWithoutRange
import com.now.nowbot.util.DataUtil
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.OfficialInstruction
import com.now.nowbot.util.command.FLAG_ID
import com.now.nowbot.util.command.FLAG_NAME
import com.now.nowbot.util.command.FLAG_PAGE
import com.now.nowbot.util.command.REG_NUMBER
import org.springframework.stereotype.Service
import java.util.regex.Matcher

@Service("TEAM")
class TeamService(
    private val userApiService: OsuUserApiService,
    private val imageService: ImageService,
) : MessageService<TeamService.TeamParam>, TencentMessageService<TeamService.TeamParam> {

    data class TeamParam(val teamID: Int, val assumeTeam: Boolean = false, val page: Int = 1)

    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: MessageService.DataValue<TeamParam>
    ): Boolean {
        val matcher = Instruction.TEAM.matcher(messageText)
        if (!matcher.find()) {
            return false
        }

        data.value = getParam(event, matcher)

        return true
    }

    @Throws(Throwable::class)
    override fun handleMessage(event: MessageEvent, param: TeamParam): ServiceCallStatistic? {
        val team = param.getTeam()

        val image = team.getImage(param.page)

        try {
            event.reply(image)
        } catch (_: Exception) {
            throw IllegalStateException.Send("战队信息")
        }

        return ServiceCallStatistic.building(event) {
            setParam(mapOf(
                "tids" to listOf(team.id),
                "modes" to listOf(team.ruleset.modeValue)
            ))
        }
    }

    override fun accept(event: MessageEvent, messageText: String): TeamParam? {
        val matcher = OfficialInstruction.TEAM.matcher(messageText)
        if (!matcher.find()) {
            return null
        }

        return getParam(event, matcher)
    }

    override fun reply(event: MessageEvent, param: TeamParam): MessageChain? {

        return MessageChain(param.getTeam().getImage(param.page))
    }


    private fun getParam(event: MessageEvent, matcher: Matcher): TeamParam {
        val nameStr: String = (matcher.group(FLAG_NAME) ?: "").trim()
        val idStr: String = (matcher.group(FLAG_ID) ?: "").trim()
        val page = matcher.group(FLAG_PAGE)?.toIntOrNull() ?: 1
        val id: Int

        return if (idStr.isNotEmpty()) {
            id = idStr.toIntOrNull() ?: throw IllegalArgumentException.WrongException.TeamID()

            TeamParam(id, assumeTeam = true, page)
        } else if (nameStr.matches(REG_NUMBER.toRegex())) {
            id = nameStr.toIntOrNull() ?: throw IllegalArgumentException.WrongException.TeamID()

            TeamParam(id, assumeTeam = false, page)
        } else {
            val user = getUserWithoutRange(event, matcher, InstructionObject(OsuMode.DEFAULT))
            id = user.team?.id ?: throw NoSuchElementException.PlayerTeam(user.username)

            TeamParam(id, assumeTeam = true, page)
        }
    }

    private fun TeamInfo.getImage(page: Int = 1): ByteArray {
        val split = DataUtil.splitPage(
            users,
            page = page,
            maxPerPage = 48
        )

        return imageService.getPanel(mapOf("team" to this, "page" to split.second, "max_page" to split.third), "A9")
    }

    private fun TeamParam.getTeam(): TeamInfo {

        val team = try {
            userApiService.getTeamInfo(teamID)!!
        } catch (_: Exception) {
            if (assumeTeam) {
                throw NoSuchElementException.TeamID(teamID)
            } else try {
                val user = userApiService.getOsuUser(teamID.toLong())

                userApiService.getTeamInfo(user.team?.id ?: throw NoSuchElementException.PlayerTeam(user.username))
                    ?: throw NoSuchElementException.PlayerTeam(user.username)
            } catch (_: Exception) {
                throw NoSuchElementException.TeamID(teamID)
            }
        }

        return team
    }
}
