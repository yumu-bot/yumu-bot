package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.dao.BindDao
import com.now.nowbot.model.osu.MicroUser
import com.now.nowbot.model.osu.OsuUser
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.qq.tencent.TencentMessageService
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.messageServiceImpl.OldAvatarService.OAParam
import com.now.nowbot.service.osuApiService.OsuUserApiService
import com.now.nowbot.throwable.botRuntimeException.IllegalArgumentException
import com.now.nowbot.throwable.botRuntimeException.IllegalStateException
import com.now.nowbot.throwable.botRuntimeException.NetworkException
import com.now.nowbot.throwable.botRuntimeException.NoSuchElementException
import com.now.nowbot.util.AsyncMethodExecutor
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.OfficialInstruction
import com.now.nowbot.util.QQMsgUtil
import com.now.nowbot.util.command.FLAG_DATA
import com.now.nowbot.util.command.FLAG_QQ_ID
import com.now.nowbot.util.command.FLAG_UID
import com.now.nowbot.util.command.REG_SEPERATOR_NO_SPACE
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Duration
import java.util.*
import java.util.concurrent.ExecutionException
import java.util.regex.Matcher

@Service("OLD_AVATAR")
class OldAvatarService(
    private val userApiService: OsuUserApiService,
    private val bindDao: BindDao,
    private val imageService: ImageService,
) : MessageService<OAParam>, TencentMessageService<OAParam> {

    @JvmRecord
    data class OAParam(
        val qq: Long?,
        val uid: Long?,
        val name: String?,
        val at: Boolean,
        val isMyself: Boolean,
    )

    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: DataValue<OAParam>,
    ): Boolean {
        val matcher = Instruction.OLD_AVATAR.matcher(messageText)
        if (!matcher.find()) return false

        data.value = getParam(event, matcher)
        return true
    }

    @Throws(Throwable::class)
    override fun HandleMessage(event: MessageEvent, param: OAParam) {
        val images = getImages(param)

        try {
            if (images.size == 1) {
                event.reply(images.first())
            } else {
                QQMsgUtil.sendImages(event, images)
            }
        } catch (e: Exception) {
            log.error("旧头像：发送失败", e)
            throw IllegalStateException.Send("官网头像")
        }
    }

    override fun accept(event: MessageEvent, messageText: String): OAParam? {
        val matcher = OfficialInstruction.OLD_AVATAR.matcher(messageText)
        if (!matcher.find()) return null

        return getParam(event, matcher)
    }

    override fun reply(event: MessageEvent, param: OAParam): MessageChain? {
        val images = getImages(param)

        try {
            return if (images.size == 1) {
                QQMsgUtil.getImage(images.first())
            } else {
                QQMsgUtil.getImages(images).first()
            }
        } catch (e: Exception) {
            log.error("旧头像：发送失败", e)
            throw IllegalStateException.Send("官网头像")
        }
    }

    private fun getParam(event: MessageEvent, matcher: Matcher): OAParam {
        val qqStr: String = matcher.group(FLAG_QQ_ID) ?: ""
        val uidStr: String = matcher.group(FLAG_UID) ?: ""
        val name: String = matcher.group(FLAG_DATA) ?: ""

        return if (event.isAt) {
            OAParam(event.target, null, null, at = true,  isMyself = false)
        } else if (qqStr.isNotBlank()) {
            OAParam(qqStr.toLong(), null, null, at = false, isMyself = false)
        } else if (uidStr.isNotBlank()) {
            OAParam(null, uidStr.toLong(), null, at = false, isMyself = false)
        } else if (name.isNotBlank()) {
            OAParam(null, null, name.trim(), at = false, isMyself = false)
        } else {
            OAParam(event.sender.id, null, null, at = false, isMyself = true)
        }
    }


    private fun parseDataString(dataStr: String?): List<MicroUser>? {
        if (dataStr.isNullOrBlank()) return null

        val strings =
            dataStr
                .trim { it <= ' ' }
                .split(REG_SEPERATOR_NO_SPACE.toRegex())
                .dropLastWhile { it.isEmpty() }
                .map { it.trim() }
                .dropWhile { it.isBlank() }
                .distinct()

        val ids = try {
            AsyncMethodExecutor.awaitCallableExecute({
                strings.map {
                    userApiService.getOsuID(it)
                }
            })
        } catch (e: ExecutionException) {
            throw IllegalArgumentException.WrongException.PlayerName()
        }

        /*
        for (s in dataStrArray) {
            if (s.isBlank()) continue

            try {
                ids.add(userApiService.getOsuID(s))
            } catch (e: NetworkException) {
                ids.add(s.toLongOrNull() ?: throw IllegalArgumentException.WrongException.PlayerName())
            }
        }

         */

        val users = userApiService.getUsers(ids)

        /*
        for (id in ids) {
            try {
                users.add(userApiService.getOsuUser(id))
            } catch (e: NetworkException) {
                try {
                    users.add(userApiService.getOsuUser(id.toString()))
                } catch (e1: NetworkException) {
                    throw NoSuchElementException.Player(id.toString())
                }
            }
        }

         */

        return users
    }

    private fun getImages(param: OAParam): List<ByteArray> {
        val user: OsuUser

        if (param.uid != null) {
            try {
                user = userApiService.getOsuUser(param.uid)
            } catch (e: Exception) {
                throw NoSuchElementException.Player(param.uid.toString())
            }
        } else if (param.qq != null) {
            user = userApiService.getOsuUser(bindDao.getBindFromQQ(param.qq))
        } else {
            val users = parseDataString(param.name)

            if (users.isNullOrEmpty()) {
                throw IllegalStateException.Fetch("玩家名")
            }

            AsyncMethodExecutor.asyncRunnableExecute {
                userApiService.asyncDownloadAvatar(users)
            }

            return try {
                AsyncMethodExecutor.awaitSupplierExecute(
                    users.map { u ->
                        AsyncMethodExecutor.Supplier {
                            imageService.getPanel(mapOf("user" to u), "Epsilon")
                        }
                    }, Duration.ofSeconds(30L + users.size / 2)
                )
            } catch (e: ExecutionException) {
                throw NetworkException.RenderModuleException.BadGateway()
            }
        }

        return listOf(imageService.getPanel(mapOf("user" to user), "Epsilon"))
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(OldAvatarService::class.java)
    }
}
