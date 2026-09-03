package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.dao.BindDao
import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.model.osu.MicroUser
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.restrict.RestrictUtils.isCommonUser
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.osuApiService.OsuUserApiService

import com.now.nowbot.throwable.botRuntimeException.IllegalStateException
import com.now.nowbot.throwable.botRuntimeException.PermissionException
import com.now.nowbot.util.DataUtil.splitString
import com.now.nowbot.util.Instruction
import org.springframework.stereotype.Service
import java.util.regex.Matcher

@Service("GET_NAME")
class GetNameService(
    private val userApiService: OsuUserApiService,
    private val bindDao: BindDao
) : MessageService<Matcher> {

    @Throws(Throwable::class) override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: DataValue<Matcher>
    ): Boolean {
        val m = Instruction.GET_NAME.matcher(messageText)
        if (m.find()) {
            data.value = m
            return true
        } else return false
    }

    @Throws(Throwable::class) override fun handleMessage(event: MessageEvent, param: Matcher): ServiceCallStatistic? {
        if (event.isCommonUser()) {
            throw PermissionException.DeniedException.BelowGroupAdministrator()
        }

        val sb = StringBuilder()

        // 使用批量获取
        val ids = if (event.hasAt()) {
            bindDao.getBindFromQQs(event.targets).map { it.userID }
        } else {
            splitString(param.group("data"), splitSpace = true)?.mapNotNull { it.toLongOrNull() }
        }

        if (ids.isNullOrEmpty()) throw IllegalStateException.Fetch("玩家编号")

        val nameMap: Map<Long, MicroUser> = userApiService.getMicroUsers(ids)
            .associateBy { it.userID }

        ids.forEach {
            val user = nameMap[it]

            sb.append(user?.username ?: "-").append(',')
        }

        event.replyAsync(sb.toString().removeSuffix(","))

        /*
        for (i in idStrs) {
            if (i.isBlank()) {
                continue
            }

            val id = try {
                i.toLong()
            } catch (e: NumberFormatException) {
                sb.append("id=").append(i).append(" can't parse").append(',')
                continue
            }

            val name = try {
                userApiService.getPlayerInfo(id).username
            } catch (e: Exception) {
                sb.append("id=").append(id).append(" not found").append(',')
                break
            }

            sb.append(name).append(',')
        }


        from.sendMessage(sb.substring(0, sb.length - 2))

         */


        return ServiceCallStatistic.builds(event, userIDs = ids)
    }
}
