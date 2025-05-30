package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.dao.BindDao
import com.now.nowbot.model.BindUser
import com.now.nowbot.model.json.OsuUser
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.messageServiceImpl.OldAvatarService.OAParam
import com.now.nowbot.service.osuApiService.OsuUserApiService
import com.now.nowbot.throwable.GeneralTipsException
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.QQMsgUtil
import com.now.nowbot.util.command.FLAG_DATA
import com.now.nowbot.util.command.FLAG_QQ_ID
import com.now.nowbot.util.command.FLAG_UID
import com.now.nowbot.util.command.REG_SEPERATOR_NO_SPACE
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.util.*

@Service("OLD_AVATAR")
class OldAvatarService(
    private val userApiService: OsuUserApiService,
    private val bindDao: BindDao,
    private val imageService: ImageService,
) : MessageService<OAParam> {

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

        val qqStr: String = matcher.group(FLAG_QQ_ID) ?: ""
        val uidStr: String = matcher.group(FLAG_UID) ?: ""
        val name: String = matcher.group(FLAG_DATA) ?: ""

        if (event.isAt) {
            data.value = OAParam(event.target, null, null, at = true,  isMyself = false)
            return true
        } else if (qqStr.isNotBlank()) {
            data.value = OAParam(qqStr.toLong(), null, null, at = false, isMyself = false)
            return true
        }

        if (uidStr.isNotBlank()) {
            data.value = OAParam(null, uidStr.toLong(), null, at = false, isMyself = false)
            return true
        } else if (name.isNotBlank()) {
            data.value = OAParam(null, null, name.trim { it <= ' ' }, at = false, isMyself = false)
            return true
        } else {
            data.value = OAParam(event.sender.id, null, null, at = false, isMyself = true)
            return true
        }
    }

    @Throws(Throwable::class)
    override fun HandleMessage(event: MessageEvent, param: OAParam) {
        val user: OsuUser

        if (param.uid != null) {
            try {
                user = userApiService.getOsuUser(param.uid)
            } catch (e: Exception) {
                throw GeneralTipsException(GeneralTipsException.Type.G_Null_Player, param.uid)
            }
        } else if (param.qq != null) {
            val bindUser: BindUser
            try {
                bindUser = bindDao.getBindFromQQ(param.qq)
            } catch (e: Exception) {
                if (param.isMyself) {
                    throw GeneralTipsException(GeneralTipsException.Type.G_TokenExpired_Me)
                } else {
                    throw GeneralTipsException(GeneralTipsException.Type.G_TokenExpired_Player)
                }
            }

            try {
                user = userApiService.getOsuUser(bindUser)
            } catch (e: WebClientResponseException) {
                throw GeneralTipsException(GeneralTipsException.Type.G_Null_Player, bindUser.username)
            } catch (e: Exception) {
                log.error("旧头像：获取玩家信息失败: ", e)
                throw GeneralTipsException(GeneralTipsException.Type.G_Fetch_PlayerInfo)
            }
        } else {
            val users = parseDataString(param.name)

            if (users.isNullOrEmpty())
                throw GeneralTipsException(GeneralTipsException.Type.G_Fetch_List)

            val images = ArrayList<ByteArray>(users.size)

            try {
                for (u in users) {
                    images.add(imageService.getPanel(mapOf("user" to u), "Epsilon"))
                }

                QQMsgUtil.sendImages(event, images)
                return
            } catch (e: Exception) {
                log.error("旧头像：发送失败", e)
                throw GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Send, "官网头像")
            }
        }

        try {
            val image = imageService.getPanel(mapOf("user" to user), "Epsilon")
            event.reply(image)
        } catch (e: Exception) {
            log.error("旧头像：发送失败", e)
            throw GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Send, "官网头像")
        }
    }

    @Throws(GeneralTipsException::class)
    private fun parseDataString(dataStr: String?): List<OsuUser?>? {
        if (dataStr.isNullOrBlank()) return null

        val dataStrArray =
            dataStr
                .trim { it <= ' ' }
                .split(REG_SEPERATOR_NO_SPACE.toRegex())
                .dropLastWhile { it.isEmpty() }
                .stream()
                .map(String::trim)
                .distinct()
                .toList()

        val ids = mutableListOf<Long>()
        val users = mutableListOf<OsuUser>()

        for (s in dataStrArray) {
            if (s.isNullOrBlank()) continue

            try {
                ids.add(userApiService.getOsuID(s))
            } catch (e: WebClientResponseException) {
                try {
                    ids.add(s.toLong())
                } catch (e1: NumberFormatException) {
                    throw GeneralTipsException(GeneralTipsException.Type.G_Null_UserName)
                }
            }
        }

        for (id in ids) {
            try {
                users.add(userApiService.getOsuUser(id))
            } catch (e: WebClientResponseException) {
                try {
                    users.add(userApiService.getOsuUser(id.toString()))
                } catch (e1: WebClientResponseException) {
                    throw GeneralTipsException(GeneralTipsException.Type.G_Null_Player, id)
                }
            }
        }

        return users
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(OldAvatarService::class.java)
    }
}
