package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.config.Permission
import com.now.nowbot.permission.PermissionController
import com.now.nowbot.permission.PermissionImplement
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.messageServiceImpl.ServiceSwitchService.SwitchParam
import com.now.nowbot.throwable.GeneralTipsException
import com.now.nowbot.throwable.serviceException.ServiceSwitchException
import com.now.nowbot.throwable.TipsRuntimeException
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.command.FLAG_QQ_GROUP
import java.util.*
import org.springframework.stereotype.Service
import org.springframework.util.StringUtils
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.reactive.function.client.WebClientResponseException

@Service(ServiceSwitchService.SWITCH_SERVICE_NAME)
class ServiceSwitchService(
    val controller: PermissionController,
    val imageService: ImageService,
) : MessageService<SwitchParam> {
    data class SwitchParam(val groupID: Long, val serviceName: String?, val operation: Operation)

    enum class Operation {
        REVIEW,
        ON,
        OFF,
    }

    @Throws(Throwable::class)
    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: DataValue<SwitchParam>
    ): Boolean {
        val m = Instruction.SERVICE_SWITCH.matcher(messageText)
        if (!m.find()) {
            return false
        }

        if (!Permission.isSuperAdmin(event.sender.id)) {
            throw GeneralTipsException(GeneralTipsException.Type.G_Permission_Super)
        }

        val service = m.group("service") ?: ""
        val operate = m.group("operate") ?: ""

        // var o = Pattern.compile("(black|white)?list|on|off|start|close|[bkwlofsc]+");
        val groupStr: String = m.group(FLAG_QQ_GROUP) ?: ""

        if (StringUtils.hasText(service)) {
            if (StringUtils.hasText(operate)) {
                if (StringUtils.hasText(groupStr)) {
                    data.setValue(
                        SwitchParam(
                            groupStr.toLong(),
                            service.uppercase(Locale.getDefault()),
                            getOperation(operate)
                        )
                    )
                } else {
                    data.setValue(
                        SwitchParam(
                            -1L,
                            service.uppercase(Locale.getDefault()),
                            getOperation(operate)
                        )
                    )
                }
            } else {
                if (StringUtils.hasText(groupStr)) {
                    throw ServiceSwitchException(ServiceSwitchException.Type.SW_Parameter_OnlyGroup)
                } else {
                    val op = getOperation(service.uppercase(Locale.getDefault()))
                    if (op != Operation.REVIEW) {
                        throw ServiceSwitchException(ServiceSwitchException.Type.SW_Service_Missing)
                    }
                    data.setValue(SwitchParam(-1L, null, Operation.REVIEW))
                }
            }
        } else {
            if (StringUtils.hasText(groupStr)) {
                throw ServiceSwitchException(ServiceSwitchException.Type.SW_Parameter_OnlyGroup)
            } else {
                throw ServiceSwitchException(ServiceSwitchException.Type.SW_Instructions)
            }
        }

        return true
    }

    // @CheckPermission(isSuperAdmin = true)
    @Throws(Throwable::class)
    override fun HandleMessage(event: MessageEvent, param: SwitchParam) {
        val from = event.subject
        val service = param.serviceName
        val group = param.groupID

        when (param.operation) {
            Operation.ON -> {
                try {
                    if (group == -1L) {
                        controller.serviceSwitch(service, true)
                        //                        Permission.openService(service);
                        from.sendMessage("已启动 ${service} 服务")
                    } else if (group == 0L) {
                        // 这里要放对所有群聊的操作
                        // 貌似没这功能
                        //                        permission.removeGroupAll(service, true);
                        //                        from.sendMessage(STR."已全面清除 \{service} 服务的禁止状态");
                        from.sendMessage("已全面解禁 ${service} 服务（并未修好）")
                    } else {
                        controller.unblockGroup(service, group)
                        //                        permission.removeGroup(service, group, true,
                        // false);
                        from.sendMessage("已解禁群聊 ${group} 的 ${service} 服务")
                    }
                } catch (e: TipsRuntimeException) {
                    throw ServiceSwitchException(
                        ServiceSwitchException.Type.SW_Service_RemoveNotExists, service
                    )
                } catch (e: RuntimeException) {
                    throw ServiceSwitchException(
                        ServiceSwitchException.Type.SW_Service_NotFound, service
                    )
                }
            }

            Operation.OFF -> {
                try {
                    if (group == -1L) {
                        controller.serviceSwitch(service, false)
                        from.sendMessage("已关闭 ${service} 服务")
                    } else if (group == 0L) {
                        //                        permission.removeGroupAll(service, true);
                        //                        from.sendMessage(STR."已全面清除 \{service} 服务的禁止状态");
                        from.sendMessage("已全面禁止 ${service} 服务（并未修好）")
                    } else {
                        controller.unblockGroup(service, group)
                        //                        permission.addGroup(service, group, true, false);
                        from.sendMessage("已禁止群聊 ${group} 的 ${service} 服务")
                    }
                } catch (e: TipsRuntimeException) {
                    throw ServiceSwitchException(
                        ServiceSwitchException.Type.SW_Service_AddExists, service
                    )
                } catch (e: RuntimeException) {
                    throw ServiceSwitchException(
                        ServiceSwitchException.Type.SW_Service_NotFound, service
                    )
                }
            }

            Operation.REVIEW -> {
                val md = serviceListMarkdown
                try {
                    val image = imageService.getPanelA6(md, "switch")
                    from.sendImage(image)
                } catch (e: HttpServerErrorException.InternalServerError) {
                    throw ServiceSwitchException(ServiceSwitchException.Type.SW_Render_Failed)
                } catch (e: WebClientResponseException.InternalServerError) {
                    throw ServiceSwitchException(ServiceSwitchException.Type.SW_Render_Failed)
                }
            }
        }
    }

    private val serviceListMarkdown: String
        get() {
            // 这里的状态很复杂, 每个服务有三个id list(群, qq, ignore的群)
            val data = controller.queryAllBlock()
            val service1 = data.first()
            // 是否为开启状态
            service1.enable

            // 群黑名单
            service1.groups

            // 用户黑名单
            service1.users

            // ignore
            service1.ignores

            // 另外作用在全局服务的状态是第一个, 可以通过下面来判断
            service1.name == PermissionImplement.GLOBAL_PERMISSION
            // 或者直接获取
            controller!!.queryGlobal()

            val sb = StringBuilder()
            sb.append("## 服务：开关状态\n")

            sb.append(
                """
                | 状态 | 服务名 | 无法使用的群聊 |
                | :-: | :-- | :-- |
                
                """
                    .trimIndent()
            )

            val list = Permission.getClosedService()

            for (serviceName in Permission.getAllService()) {
                sb.append("| ")
                    .append(if (list.contains(serviceName)) "-" else "O")
                    .append(" | ")
                    .append(serviceName)
                    .append(" | ")
                    .append("-") // 114514, 1919810
                    .append(" |\n")
            }

            return sb.toString()
        }

    companion object {
        const val SWITCH_SERVICE_NAME = "SWITCH"
        private fun getOperation(str: String): Operation {
            return when (str) {
                "on",
                "start",
                "o",
                "s" -> Operation.ON

                "off",
                "close",
                "end",
                "f",
                "c",
                "e" -> Operation.OFF

                else -> Operation.REVIEW
            }
        }
    }
}