package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.config.Permission
import com.now.nowbot.config.PermissionParam
import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.messageServiceImpl.BanService.BanParam
import com.now.nowbot.throwable.TipsException
import com.now.nowbot.throwable.botRuntimeException.PermissionException
import com.now.nowbot.throwable.botRuntimeException.UnsupportedOperationException
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.command.FLAG_NAME
import com.now.nowbot.util.command.FLAG_QQ_GROUP
import com.now.nowbot.util.command.FLAG_QQ_ID
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

        val qq: String? = matcher.group(FLAG_QQ_ID)
        val group: String? = matcher.group(FLAG_QQ_GROUP)
        val name: String? = matcher.group(FLAG_NAME)
        val operate = matcher.group("operate")

        data.value = if (event.hasAt()) {
            BanParam(event.target, null, operate, true)
        } else if (!(qq.isNullOrBlank())) {
            BanParam(qq.toLongOrNull(), null, operate, true)
        } else if (!(group.isNullOrBlank())) {
            BanParam(group.toLongOrNull(), null, operate, false)
        } else if (!(name.isNullOrBlank())) {
            BanParam(null, name, operate, true)
        } else {
            BanParam(null, null, operate, false)
        }

        return true
    }

    @Throws(Throwable::class)
    override fun handleMessage(event: MessageEvent, param: BanParam): ServiceCallStatistic? {
        if (!Permission.isSuperAdmin(event.sender.id)) {
            throw PermissionException.DeniedException.BelowSuperAdministrator()
        }

        when (param.operate) {
            "list",
            "whitelist",
            "l",
            "w" -> sendImage(event, Permission.getWhiteList(), "白名单包含：")
            "banlist",
            "blacklist",
            "k" -> sendImage(event, Permission.getBlackList(), "黑名单包含：")
            "add",
            "a" -> {
                if (param.qq != null && param.isUser) {
                    val add = permission.addUser(param.qq, true)
                    if (add) {
                        event.reply("成功添加用户 ${param.qq} 进白名单")
                    }
                } else if (param.qq != null) {
                    val add = permission.addGroup(param.qq, true, true)
                    if (add) {
                        event.reply("成功添加群聊 ${param.qq} 进白名单")
                    }
                } else {
                    throw UnsupportedOperationException.NoQQ("add")
                }
            }
            "remove",
            "r" -> {
                if (param.qq != null && param.isUser) {
                    val remove = permission.removeUser(param.qq, true)
                    if (remove) {
                        event.reply("成功添加群聊 ${param.qq} 出白名单")
                    }
                } else if (param.qq != null) {
                    val add = permission.removeGroup(param.qq, false, true)
                    if (add) {
                        event.reply("成功移除群聊 ${param.qq} 出白名单")
                    }
                } else {
                    throw UnsupportedOperationException.NoQQ("remove")
                }
            }
            "ban",
            "b" -> {
                if (param.qq != null && param.isUser) {
                    val add = permission.addUser(param.qq, false)
                    if (add) {
                        event.reply("成功拉黑用户 ${param.qq}")
                    }
                } else if (param.qq != null) {
                    val add = permission.addGroup(param.qq, false, true)
                    if (add) {
                        event.reply("成功拉黑群聊 ${param.qq}")
                    }
                } else {
                    throw UnsupportedOperationException.NoQQ("ban")
                }
            }
            "unban",
            "u" -> {
                if (param.qq != null && param.isUser) {
                    val add = permission.removeUser(param.qq, false)
                    if (add) {
                        event.reply("成功恢复用户 ${param.qq}")
                    }
                } else if (param.qq != null) {
                    val add = permission.removeGroup(param.qq, false, true)
                    if (add) {
                        event.reply("成功恢复群聊 ${param.qq}")
                    }
                } else {
                    throw UnsupportedOperationException.NoQQ("unban")
                }
            }
            else -> throw TipsException(
                """
                请输入 super 操作！超管可用的操作有：
                
                whitelist：查询白名单
                blacklist：查询黑名单
                add：添加用户至白名单
                remove：移除用户出白名单
                ban：添加用户至黑名单
                unban：移除用户出黑名单
                """.trimIndent())
        }
        
        return ServiceCallStatistic.building(event) {
            if (param.isUser) {
                param.qq?.let { userID = it }
            } else {
                param.qq?.let { groupID = it }
            }

            setParam(mapOf(
                "operation" to param.operate.first()
            ))
        }
    }

    private fun sendImage(event: MessageEvent, param: PermissionParam, info: String) {
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
