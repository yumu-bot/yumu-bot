package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.config.Permission
import com.now.nowbot.dao.BindDao
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.osuApiService.OsuUserApiService
import com.now.nowbot.throwable.GeneralTipsException
import com.now.nowbot.throwable.botRuntimeException.BindException
import com.now.nowbot.util.AsyncMethodExecutor
import com.now.nowbot.util.DataUtil.splitString
import com.now.nowbot.util.Instruction
import io.ktor.util.collections.*
import org.springframework.stereotype.Service
import kotlin.jvm.optionals.getOrNull

@Service("GET_ID")
class GetIDService(private val userApiService: OsuUserApiService, private val bindDao: BindDao) : MessageService<List<String>> {

    @Throws(Throwable::class) override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: DataValue<List<String>>
    ): Boolean {
        val m = Instruction.GET_ID.matcher(messageText)
        if (m.find()) {

            val str: String? = m.group("data")

            val names = if (event.isAt) {
                val b = bindDao.getQQLiteFromQQ(event.target).getOrNull() ?: throw BindException.NotBindException.UserNotBindException()

                event.reply(b.osuUser.osuID.toString())
                return false
            } else {
                splitString(str)
            }

            data.value = names ?: throw GeneralTipsException(GeneralTipsException.Type.G_Fetch_List)
            return true
        } else return false
    }

    @Throws(Throwable::class) override fun HandleMessage(event: MessageEvent, param: List<String>) {
        if (Permission.isCommonUser(event)) {
            throw GeneralTipsException(GeneralTipsException.Type.G_Permission_Group)
        }

        val sb = StringBuilder()

        val actions = param.map {
            return@map AsyncMethodExecutor.Supplier<Pair<String, Long>> {
                return@Supplier try {
                    it to userApiService.getOsuID(it)
                } catch (e: Exception) {
                    it to -1L
                }
            }
        }

        val ids = AsyncMethodExecutor.awaitSupplierExecute(actions)
            .filterNotNull()
            .filter { it.second > 0L }
            .toMap()

        param.forEach {
            val id = ids[it]

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
