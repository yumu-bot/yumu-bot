package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.config.Permission
import com.now.nowbot.dao.BindDao
import com.now.nowbot.entity.bind.SBQQBindLite
import com.now.nowbot.model.SBBindUser
import com.now.nowbot.model.ppysb.SBUser
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.sbApiService.SBUserApiService
import com.now.nowbot.throwable.GeneralTipsException
import com.now.nowbot.throwable.serviceException.BindException
import com.now.nowbot.util.ASyncMessageUtil
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.command.FLAG_NAME
import com.now.nowbot.util.command.FLAG_QQ_ID
import org.springframework.stereotype.Service
import kotlin.jvm.optionals.getOrNull

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

        val isSuper = Permission.isSuperAdmin(qq)

        data.value = if (event.isAt) {
            BindParam(event.target, id, name, true, isUnbind, isSuper)
        } else {
            BindParam(qq, id, name, true, isUnbind, isSuper)
        }

        return true
    }

    override fun HandleMessage(event: MessageEvent, param: BindParam) {
        val me = event.sender.id

        if (me == param.qq) {
            if (param.isUnbind) {
                unbindQQ(me)
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
                    unbindQQ(param.qq)
                } else {
                    unbindName(param.name)
                }
            } else {
                bindQQ(event, param)
            }
            return
        }

        // bi ub 但是不是自己, 也不是超管
        throw GeneralTipsException(GeneralTipsException.Type.G_Permission_Super)
    }


    private fun bindQQ(event: MessageEvent, param: BindParam) {
        val user = getSBUser(event, param)

        val qb: SBQQBindLite? = bindDao.getSBQQLiteFromUserID(user.userID)

        if (qb == null) {
            bindDao.bindSBQQ(param.qq, SBBindUser(user))
            event.reply(BindException(BindException.Type.BIND_Progress_Binding, param.qq, user.userID, user.username))
            return
        }

        // 已有绑定：覆盖绑定
        event.reply(BindException(BindException.Type.BIND_Progress_BindingRecover, qb.bindUser.username, param.qq))

        val lock = ASyncMessageUtil.getLock(event)
        val ev: MessageEvent = lock.get() ?: throw BindException(BindException.Type.BIND_Receive_Overtime)

        if (ev.rawMessage.uppercase().startsWith("OK")) {
            bindDao.bindSBQQ(param.qq, qb.bindUser)
            event.reply(BindException.Type.BIND_Response_Success.message)
        } else {
            event.reply(BindException(BindException.Type.BIND_Receive_Refused))
        }
    }

    // TODO 这里应该查本地表并更新的
    private fun unbindName(name: String) {
        val userID = userApiService.getUserID(name) ?: throw GeneralTipsException(GeneralTipsException.Type.G_Null_Player, name)

        val qb = bindDao.getSBQQLiteFromUserID(userID) ?: throw BindException(BindException.Type.BIND_Player_HadNotBind)

        unbindQQ(qb.qq)
    }

    private fun unbindQQ(qq: Long) {
        val bind = bindDao.getSBQQLiteFromQQ(qq).getOrNull() ?: throw BindException(BindException.Type.BIND_Player_NoBind)

        if (bindDao.unBindSBQQ(bind.bindUser)) {
            throw BindException(BindException.Type.BIND_UnBind_Successes, qq)
        } else {
            throw BindException(BindException.Type.BIND_UnBind_Failed)
        }
    }


    private fun getSBUser(event: MessageEvent, param: BindParam): SBUser {
        val name: String?
        val id: Long?

        if (param.name.isNullOrEmpty() && param.id == null) {
            val lock = ASyncMessageUtil.getLock(event)
            val ev: MessageEvent = lock.get() ?: throw BindException(BindException.Type.BIND_Receive_Overtime)

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
        } ?: throw GeneralTipsException(GeneralTipsException.Type.G_Null_Player, name)

        return user
    }
}