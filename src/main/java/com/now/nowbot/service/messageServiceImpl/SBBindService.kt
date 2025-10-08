package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.config.Permission
import com.now.nowbot.dao.BindDao
import com.now.nowbot.entity.bind.SBQQBindLite
import com.now.nowbot.model.SBBindUser
import com.now.nowbot.model.ppysb.SBUser
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.sbApiService.SBUserApiService

import com.now.nowbot.throwable.botRuntimeException.BindException
import com.now.nowbot.throwable.botRuntimeException.NoSuchElementException
import com.now.nowbot.throwable.botRuntimeException.PermissionException
import com.now.nowbot.util.ASyncMessageUtil
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
        val qq: Long,
        val id: Long?,
        val name: String?,
        val at: Boolean,
        val isUnbind: Boolean,
        val isSuper: Boolean,
    )

    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: MessageService.DataValue<BindParam>
    ): Boolean {
        val m = Instruction.SB_BIND.matcher(messageText)
        if (!m.find()) return false

        val qq = m.group(FLAG_QQ_ID)?.toLongOrNull() ?: event.sender.id

        val isUnbind = m.group("un") != null || m.group("ub") != null

        val nameStr = m.group(FLAG_NAME)
        val id = nameStr?.toLongOrNull()
        val name = if (id == null) nameStr else null

        val isSuper = Permission.isSuperAdmin(event.sender.id)

        data.value = if (event.isAt) {
            BindParam(event.target, id, name, true, isUnbind, isSuper)
        } else {
            BindParam(qq, id, name, false, isUnbind, isSuper)
        }

        return true
    }

    override fun handleMessage(event: MessageEvent, param: BindParam) {
        val me = event.sender.id

        if (me == param.qq) {
            if (param.isUnbind) {
                unbindQQ(me, isMyself = true)
            } else {
                bindQQ(event, param)
            }
            return
        }

        // 超管使用量少, 所以相关分支靠后
        // 超级管理员的专属权利：艾特绑定和全 QQ 移除绑定
        if (param.isSuper) {
            if (param.isUnbind) {
                if (param.name.isNullOrEmpty()) {
                    unbindQQ(param.qq, isMyself = false)
                } else {
                    unbindName(param.name)
                }
            } else {
                bindQQ(event, param)
            }
            return
        }

        // bi ub 但是不是自己, 也不是超管
        throw PermissionException.DeniedException.BelowSuperAdministrator()
    }


    private fun bindQQ(event: MessageEvent, param: BindParam) {
        val user = getSBUser(event, param)

        val qb: SBQQBindLite? = bindDao.getSBQQLiteFromQQ(param.qq)

        if (qb == null) {
            bindDao.bindSBQQ(param.qq, SBBindUser(user))
            bindDao.updateSBMode(user.userID, user.mode)

            event.reply(BindException.BindResultException.BindSuccess(param.qq, user.userID, user.username, user.mode))

            return
        }

        // 已有绑定：覆盖绑定
        event.reply(BindException.BindConfirmException.RecoverBind(user.username, qb.bindUser.username, param.qq))

        val lock = ASyncMessageUtil.getLock(event)
        val ev = lock.get() ?: throw BindException.BindReceiveException.ReceiveOverTime()

        if (ev.rawMessage.uppercase().startsWith("OK")) {
            bindDao.bindSBQQ(param.qq, SBBindUser(user))
            bindDao.updateSBMode(user.userID, user.mode)

            event.reply(BindException.BindResultException.BindSuccess(param.qq, user.userID, user.username, user.mode))
        } else {
            event.reply(BindException.BindReceiveException.ReceiveRefused())
        }
    }

    // TODO 这里应该查本地表并更新的
    private fun unbindName(name: String) {
        val userID = userApiService.getUserID(name) ?: throw NoSuchElementException.Player(name)

        val qb = bindDao.getSBQQLiteFromUserID(userID) ?: throw BindException.NotBindException.YouNotBind()

        unbindQQ(qb.qq)
    }

    private fun unbindQQ(qq: Long, isMyself: Boolean = true) {
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


    private fun getSBUser(event: MessageEvent, param: BindParam): SBUser {
        val name: String?
        val id: Long?

        if (param.name.isNullOrEmpty() && param.id == null) {
            event.reply(BindException.BindReceiveException.ReceiveNoName())

            val lock = ASyncMessageUtil.getLock(event)
            val ev: MessageEvent = lock.get() ?: throw BindException.BindReceiveException.ReceiveOverTime()

            val maybeID = ev.rawMessage.trim().toLongOrNull()

            if (maybeID == null) {
                name = ev.rawMessage.trim()
                id = null
            }
            else {
                name = null
                id = maybeID
            }
        } else {
            name = param.name
            id = param.id
        }

        val user = if (id == null) {
            userApiService.getUser(username = name)
        } else {
            userApiService.getUser(id = id)
        } ?: throw NoSuchElementException.Player(name)

        return user
    }
}