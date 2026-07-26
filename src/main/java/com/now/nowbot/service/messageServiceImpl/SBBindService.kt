package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.config.Permission
import com.now.nowbot.dao.BindDao
import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.model.SBBindUser
import com.now.nowbot.model.ppysb.SBUser
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.sbApiService.SBUserApiService
import com.now.nowbot.throwable.botRuntimeException.BindException
import com.now.nowbot.throwable.botRuntimeException.NoSuchElementException
import com.now.nowbot.throwable.botRuntimeException.PermissionException
import com.now.nowbot.util.AsyncMessageUtil
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.command.FLAG_NAME
import com.now.nowbot.util.command.FLAG_QQ_ID
import org.springframework.stereotype.Service

@Service("SB_BIND")
class SBBindService(
    private val userApiService: SBUserApiService,
    private val bindDao: BindDao,
) : MessageService<SBBindService.BindParam> {

    data class BindParam(
        val targetID: Long?,
        val input: String?,
        val isUnbind: Boolean
    )

    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: MessageService.DataValue<BindParam>
    ): Boolean {
        val m = Instruction.SB_BIND.matcher(messageText)
        if (!m.find()) return false

        // 获取可能的 @ 或者 指定 QQ
        val targetID = if (event.hasAt()) {
            event.target
        } else {
            m.group(FLAG_QQ_ID)?.toLongOrNull()
        }

        val inputStr = m.group(FLAG_NAME)?.trim()
        val isUnbind = m.group("un") != null || m.group("ub") != null

        data.value = BindParam(
            targetID = targetID,
            input = inputStr?.takeIf { it.isNotBlank() },
            isUnbind = isUnbind
        )

        return true
    }

    override fun handleMessage(event: MessageEvent, param: BindParam): ServiceCallStatistic? {
        val senderID = event.sender.contactID
        val targetID = param.targetID ?: senderID

        // 1. 权限校验
        checkPermission(senderID, targetID)

        // 2. 解绑流程
        if (param.isUnbind) {
            handleUnbind(senderID, targetID, param.input)
            return ServiceCallStatistic.building(event)
        }

        // 3. 绑定流程
        val sbUser = if (param.input != null) {
            fetchSBUser(param.input)
        } else {
            interactiveFetchUser(event)
        }

        executeBind(event, targetID, sbUser)

        return ServiceCallStatistic.building(event)
    }

    // ================== 核心绑定/解绑流程 ==================

    /**
     * 处理解绑逻辑
     */
    private fun handleUnbind(senderID: Long, targetID: Long, input: String?) {
        val isMyself = senderID == targetID

        // 如果传入了名称且是超管，按名称解绑；否则按 QQ 解绑
        if (input != null && Permission.isSuperAdmin(senderID)) {
            unbindByInput(input)
        } else {
            unbindQQ(targetID, isMyself)
        }
    }

    /**
     * 执行绑定入库逻辑（含覆盖确认）
     */
    private fun executeBind(event: MessageEvent, targetID: Long, user: SBUser) {
        val existingBind = bindDao.getSBQQLiteFromQQ(targetID)

        // 已有绑定：触发覆盖绑定二次确认
        if (existingBind != null) {
            AsyncMessageUtil.doubleCheck(
                event = event,
                keyword = "OK",
                onCheck = {
                    event.reply(
                        BindException.BindConfirmException.RecoverBind(
                            user.username,
                            existingBind.bindUser.username,
                            targetID
                        )
                    )
                },
                onOverTime = {
                    throw BindException.BindReceiveException.ReceiveOverTime()
                },
                onWrong = {
                    throw BindException.BindReceiveException.ReceiveRefused()
                },
                onSuccess = {}
            )
        }

        // 写入数据库
        bindDao.bindSBQQ(targetID, SBBindUser(user))

        event.replyAsync(BindException.BindResultException.BindSuccess(targetID, user.userID, user.username, user.mode, PREFIX))
    }

    // ================== API 查询与数据库交互层 ==================

    /**
     * 根据输入获取 SBUser，自动判断是 ID 还是 Name
     */
    private fun fetchSBUser(input: String): SBUser {
        val id = input.toLongOrNull()

        val user = if (id != null) {
            userApiService.getUser(id = id)
        } else {
            userApiService.getUser(username = input)
        }

        return user ?: throw NoSuchElementException.Player(input)
    }

    /**
     * 按名称或 ID 查找到玩家后解除绑定
     */
    private fun unbindByInput(input: String) {
        val user = fetchSBUser(input)
        val qb = bindDao.getSBQQLiteFromUserID(user.userID)
            ?: throw BindException.NotBindException.UserNotBind()

        unbindQQ(qb.qq, isMyself = false)
    }

    /**
     * 解除指定 QQ 的绑定
     */
    private fun unbindQQ(qq: Long, isMyself: Boolean) {
        val bind = bindDao.getSBQQLiteFromQQ(qq) ?: if (isMyself) {
            throw BindException.NotBindException.YouNotBind()
        } else {
            throw BindException.NotBindException.UserNotBind()
        }

        if (bindDao.unBindSBQQ(bind.bindUser)) {
            throw BindException.UnBindException.UnbindSuccess()
        } else {
            throw BindException.UnBindException.UnbindFailed()
        }
    }

    // ================== 辅助与工具方法 ==================

    /**
     * 权限拦截校验
     */
    private fun checkPermission(senderID: Long, targetID: Long) {
        if (senderID != targetID && !Permission.isSuperAdmin(senderID)) {
            throw PermissionException.DeniedException.BelowSuperAdministrator()
        }
    }

    /**
     * 交互式一问一答获取玩家信息
     */
    private fun interactiveFetchUser(event: MessageEvent): SBUser {
        var sbUser: SBUser? = null

        AsyncMessageUtil.doubleCheck(
            event = event,
            keyword = "",
            onCheck = {
                event.reply(BindException.BindReceiveException.ReceiveNoName())
            },
            onOverTime = { throw BindException.BindReceiveException.ReceiveOverTime() },
            onWrong = {},
            onSuccess = { result ->
                val input = result.rawMessage.trim()
                sbUser = fetchSBUser(input)
            }
        )

        return sbUser!!
    }

    companion object {
        private const val PREFIX = '?'
    }
}