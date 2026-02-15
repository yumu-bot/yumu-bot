package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.dao.BindDao
import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.osu.OsuUser
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.messageServiceImpl.OldAvatarService.OAParam
import com.now.nowbot.service.sbApiService.SBUserApiService
import com.now.nowbot.throwable.botRuntimeException.IllegalStateException
import com.now.nowbot.throwable.botRuntimeException.NetworkException
import com.now.nowbot.throwable.botRuntimeException.NoSuchElementException
import com.now.nowbot.util.AsyncMethodExecutor
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.QQMsgUtil
import com.now.nowbot.util.command.FLAG_DATA
import com.now.nowbot.util.command.FLAG_MODE
import com.now.nowbot.util.command.FLAG_QQ_ID
import com.now.nowbot.util.command.FLAG_UID
import com.now.nowbot.util.command.REG_SEPERATOR_NO_SPACE
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.regex.Matcher
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@Service("SB_OLD_AVATAR")
class SBOldAvatarService(
    private val userApiService: SBUserApiService,
    private val bindDao: BindDao,
    private val imageService: ImageService,
) : MessageService<OAParam> {

    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: DataValue<OAParam>,
    ): Boolean {
        val m1 = Instruction.SB_OLD_AVATAR.matcher(messageText)
        val m2 = Instruction.SB_OLD_AVATAR_CARD.matcher(messageText)

        val matcher: Matcher
        val version: Int

        if (m1.find()) {
            matcher = m1
            version = 1
        } else if (m2.find()) {
            matcher = m2
            version = 2
        } else return false

        data.value = getParam(event, matcher, version)
        return true
    }

    @Throws(Throwable::class)
    override fun handleMessage(event: MessageEvent, param: OAParam): ServiceCallStatistic? {
        val users = getUsers(param)
        val images = getImages(users, param.version)

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

        return ServiceCallStatistic.builds(event, userIDs = users.map { it.userID })
    }

    private fun getParam(event: MessageEvent, matcher: Matcher, version: Int = 1): OAParam {
        val qqStr: String = matcher.group(FLAG_QQ_ID) ?: ""
        val uidStr: String = matcher.group(FLAG_UID) ?: ""
        val name: String = matcher.group(FLAG_DATA) ?: ""
        val mode = OsuMode.getMode(matcher.group(FLAG_MODE), bindDao.getGroupModeConfig(event))

        return if (event.hasAt()) {
            OAParam(event.target, null, null, at = true, isMyself = false, mode = mode, version = version)
        } else if (qqStr.isNotBlank()) {
            OAParam(qqStr.toLongOrNull(), null, null, at = false, isMyself = false, mode = mode, version = version)
        } else if (uidStr.isNotBlank()) {
            OAParam(null, uidStr.toLongOrNull(), null, at = false, isMyself = false, mode = mode, version = version)
        } else if (name.isNotBlank()) {
            OAParam(null, null, name.trim(), at = false, isMyself = false, mode = mode, version = version)
        } else {
            OAParam(event.sender.contactID, null, null, at = false, isMyself = true, mode = mode, version = version)
        }
    }


    private fun parseDataString(dataStr: String?, mode: OsuMode): List<OsuUser> {
        if (dataStr.isNullOrBlank()) return emptyList()

        val strings =
            dataStr
                .trim()
                .split(REG_SEPERATOR_NO_SPACE.toRegex())
                .dropLastWhile { it.isEmpty() }
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinct()

        return if (strings.size == 1) {
            listOf(userApiService.getUser(strings.first())?.toOsuUser(mode)
                ?: throw NoSuchElementException.Player(strings.first()))
        } else {
            AsyncMethodExecutor.awaitCallableExecute({
                strings.mapNotNull { name ->
                    userApiService.getUser(name)?.toOsuUser(mode)
                }
            })
        }
    }

    private fun getUsers(param: OAParam): List<OsuUser> {
        val user: OsuUser

        if (param.uid != null) {
            user = userApiService.getUser(param.uid)?.toOsuUser(param.mode)
                ?: throw NoSuchElementException.Player(param.uid.toString())
        } else if (param.qq != null) {
            val bind = bindDao.getSBBindFromQQ(param.qq, param.isMyself)

            user = userApiService.getUser(bind.userID)?.toOsuUser(OsuMode.getMode(param.mode, bind.mode))
                ?: throw NoSuchElementException.Player(bind.userID.toString())
        } else {
            val users = parseDataString(param.name, param.mode)

            if (users.isEmpty()) {
                throw IllegalStateException.Fetch("玩家名")
            }

            return users
        }

        return listOf(user)
    }

    private fun getImages(users: List<OsuUser>, version: Int = 1): List<ByteArray> {
        val panel = when (version) {
            2 -> "Epsilon2"
            else -> "Epsilon"
        }

        return if (users.size > 1) {
            try {
                AsyncMethodExecutor.awaitCallableExecute(
                    users.map { u ->
                        Callable {
                            imageService.getPanel(mapOf("user" to u), panel)
                        }
                    }, (30L + users.size / 2).toDuration(DurationUnit.SECONDS)
                )
            } catch (_: ExecutionException) {
                throw NetworkException.RenderModuleException.BadGateway()
            }
        } else {
            listOf(imageService.getPanel(mapOf("user" to users.first()), panel))
        }
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(OldAvatarService::class.java)
    }
}
