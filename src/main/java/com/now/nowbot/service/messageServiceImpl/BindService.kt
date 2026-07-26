package com.now.nowbot.service.messageServiceImpl

import com.mikuac.shiro.core.BotContainer
import com.now.nowbot.cache.CaptchaProvider
import com.now.nowbot.config.Permission
import com.now.nowbot.config.YumuConfig
import com.now.nowbot.dao.BindDao
import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.model.BindUser
import com.now.nowbot.qq.contact.Group
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.osuApiService.OsuUserApiService
import com.now.nowbot.throwable.botRuntimeException.BindException
import com.now.nowbot.throwable.botRuntimeException.PermissionException
import com.now.nowbot.util.AsyncMessageUtil
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.command.FLAG_NAME
import com.now.nowbot.util.command.FLAG_QQ_ID
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service("BIND")
class BindService(
    private val userApiService: OsuUserApiService,
    private val bindDao: BindDao,
    private val captchaProvider: CaptchaProvider,
    private val botContainer: BotContainer,
    val yumuConfig: YumuConfig
) : MessageService<BindService.BindParam> {

    data class BindParam(
        val targetID: Long?,
        val nameOrCaptcha: String?,
        val isUnbind: Boolean
    )

    override fun isHandle(
        event: MessageEvent, messageText: String, data: DataValue<BindParam>
    ): Boolean {
        val m = Instruction.BIND.matcher(messageText)
        if (!m.find()) return false

        // 获取可能的 @ 或 指定 QQ
        val targetID = if (event.hasAt()) {
            event.target
        } else {
            m.group(FLAG_QQ_ID)?.toLongOrNull()
        }

        val nameStr = (m.group(FLAG_NAME) ?: "").trim()
        val isUnbind = m.group("un") != null || m.group("ub") != null

        data.value = BindParam(
            targetID = targetID,
            nameOrCaptcha = nameStr.ifBlank { null },
            isUnbind = isUnbind
        )
        return true
    }

    @Throws(Throwable::class)
    override fun handleMessage(event: MessageEvent, param: BindParam): ServiceCallStatistic? {
        val targetID = checkPermission(event.sender.contactID, param.targetID, event.bot?.botID)

        // 2. 解绑流程
        if (param.isUnbind) {
            return handleUnbind(event, targetID, param.nameOrCaptcha)
        }

        // 3. 绑定流程
        if (param.nameOrCaptcha != null) {
            handleUsernameBinding(event, targetID, param.nameOrCaptcha)
        } else {
            handleEmptyBinding(event, targetID)
        }

        return ServiceCallStatistic.building(event) {
            setParam(
                mapOf(
                    "qqs" to listOf(targetID),
                    "operate" to listOf("b")
                )
            )
        }
    }

    // ================== 核心绑定流程 ==================

    /**
     * 处理带有 username 或 验证码 的绑定逻辑
     */
    private fun handleUsernameBinding(event: MessageEvent, targetID: Long, input: String) {
        val maybeCaptcha = input.trim().matches(captchaRegex)

        if (maybeCaptcha) {
            // 优先尝试作为验证码
            val uid = captchaProvider.verifyCaptcha(input)
            val bindUser = uid?.let { bindDao.getBindUserOrNull(it) }

            if (bindUser != null && bindUser.hasToken) {
                // 验证码验证成功
                executeBind(event, targetID, bindUser)
                return
            }

            // 验证码无效，尝试作为 Osu User ID 获取玩家
            val user = runCatching { userApiService.getOsuUser(input) }.getOrNull()
                ?: throw BindException.BindIllegalArgumentException.IllegalVerification()

            // 找到了玩家，二次询问确认
            AsyncMessageUtil.doubleCheck(
                event = event,
                keyword = "OK",
                onCheck = {
                    // 发送确认提示消息并返回回执（用于后续自动撤回）
                    event.reply(
                        BindException.BindConfirmException.Found(targetID, user.username)
                    )
                },
                onOverTime = {
                    throw BindException.BindReceiveException.ReceiveOverTime()
                },
                onWrong = {
                    throw BindException.BindReceiveException.ReceiveRefused()
                },
                onSuccess = { _ ->
                    executeBind(event, targetID, BindUser(user))
                }
            )
        } else {
            val user = userApiService.getOsuUser(input)
            executeBind(event, targetID, BindUser(user))
        }
    }

    /**
     * 处理没有提供参数时的绑定逻辑
     */
    private fun handleEmptyBinding(event: MessageEvent, targetID: Long) {
        if (yumuConfig.bindDomain.startsWith("http")) {
            event.replyAsync(BindException.BindResultException.BindUrl(yumuConfig.bindDomain))
            return
        }

        val name = getValidNicknameOrNull(event)

        if (!name.isNullOrBlank()) {
            val user = runCatching { userApiService.getOsuUser(name) }.getOrNull()
            if (user != null) {
                executeBind(event, targetID, BindUser(user))
                return
            }
        }

        // 3. 都没有，进入一问一答流程
        interactiveBind(event, targetID)
    }

    // ================== 数据库操作与验证层 ==================

    /**
     * 执行绑定入库逻辑（前置排查冲突）
     */
    private fun executeBind(event: MessageEvent, targetID: Long, bindUser: BindUser) {
        val existQQ = bindDao.getQQLiteFromQQ(targetID)
        if (existQQ != null) {
            if (existQQ.qq == event.sender.contactID) throw BindException.BoundException.YouBound()
            else throw BindException.BoundException.UserBound(bindUser.username, existQQ.qq)
        }

        // 检查该 Osu 账号是否已被绑定
        val existQQLite = bindDao.getQQLiteFromUserID(bindUser.userID)
        if (existQQLite != null) {
            if (existQQLite.qq == event.sender.contactID) throw BindException.BoundException.YouBound()
            else throw BindException.BoundException.UserBound(bindUser.username, existQQLite.qq)
        }

        // 执行绑定
        bindDao.bindQQ(targetID, bindUser)
        event.replyAsync(BindException.BindResultException.BindSuccess(targetID, bindUser.userID, bindUser.username, bindUser.mode))
    }

    private fun handleUnbind(event: MessageEvent, targetID: Long, input: String?): ServiceCallStatistic {
        if (!input.isNullOrBlank()) {
            val userID = bindDao.getOsuID(input) ?: throw BindException.NotBindException.UserNotBind()
            val qq = bindDao.getQQ(userID)
            unbindQQ(qq)
        } else {
            unbindQQ(targetID)
        }

        return ServiceCallStatistic.building(event) {
            setParam(
                mapOf(
                    "qqs" to listOf(targetID),
                    "operate" to listOf("u")
                )
            )
        }
    }

    private fun unbindQQ(qq: Long) {
        val bindUser = bindDao.getQQLiteFromQQ(qq)?.bindUser ?: throw BindException.NotBindException.UserNotBind()
        if (! bindDao.unBindQQ(bindUser)) {
            throw BindException.UnBindException.UnbindFailed()
        }
        throw BindException.UnBindException.UnbindSuccess()
    }

    // ================== 辅助与工具方法 ==================

    /**
     * 权限拦截校验
     * 逻辑：
     * 1. 超级管理员：拥有最高权限，跳过所有限制，直接返回 targetID（为空时兜底 senderID）。
     * 2. 普通用户：
     *    - 如果 targetID == botID，重定向返回 senderID；
     *    - 如果 targetID 为 null，默认对自己操作返回 senderID；
     *    - 如果 targetID 不是自己，抛出权限异常。
     */
    private fun checkPermission(senderID: Long, targetID: Long?, botID: Long?): Long {
        // 1. 超级管理员：直接忽视后续一切限制
        if (Permission.isSuperAdmin(senderID)) {
            return targetID ?: senderID
        }

        if (botID != null && targetID == botID) {
            return senderID
        }

        val effectiveTargetID = targetID ?: senderID

        if (senderID != effectiveTargetID) {
            throw PermissionException.DeniedException.BelowSuperAdministrator()
        }

        return effectiveTargetID
    }

    /**
     * 获取用户群名片
     */
    private fun getValidNicknameOrNull(event: MessageEvent): String? {
        val bot = botContainer.robots[event.bot?.botID ?: return null] ?: return null

        val nickname = if (event.subject is Group) {
            bot.getGroupMemberInfo(event.subject.contactID, event.sender.contactID, false)?.data?.nickname
        } else {
            bot.getStrangerInfo(event.sender.contactID, false)?.data?.nickname
        } ?: return null

        return usernameRegex.findAll(nickname)
            .map { it.value.trim() }
            .filter { candidate ->
                candidate.length >= 3 && candidate.any { it.isLetterOrDigit() }
            }
            .maxByOrNull { it.length }
    }

    /**
     * 一问一答交互获取玩家名并绑定
     */
    private fun interactiveBind(event: MessageEvent, targetID: Long) {
        AsyncMessageUtil.doubleCheck(
            event = event,
            keyword = "",
            onCheck = {
                event.reply(BindException.BindReceiveException.ReceiveNoName())
            },
            onSuccess = { ev ->
                val name = ev.rawMessage.trim()

                val user = userApiService.getOsuUser(name)

                executeBind(event, targetID, BindUser(user))
            },
            onOverTime = {
                throw BindException.BindReceiveException.ReceiveOverTime()
            },
            onWrong = {},
        )
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(BindService::class.java)

        private val usernameRegex = Regex("""[A-Za-z0-9 _\[\].\-]{3,15}""")

        private val captchaRegex = Regex("\\d{6}")
    }
}