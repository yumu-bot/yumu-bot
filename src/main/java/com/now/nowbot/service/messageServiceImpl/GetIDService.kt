package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.config.Permission
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.osuApiService.OsuUserApiService
import com.now.nowbot.throwable.GeneralTipsException
import com.now.nowbot.util.AsyncMethodExecutor
import com.now.nowbot.util.DataUtil.splitString
import com.now.nowbot.util.Instruction
import io.ktor.util.collections.*
import org.springframework.stereotype.Service
import java.util.regex.Matcher

@Service("GET_ID")
class GetIDService(private val userApiService: OsuUserApiService) : MessageService<Matcher> {

    @Throws(Throwable::class) override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: DataValue<Matcher>
    ): Boolean {
        val m = Instruction.GET_ID.matcher(messageText)
        if (m.find()) {
            data.value = m
            return true
        } else return false
    }

    @Throws(Throwable::class) override fun HandleMessage(event: MessageEvent, matcher: Matcher) {
        if (Permission.isCommonUser(event)) {
            throw GeneralTipsException(GeneralTipsException.Type.G_Permission_Group)
        }

        val names: List<String>? = splitString(matcher.group("data"))

        if (names.isNullOrEmpty()) throw GeneralTipsException(GeneralTipsException.Type.G_Fetch_List)

        val sb = StringBuilder()

        val map = ConcurrentMap<String, String>(names.size)

        val actions = names.map {
            return@map AsyncMethodExecutor.Supplier<Unit> {
                val id = try {
                    userApiService.getOsuId(it).toString()
                } catch (e: Exception) {
                    "name=$it not found"
                }

                map[it] = id
            }
        }

        AsyncMethodExecutor.AsyncSupplier(actions)

        names.forEach {
            val id = map[it] ?: "unknown"

            sb.append(id).append(',')
        }

        /*

        for (name in names) {
            if (name.isBlank()) {
                continue
            }

            val id: Long

            try {
                id = userApiService.getOsuId(name)
            } catch (e: Exception) {
                sb.append("name=").append(name).append(" not found").append(',')
                continue
            }

            sb.append(id).append(',')
        }

         */

        event.reply(sb.toString().removeSuffix(","))
    }
}
