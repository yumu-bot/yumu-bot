package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.config.Permission
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.osuApiService.OsuUserApiService
import com.now.nowbot.throwable.GeneralTipsException
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

    @Throws(Throwable::class) override fun HandleMessage(event: MessageEvent, matcher: Matcher) {
        if (Permission.isCommonUser(event)) {
            throw GeneralTipsException(GeneralTipsException.Type.G_Permission_Group)
        }

        val ids: List<String>? = splitString(matcher.group("data"))
        if (ids.isNullOrEmpty()) throw GeneralTipsException(GeneralTipsException.Type.G_Fetch_List)

        val sb = StringBuilder()

        // 使用批量获取
        val chunk = ids.mapNotNull { it.toLongOrNull() }.chunked(50)
        val names = chunk.map { userApiService.getUsers(it) }.flatten()

        names.forEach {
            sb.append(it.userName).append(',').append(' ')
        }

        event.reply(sb.substring(0, sb.length - 2))

        /*
        for (i in idStrs) {
            if (i.isBlank()) {
                continue
            }

            val id = try {
                i.toLong()
            } catch (e: NumberFormatException) {
                sb.append("id=").append(i).append(" can't parse").append(',').append(' ')
                continue
            }

            val name = try {
                userApiService.getPlayerInfo(id).username
            } catch (e: Exception) {
                sb.append("id=").append(id).append(" not found").append(',').append(' ')
                break
            }

            sb.append(name).append(',').append(' ')
        }


        from.sendMessage(sb.substring(0, sb.length - 2))

         */
    }
}
