package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.config.Permission
import com.now.nowbot.model.osu.MicroUser
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.osuApiService.OsuUserApiService
import com.now.nowbot.throwable.GeneralTipsException
import com.now.nowbot.throwable.botRuntimeException.PermissionException
import com.now.nowbot.util.DataUtil.splitString
import com.now.nowbot.util.Instruction
import org.springframework.stereotype.Service
import java.util.regex.Matcher

@Service("GET_NAME")
class GetNameService(private val userApiService: OsuUserApiService) : MessageService<Matcher> {

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

    @Throws(Throwable::class) override fun HandleMessage(event: MessageEvent, param: Matcher) {
        if (Permission.isCommonUser(event)) {
            throw PermissionException.DeniedException.BelowGroupAdministrator()
        }

        val idStr: List<String>? = splitString(param.group("data"), splitSpace = true)
        if (idStr.isNullOrEmpty()) throw GeneralTipsException(GeneralTipsException.Type.G_Fetch_List)

        val sb = StringBuilder()

        // 使用批量获取
        val ids = idStr.mapNotNull { it.toLongOrNull() }

        val nameMap: Map<Long, MicroUser> = ids.chunked(50).map { userApiService.getUsers(it) }.flatten().associateBy { it.userID }

        ids.forEach {
            val user = nameMap[it]

            sb.append(user?.userName ?: "-").append(',')
        }

        event.reply(sb.toString().removeSuffix(","))

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
    }
}
