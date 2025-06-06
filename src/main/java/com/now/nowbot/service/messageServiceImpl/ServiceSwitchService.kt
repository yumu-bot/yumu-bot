package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.config.Permission
import com.now.nowbot.permission.PermissionController
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.messageServiceImpl.ServiceSwitchService.SwitchParam
import com.now.nowbot.throwable.GeneralTipsException
import com.now.nowbot.throwable.TipsRuntimeException
import com.now.nowbot.throwable.botException.ServiceSwitchException
import com.now.nowbot.throwable.botRuntimeException.PermissionException
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.command.FLAG_QQ_GROUP
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.util.*

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
            throw PermissionException.DeniedException.BelowSuperAdministrator()
        }

        val service = m.group("service") ?: ""
        val operate = m.group("operate") ?: ""

        // var o = Pattern.compile("(black|white)?list|on|off|start|close|[bkwlofsc]+");
        val groupStr: String = m.group(FLAG_QQ_GROUP) ?: ""

        if (service.isNotBlank()) {
            if (operate.isNotBlank()) {
                if (groupStr.isNotBlank()) {
                    data.value = SwitchParam(
                        groupStr.toLong(),
                        service.uppercase(Locale.getDefault()),
                        getOperation(operate)
                    )

                } else {
                    data.value = SwitchParam(
                        -1L,
                        service.uppercase(Locale.getDefault()),
                        getOperation(operate)
                    )
                }
            } else {
                if (groupStr.isNotBlank()) {
                    throw ServiceSwitchException(ServiceSwitchException.Type.SW_Parameter_OnlyGroup)
                } else {
                    val op = getOperation(service.uppercase(Locale.getDefault()))
                    if (op != Operation.REVIEW) {
                        throw ServiceSwitchException(ServiceSwitchException.Type.SW_Service_Missing)
                    }
                    data.value = SwitchParam(-1L, null, Operation.REVIEW)
                }
            }
        } else {
            if (groupStr.isNotBlank()) {
                throw ServiceSwitchException(ServiceSwitchException.Type.SW_Parameter_OnlyGroup)
            } else {
                data.value = SwitchParam(-1L, null, Operation.REVIEW)
                // throw ServiceSwitchException(ServiceSwitchException.Type.SW_Instructions)
            }
        }

        return true
    }

    // @CheckPermission(isSuperAdmin = true)
    @Throws(Throwable::class)
    override fun HandleMessage(event: MessageEvent, param: SwitchParam) {
        val service = param.serviceName
        val group = param.groupID

        when (param.operation) {
            Operation.ON -> {
                try {
                    when (group) {
                        -1L -> {
                            controller.serviceSwitch(service, true)
                            //                        Permission.openService(service);
                            event.reply("已启动 $service 服务")
                        }
                        0L -> {
                            // 这里要放对所有群聊的操作
                            // 貌似没这功能
                            //                        permission.removeGroupAll(service, true);
                            //                        event.reply(STR."已全面清除 \{service} 服务的禁止状态");
                            event.reply("已全面解禁 $service 服务（并未修好）")
                        }
                        else -> {
                            controller.unblockGroup(service, group)
                            //                        permission.removeGroup(service, group, true,
                            // false);
                            event.reply("已解禁群聊 $group 的 $service 服务")
                        }
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
                    when (group) {
                        -1L -> {
                            controller.serviceSwitch(service, false)
                            event.reply("已关闭 $service 服务")
                        }
                        0L -> {
                            //                        permission.removeGroupAll(service, true);
                            //                        event.reply(STR."已全面清除 \{service} 服务的禁止状态");
                            event.reply("已全面禁止 $service 服务（并未修好）")
                        }
                        else -> {
                            controller.blockGroup(service, group)
                            event.reply("已禁止群聊 $group 的 $service 服务")
                        }
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
                    event.reply(image)
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
            // service1.name == PermissionImplement.GLOBAL_PERMISSION
            // 或者直接获取
            // controller!!.queryGlobal()

            val sb = StringBuilder()
            sb.append("## 服务：开关状态\n")

            sb.append(
                """
                
                要控制开关，请按 !sw service operate 的语法输入！
                
                | 状态 | 服务名 | 无法使用的群聊 |
                | :-: | :-- | :-- |
                
                """
                    .trimIndent()
            )

            val closedServices = Permission.getClosedService()

            for (name in Permission.getAllService().sorted()) {
                var isClosed = false

                for (it in closedServices) {
                    if (name.equals(it, true)) {
                        isClosed = true
                        break
                    }
                }

                sb.append("| ")
                    .append(if (isClosed) "X" else "O")
                    .append(" | ")
                    .append(name)
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
