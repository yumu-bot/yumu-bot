package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.config.Permission
import com.now.nowbot.config.YumuConfig
import com.now.nowbot.dao.BindDao
import com.now.nowbot.model.BindUser
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.osu.OsuUser
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.MessageReceipt
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.osuApiService.OsuUserApiService
import com.now.nowbot.throwable.GeneralTipsException
import com.now.nowbot.throwable.botRuntimeException.BindException
import com.now.nowbot.throwable.botRuntimeException.PermissionException
import com.now.nowbot.util.ASyncMessageUtil
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.command.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Predicate
import java.util.regex.Pattern
import kotlin.jvm.optionals.getOrNull

@Service("BIND") class BindService(
    private val userApiService: OsuUserApiService,
    private val bindDao: BindDao,
    val yumuConfig: YumuConfig
) : MessageService<BindService.BindParam> {
    val captchaReg: Pattern = "\\d{6}".toPattern()

    // full: 全绑定，只有 oauth 应用所有者可以这样做
    data class BindParam(
        val qq: Long,
        val name: String,
        val at: Boolean,
        val unbind: Boolean,
        val isSuper: Boolean,
        val isFull: Boolean
    )

    override fun isHandle(
        event: MessageEvent, messageText: String, data: DataValue<BindParam>
    ): Boolean {
        val m = Instruction.BIND.matcher(messageText)
        if (!m.find()) return false

        val qq = m.group(FLAG_QQ_ID)?.toLongOrNull() ?: event.sender.id

        val nameStr = m.group(FLAG_NAME) ?: ""

        val isYmBot = messageText.substring(0, 3).contains("ym") ||
                m.group("bi") != null ||
                m.group("un") != null ||
                m.group("ub") != null

        val needConfirm = isYmBot.not() && m.group("bind").isNullOrBlank()

        val name = if (needConfirm) {
            // 带着 ym 以及特殊短链不用问
            if (nameStr.isNotBlank() && nameStr.contains("osu") && userApiService.isPlayerExist(nameStr)) {
                userApiService.getOsuUser(nameStr).username
            } else {
                log.info("绑定：退避成功：!bind osu <data>")
                return false
            }
        } else {
            nameStr
        }

        if (needConfirm) {
            // 提问
            val receipt = event.reply(BindException.BindConfirmException.ConfirmThis())

            val lock = ASyncMessageUtil.getLock(event, (30 * 1000).toLong())
            val ev = lock.get()

            if (ev.rawMessage.uppercase().contains("OK").not()) {
                return false
            }

            ev.subject.recall(receipt)
        }

        val isUnbind = m.group("un") != null || m.group("ub") != null
        val isSuper = Permission.isSuperAdmin(event.sender.id)
        val isFull = m.group("full") != null

        val param = if (event.isAt) { // bi/ub @
            BindParam(event.target, name, true, isUnbind, isSuper, isFull)
        } else { // bi
            BindParam(qq, name, false, isUnbind, isSuper, isFull)
        }

        data.value = param
        return true
    }

    @Throws(Throwable::class) override fun HandleMessage(event: MessageEvent, param: BindParam) {
        val me = event.sender.id

        if (me == param.qq) {
            if (param.unbind) {
                unbindQQ(me)
            } else if (param.name.isNotBlank()) {
                bindQQName(event, param.name, me)
            } else {
                bindQQ(event, me)
            }
            return
        }

        // 超管使用量少, 所以相关分支靠后
        // 超级管理员的专属权利：艾特绑定和全 QQ 移除绑定
        if (param.isSuper) {
            if (param.unbind) {
                if (param.name.isNotBlank()) {
                    unbindName(param.name)
                } else {
                    unbindQQ(param.qq)
                }
            } else if (param.name.isNotBlank()) {
                bindQQName(event, param.name, param.qq)
            } else if (param.at) {
                bindQQAt(event, param.qq)
            }
            return
        }

        // bi ub 但是不是自己, 也不是超管
        throw PermissionException.DeniedException.BelowSuperAdministrator()
    }

    private fun unbindName(name: String) {
        val uid = bindDao.getOsuID(name) ?: throw BindException.NotBindException.UserNotBind()
        val qq = bindDao.getQQ(uid)

        if (qq < 0L) throw BindException.NotBindException.UserNotBind()

        unbindQQ(qq)
    }

    private fun unbindQQ(qq: Long?) {
        if (qq == null) throw BindException.BindIllegalArgumentException.IllegalQQ()
        val bind = bindDao.getQQLiteFromQQ(qq).getOrNull() ?: throw BindException.NotBindException.UserNotBind()

        if (bindDao.unBindQQ(bind.bindUser)) {
            throw BindException.UnBindException.UnbindSuccess()
        } else {
            throw BindException.UnBindException.UnbindFailed()
        }
    }

    private fun bindQQAt(event: MessageEvent, qq: Long) {

        event.reply(BindException.BindReceiveException.ReceiveNoName())

        val lock = ASyncMessageUtil.getLock(event)
        var ev: MessageEvent = lock.get() ?: throw BindException.BindReceiveException.ReceiveOverTime()

        val name = ev.rawMessage.trim()

        val ou: OsuUser = userApiService.getOsuUser(name)

        val qb = bindDao.getQQLiteFromOsuId(ou.userID)

        if (qb == null) {
            bindDao.bindQQ(qq, BindUser(ou))
            bindDao.updateMode(ou.userID, ou.defaultOsuMode)
            event.reply(BindException.BindResultException.BindSuccess(qq, ou.userID, ou.username))
            return
        }

        event.reply(BindException.BindConfirmException.RecoverBind(ou.username, qb.bindUser.username, qq))

        ev = lock.get() ?: throw BindException.BindReceiveException.ReceiveOverTime()

        if (ev.rawMessage.uppercase().startsWith("OK")) {
            val result = bindDao.bindQQ(qq, BindUser(ou))
            if (result != null) {
                event.reply(BindException.BindResultException.BindSuccess(qq, ou.userID, ou.username))
            } else {
                event.reply(BindException.BindResultException.BindFailed())
            }
        } else {
            event.reply(BindException.BindReceiveException.ReceiveRefused())
        }
    }

    @JvmRecord data class BindData(val key: Long, @JvmField val receipt: MessageReceipt, @JvmField val qq: Long)

    //默认绑定路径
    @Throws(BindException::class) private fun bindQQ(event: MessageEvent, qq: Long) {
        val bindUser: BindUser
        val osuUser: OsuUser?

        //检查是否已经绑定
        val qqBindLite = bindDao.getQQLiteFromQQ(qq).getOrNull()

        if (qqBindLite != null && qqBindLite.bindUser.isAuthorized) {
            bindUser = qqBindLite.bindUser

            try {
                osuUser = userApiService.getOsuUser(bindUser, OsuMode.DEFAULT)

                if (osuUser.userID != bindUser.userID) {
                    throw RuntimeException()
                }

                event.reply(BindException.BindConfirmException.NoNeedReBind(bindUser.userID, bindUser.username))
            } catch (e: GeneralTipsException) {
                if (e.message?.contains("403") == true)
                event.reply(BindException.BindConfirmException.NeedReBind(bindUser.userID, bindUser.username))
            }

            try {
                val lock = ASyncMessageUtil.getLock(event)
                val ev = lock.get() ?: throw BindException.BindReceiveException.ReceiveOverTime()
                if (ev.rawMessage.uppercase().contains("OK").not()) {
                    event.reply(BindException.BindReceiveException.ReceiveRefused())
                    return
                }
            } catch (ignored: Exception) { // 如果符合，直接允许绑定
            }
        }

        // 需要绑定
        bindUrl(event, qq)
    }

    private fun bindUrl(event: MessageEvent, qq: Long) { // 将当前毫秒时间戳作为 key
        if (yumuConfig.bindDomain.contains("http")) {
            event.reply(BindException.BindResultException.BindUrl(yumuConfig.bindDomain))
        } else {
            bindQQAt(event, qq)
        }
    }

    private fun bindQQName(event: MessageEvent, name: String, qq: Long) {
        // 绑定先判断是否是传入验证码
        val m = captchaReg.matcher(name.trim())
        if (m.find()) {
            val code = m.group(0)
            val uid = bindDao.verifyCaptcha(code)
            val bu = bindDao.getBindUser(uid)
            if (bu != null && bu.isAuthorized) {
                bindDao.bindQQ(qq, bu)
                event.reply(BindException.BindResultException.BindSuccessWithMode(bu.mode))
                return
            }
        }

        val ql = bindDao.getQQLiteFromQQ(qq).getOrNull()

        if (ql != null) {
            if (ql.qq == event.sender.id) {
                throw BindException.BoundException.YouBound()
            } else {
                throw BindException.BoundException.UserBound(name, ql.qq)
            }
        }

        val ou = userApiService.getOsuUser(name)

        val qb = bindDao.getQQLiteFromOsuId(ou.userID)

        if (qb != null) {
            if (qb.qq == event.sender.id) {
                throw BindException.BoundException.YouBound()
            } else {
                throw BindException.BoundException.UserBound(name, qb.qq)
            }
        } else {
            bindDao.bindQQ(qq, BindUser(ou))
            bindDao.updateMode(ou.userID, ou.defaultOsuMode)
            event.reply(BindException.BindResultException.BindSuccess(qq, ou.userID, name))
        }
    }

    /**
     * 检查绑定次数
     */
    fun check(qq: Long): Boolean {
        val check = Predicate { t: Long -> t + 1000 * 60 * 30 < System.currentTimeMillis() }
        BIND_CACHE.entries.removeIf {
            it.value.removeIf(check)
            it.value.isEmpty()
        }

        val timeList = BIND_CACHE.computeIfAbsent(qq) { ArrayList() }
        timeList.removeIf(check)
        timeList.addLast(System.currentTimeMillis())
        return timeList.size > 3
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(BindService::class.java)
        private val BIND_MSG_MAP: MutableMap<Long, BindData> = ConcurrentHashMap()
        private val BIND_CACHE: MutableMap<Long, MutableList<Long>> = ConcurrentHashMap()

        @JvmStatic fun contains(t: Long): Boolean {
            return BIND_MSG_MAP.containsKey(t)
        }

        @JvmStatic fun getBind(t: Long): BindData? {
            removeOldBind()
            return BIND_MSG_MAP[t]
        }

        @JvmStatic fun removeBind(t: Long) {
            BIND_MSG_MAP.remove(t)
        }

        private fun removeOldBind() {
            BIND_MSG_MAP.keys.removeIf { k: Long -> (k + 120 * 1000) < System.currentTimeMillis() }
        }

    }
}