package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.config.Permission
import com.now.nowbot.config.YumuConfig
import com.now.nowbot.dao.BindDao
import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.model.BindUser
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.osu.OsuUser
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.MessageReceipt
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.osuApiService.OsuUserApiService

import com.now.nowbot.throwable.botRuntimeException.BindException
import com.now.nowbot.throwable.botRuntimeException.PermissionException
import com.now.nowbot.util.ASyncMessageUtil
import com.now.nowbot.util.DataUtil.findCauseOfType
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.command.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Predicate

@Service("BIND") class BindService(
    private val userApiService: OsuUserApiService,
    private val bindDao: BindDao,
    val yumuConfig: YumuConfig
) : MessageService<BindService.BindParam> {

    // full: 全绑定，只有 oauth 应用所有者可以这样做
    data class BindParam(
        val qq: Long,
        val name: String,
        val at: Boolean,
        val unbind: Boolean,
        val isSuper: Boolean,
        val isFull: Boolean,
        val isCaptcha: Boolean = false,
    )

    override fun isHandle(
        event: MessageEvent, messageText: String, data: DataValue<BindParam>
    ): Boolean {
        val m = Instruction.BIND.matcher(messageText)
        if (!m.find()) return false

        val qq = m.group(FLAG_QQ_ID)?.toLongOrNull() ?: event.sender.id

        val nameStr = (m.group(FLAG_NAME) ?: "").trim()

        val isCaptcha = nameStr.matches("\\d{6}".toRegex())

        val isYmBot = messageText.take(3).contains("ym", ignoreCase = true) ||
                m.group("bi") != null ||
                m.group("un") != null ||
                m.group("ub") != null

        val name = if (!isYmBot && nameStr.isNotBlank() && !isCaptcha && nameStr.contains("osu")) {
            if (userApiService.isPlayerExist(nameStr)) {
                userApiService.getOsuUser(nameStr).username
            } else {
                log.info("绑定：退避成功：!bind $nameStr")
                return false
            }
        } else {
            nameStr
        }


        if (!isYmBot && !isCaptcha) {
            // 提问
            val receipt = event.reply(BindException.BindConfirmException.NeedConfirm())

            val lock = ASyncMessageUtil.getLock(event, 30L * 1000)
            val ev = lock.get()

            if (ev.rawMessage.uppercase().contains("OK").not()) {
                return false
            }

            ev.subject.recall(receipt)
        }

        val isUnbind = m.group("un") != null || m.group("ub") != null
        val isSuper = Permission.isSuperAdmin(event.sender.id)
        val isFull = m.group("full") != null

        val param = if (event.hasAt()) { // bi/ub @
            BindParam(event.target, name, true, isUnbind, isSuper, isFull, isCaptcha)
        } else { // bi
            BindParam(qq, name, false, isUnbind, isSuper, isFull, isCaptcha)
        }

        data.value = param
        return true
    }

    @Throws(Throwable::class)
    override fun handleMessage(event: MessageEvent, param: BindParam): ServiceCallStatistic? {
        val me = event.sender.id

        if (me == param.qq) {
            if (param.unbind) {
                unbindQQ(me)
            } else if (param.name.isNotBlank()) {
                bindQQName(event, param.name, me, param.isCaptcha)
            } else {
                bindQQ(event, me)
            }
            return ServiceCallStatistic.building(event)
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
                bindQQName(event, param.name, param.qq, param.isCaptcha)
            } else if (param.at) {
                bindQQAt(event, param.qq)
            }
            return ServiceCallStatistic.building(event)
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
        val bind = bindDao.getQQLiteFromQQ(qq) ?: throw BindException.NotBindException.UserNotBind()

        if (bindDao.unBindQQ(bind.bindUser!!)) {
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

        val qb = bindDao.getQQLiteFromUserID(ou.userID)

        if (qb == null) {
            bindDao.bindQQ(qq, BindUser(ou))
            bindDao.updateMode(ou.userID, ou.defaultOsuMode)
            event.reply(BindException.BindResultException.BindSuccess(qq, ou.userID, ou.username, ou.defaultOsuMode))
            return
        }

        event.reply(BindException.BindConfirmException.RecoverBind(ou.username, qb.bindUser!!.username, qq))

        ev = lock.get() ?: throw BindException.BindReceiveException.ReceiveOverTime()

        if (ev.rawMessage.uppercase().startsWith("OK")) {
            bindDao.bindQQ(qq, BindUser(ou))

            event.reply(BindException.BindResultException.BindSuccess(qq, ou.userID, ou.username, ou.defaultOsuMode))
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
        val qqBindLite = bindDao.getQQLiteFromQQ(qq)

        if (qqBindLite != null && qqBindLite.bindUser!!.isAuthorized) {
            bindUser = qqBindLite.bindUser!!

            try {
                osuUser = userApiService.getOsuUser(bindUser, OsuMode.DEFAULT)

                if (osuUser.userID != bindUser.userID) {
                    throw RuntimeException()
                }

                event.reply(BindException.BindConfirmException.NoNeedReBind(bindUser.userID, bindUser.username))
            } catch (e: Exception) {
                if (e.findCauseOfType<WebClientResponseException.Forbidden>() != null) {
                    event.reply(BindException.BindConfirmException.NeedReBind(bindUser.userID, bindUser.username))
                }
            }

            try {
                val lock = ASyncMessageUtil.getLock(event)
                val ev = lock.get() ?: throw BindException.BindReceiveException.ReceiveOverTime()
                if (ev.rawMessage.uppercase().contains("OK").not()) {
                    event.reply(BindException.BindReceiveException.ReceiveRefused())
                    return
                }
            } catch (_: Exception) { // 如果符合，直接允许绑定

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

    private fun bindQQName(event: MessageEvent, name: String, qq: Long, isCaptcha: Boolean) {
        // 绑定先判断是否是传入验证码
        if (isCaptcha) {
            val uid = bindDao.verifyCaptcha(name)
            val bu = bindDao.getBindUser(uid)
            if (bu != null && bu.isAuthorized) {
                val mode = userApiService.getOsuUser(bu.userID).currentOsuMode

                bindDao.bindQQ(qq, bu)
                bindDao.updateMode(bu.userID, mode)

                event.reply(BindException.BindResultException.BindSuccessWithMode(mode))
                return
            }
        }

        val ql = bindDao.getQQLiteFromQQ(qq)

        if (ql != null) {
            if (ql.qq == event.sender.id) {
                throw BindException.BoundException.YouBound()
            } else {
                throw BindException.BoundException.UserBound(name, ql.qq!!)
            }
        }

        val ou = userApiService.getOsuUser(name)

        val qb = bindDao.getQQLiteFromUserID(ou.userID)

        if (qb != null) {
            if (qb.qq == event.sender.id) {
                throw BindException.BoundException.YouBound()
            } else {
                throw BindException.BoundException.UserBound(name, qb.qq!!)
            }
        } else {
            bindDao.bindQQ(qq, BindUser(ou))
            bindDao.updateMode(ou.userID, ou.defaultOsuMode)
            event.reply(BindException.BindResultException.BindSuccess(qq, ou.userID, name, ou.defaultOsuMode))
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