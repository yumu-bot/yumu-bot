package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.config.Permission
import com.now.nowbot.config.YumuConfig
import com.now.nowbot.dao.BindDao
import com.now.nowbot.model.BindUser
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.json.OsuUser
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.MessageReceipt
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.osuApiService.OsuUserApiService
import com.now.nowbot.throwable.GeneralTipsException
import com.now.nowbot.throwable.serviceException.BindException
import com.now.nowbot.util.ASyncMessageUtil
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.command.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.reactive.function.client.WebClientResponseException
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
            val receipt = event.reply(BindException.Type.BIND_Question_BindRetreat.message)

            val lock = ASyncMessageUtil.getLock(event, (30 * 1000).toLong())
            val ev = lock.get()

            if (ev.rawMessage.uppercase().contains("OK").not()) {
                return false
            }

            ev.subject.recall(receipt)
        }

        val isUnbind = m.group("un") != null || m.group("ub") != null
        val isSuper = Permission.isSuperAdmin(qq)
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
                bindQQAt(event, param.qq, param.isFull)
            }
            return
        }

        // bi ub 但是不是自己, 也不是超管
        throw GeneralTipsException(GeneralTipsException.Type.G_Permission_Super)
    }

    private fun unbindName(name: String) {
        val uid = bindDao.getOsuID(name) ?: throw BindException(BindException.Type.BIND_Player_HadNotBind)
        val qq = bindDao.getQQ(uid)

        if (qq < 0L) throw BindException(BindException.Type.BIND_Player_HadNotBind)

        unbindQQ(qq)
    }

    private fun unbindQQ(qq: Long?) {
        if (qq == null) throw BindException(BindException.Type.BIND_Player_NoQQ)
        val bind = bindDao.getQQLiteFromQQ(qq).getOrNull() ?: throw BindException(BindException.Type.BIND_Player_NoBind)

        if (bindDao.unBindQQ(bind.bindUser)) {
            throw BindException(BindException.Type.BIND_UnBind_Successes, qq)
        } else {
            throw BindException(BindException.Type.BIND_UnBind_Failed)
        }
    }

    private fun bindQQAt(event: MessageEvent, qq: Long, isFull: Boolean) {
        if (isFull) {
            bindUrl(event, qq)
            return
        } // 只有管理才有权力@人绑定,提示就不改了

        event.reply(BindException(BindException.Type.BIND_Receive_NoName))

        val lock = ASyncMessageUtil.getLock(event)
        var ev: MessageEvent = lock.get() ?: throw BindException(BindException.Type.BIND_Receive_Overtime)

        val name = ev.rawMessage.trim()

        val userID: Long = try {
            userApiService.getOsuID(name)
        } catch (e: HttpClientErrorException.Forbidden) {
            throw BindException(BindException.Type.BIND_Player_Banned)
        } catch (e: WebClientResponseException.Forbidden) {
            throw BindException(BindException.Type.BIND_Player_Banned)
        } catch (e: Exception) {
            throw BindException(BindException.Type.BIND_Player_NotFound)
        }

        val qb = bindDao.getQQLiteFromOsuId(userID).getOrNull() ?: run {
            event.reply(BindException(BindException.Type.BIND_Progress_Binding, qq, userID, name))
            bindDao.bindQQ(qq, BindUser(userID, name))
            return
        }

        event.reply(BindException(BindException.Type.BIND_Progress_BindingRecover, qb.osuUser.osuName, qq))

        ev = lock.get() ?: throw BindException(BindException.Type.BIND_Receive_Overtime)

        if (ev.rawMessage.uppercase().startsWith("OK")) {
            bindDao.bindQQ(qq, qb.osuUser)
            event.reply(BindException.Type.BIND_Response_Success.message)
        } else {
            event.reply(BindException(BindException.Type.BIND_Receive_Refused))
        }
    }

    @JvmRecord data class BindData(val key: Long, @JvmField val receipt: MessageReceipt, @JvmField val qq: Long)

    //默认绑定路径
    @Throws(BindException::class) private fun bindQQ(event: MessageEvent, qq: Long) {
        val bindUser: BindUser
        var osuUser: OsuUser? = null

        //检查是否已经绑定
        val qqBindLite = bindDao.getQQLiteFromQQ(qq).getOrNull()

        if (qqBindLite != null && qqBindLite.bindUser.isAuthorized) {
            bindUser = qqBindLite.bindUser

            try {
                osuUser = userApiService.getOsuUser(bindUser, OsuMode.DEFAULT)
            } catch (e: WebClientResponseException.Unauthorized) {
                event.reply(
                    BindException(
                        BindException.Type.BIND_Progress_NeedToReBindInfo,
                        bindUser.userID,
                        bindUser.username
                    ).message
                )
            }

            if (osuUser != null && osuUser.userID != bindUser.userID) {
                throw RuntimeException()
            }

            event.reply(
                BindException(
                    BindException.Type.BIND_Progress_BindingRecoverInfo,
                    bindUser.userID,
                    bindUser.username
                ).message
            )

            try {
                val lock = ASyncMessageUtil.getLock(event)
                val ev = lock.get() ?: throw BindException(BindException.Type.BIND_Receive_Overtime)
                if (ev.rawMessage.uppercase().contains("OK").not()) {
                    event.reply(BindException.Type.BIND_Receive_Refused.message)
                    return
                }
            } catch (e: WebClientResponseException.Unauthorized) {
                throw e
            } catch (ignored: Exception) { // 如果符合，直接允许绑定
            }
        }

        // 需要绑定
        bindUrl(event, qq)
    }

    private fun bindUrl(event: MessageEvent, qq: Long) { // 将当前毫秒时间戳作为 key
        if (yumuConfig.bindDomain.contains("http")) {
            event.reply("""
                ${yumuConfig.bindDomain}
                请 ctrl+c 并 ctrl+v 到其他 browser 完成绑定。
            """.trimMargin())
        } else {
            bindQQAt(event, qq, isFull = false)
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
                event.reply(BindException.Type.BIND_Response_Success.message)
                return
            }
        }

        if (bindDao.getQQLiteFromQQ(qq).getOrNull() != null) {
            throw BindException(BindException.Type.BIND_Response_AlreadyBound)
        }
        val userID: Long
        try {
            userID = userApiService.getOsuID(name)
        } catch (e: HttpClientErrorException.Forbidden) {
            throw BindException(BindException.Type.BIND_Player_Banned)
        } catch (e: WebClientResponseException.Forbidden) {
            throw BindException(BindException.Type.BIND_Player_Banned)
        } catch (e: Exception) {
            throw BindException(BindException.Type.BIND_Player_NotFound)
        }

        val qb = bindDao.getQQLiteFromOsuId(userID).getOrNull()

        if (qb != null) {
            event.reply(BindException(BindException.Type.BIND_Response_AlreadyBoundInfo, qb.qq, name))
        } else {
            bindDao.bindQQ(qq, BindUser(userID, name))
            event.reply(BindException(BindException.Type.BIND_Progress_Binding, qq, userID, name).message)
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

        private fun find(map: Array<IntArray>, size: Int, start: Int, end: Int): Int {
            val toMin = IntArray(size)
            val find = IntArray(size)
            find[start] = 1
            Arrays.fill(toMin, Int.MAX_VALUE)
            var point = start
            var pointBef = start
            for (n in 0..<size - 1) {
                var minIndex = -1
                var min = Int.MAX_VALUE
                for (i in 0..<size) {
                    if (find[i] == 1) continue
                    if (map[pointBef][point] + map[point][i] >= toMin[i]) continue
                    toMin[i] = map[pointBef][point] + map[point][i]
                    if (min > toMin[i]) {
                        min = toMin[i]
                        minIndex = i
                    }
                }
                if (minIndex == end) return toMin[minIndex]
                if (minIndex < 0) {
                    for (i in 0..<size) {
                        if (find[i] == 1) continue
                        if (min > toMin[i]) {
                            min = toMin[i]
                            minIndex = i
                        }
                    }
                }
                find[minIndex] = 1
                pointBef = point
                point = minIndex
            }
            return toMin[end]
        }
    }
}