package com.now.nowbot.service.MessageServiceImpl

import com.now.nowbot.config.Permission
import com.now.nowbot.config.PermissionParam
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.AtMessage
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.MessageServiceImpl.BanService.BanParam
import com.now.nowbot.throwable.GeneralTipsException
import com.now.nowbot.throwable.ServiceException.BanException
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.QQMsgUtil
import com.now.nowbot.util.command.FLAG_NAME
import com.now.nowbot.util.command.FLAG_QQ_GROUP
import com.now.nowbot.util.command.FLAG_QQ_ID
import java.util.*
import org.springframework.stereotype.Service

@Service("BAN")
class BanService(private val permission: Permission, private val imageService: ImageService) :
    MessageService<BanParam> {

    @JvmRecord
    data class BanParam(val qq: Long?, val name: String?, val operate: String, val isUser: Boolean)

    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: DataValue<BanParam>,
    ): Boolean {
        val matcher = Instruction.BAN.matcher(messageText)
        if (!matcher.find()) return false

        val at = QQMsgUtil.getType(event.message, AtMessage::class.java)

        val qq: String? = matcher.group(FLAG_QQ_ID)
        val group: String? = matcher.group(FLAG_QQ_GROUP)
        val name: String? = matcher.group(FLAG_NAME)
        val operate = matcher.group("operate")

        if (Objects.nonNull(at)) {
            data.value = BanParam(at!!.target, null, operate, true)
            return true
        }

        if (Objects.nonNull(qq)) {
            data.value = BanParam(qq!!.toLong(), null, operate, true)
            return true
        }

        if (Objects.nonNull(group)) {
            data.value = BanParam(group!!.toLong(), null, operate, false)
            return true
        }

        if (Objects.nonNull(name)) {
            data.value = BanParam(null, name, operate, true)
            return true
        }

        data.value = BanParam(null, null, operate, false)
        return true
    }

    @Throws(Throwable::class)
    override fun HandleMessage(event: MessageEvent, param: BanParam) {
        if (!Permission.isSuperAdmin(event.sender.id)) {
            throw GeneralTipsException(GeneralTipsException.Type.G_Permission_Super)
        }

        when (param.operate) {
            "list",
            "whitelist",
            "l",
            "w" -> SendImage(event, Permission.getWhiteList(), "白名单包含：")
            "banlist",
            "blacklist",
            "k" -> SendImage(event, Permission.getBlackList(), "黑名单包含：")
            "add",
            "a" -> {
                if (Objects.nonNull(param.qq) && param.isUser) {
                    val add = permission.addUser(param.qq, true)
                    if (add) {
                        event.reply("成功添加用户 ${param.qq} 进白名单")
                    }
                } else if (Objects.nonNull(param.qq)) {
                    // throw new TipsException("群组功能还在制作中");
                    val add = permission.addGroup(param.qq, true, true)
                    if (add) {
                        event.reply("成功添加群组 ${param.qq} 进白名单")
                    }
                } else {
                    throw BanException(BanException.Type.SUPER_Receive_NoQQ, "add", "add")
                }
            }
            "remove",
            "r" -> {
                if (Objects.nonNull(param.qq) && param.isUser) {
                    val remove = permission.removeUser(param.qq, true)
                    if (remove) {
                        event.reply("成功添加群组 ${param.qq} 出白名单")
                    }
                } else if (Objects.nonNull(param.qq)) {
                    // throw new TipsException("群组功能还在制作中");
                    val add = permission.removeGroup(param.qq, false, true)
                    if (add) {
                        event.reply("成功移除群组 ${param.qq} 出白名单")
                    }
                } else {
                    throw BanException(BanException.Type.SUPER_Receive_NoQQ, "remove", "remove")
                }
            }
            "ban",
            "b" -> {
                if (Objects.nonNull(param.qq) && param.isUser) {
                    val add = permission.addUser(param.qq, false)
                    if (add) {
                        event.reply("成功拉黑用户 ${param.qq}")
                    }
                } else if (Objects.nonNull(param.qq)) {
                    // throw new TipsException("群组功能还在制作中");
                    val add = permission.addGroup(param.qq, false, true)
                    if (add) {
                        event.reply("成功拉黑群组 ${param.qq}")
                    }
                } else {
                    throw BanException(BanException.Type.SUPER_Receive_NoQQ, "ban", "ban")
                }
            }
            "unban",
            "u" -> {
                if (Objects.nonNull(param.qq) && param.isUser) {
                    val add = permission.removeUser(param.qq, false)
                    if (add) {
                        event.reply("成功恢复用户 ${param.qq}")
                    }
                } else if (Objects.nonNull(param.qq)) {
                    // throw new TipsException("群组功能还在制作中");
                    val add = permission.removeGroup(param.qq, false, true)
                    if (add) {
                        event.reply("成功恢复群组 ${param.qq}")
                    }
                } else {
                    throw BanException(BanException.Type.SUPER_Receive_NoQQ, "unban", "unban")
                }
            }
            else -> throw BanException(BanException.Type.SUPER_Instruction)
        }
    }

    private fun SendImage(event: MessageEvent, param: PermissionParam, info: String) {
        val users = param.userList
        val groups = param.groupList

        val sb: StringBuilder = StringBuilder("${info}\nqq:")

        for (qq in users) {
            sb.append(qq).append("\n")
        }

        sb.append("group:").append('\n')

        for (qq in groups) {
            sb.append(qq).append("\n")
        }

        val image = imageService.getPanelAlpha(sb)
        event.reply(image)
    }
}
