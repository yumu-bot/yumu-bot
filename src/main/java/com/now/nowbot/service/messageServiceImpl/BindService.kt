package com.now.nowbot.service.messageServiceImpl

import com.mikuac.shiro.core.BotContainer
import com.now.nowbot.cache.CaptchaProvider
import com.now.nowbot.config.YumuConfig
import com.now.nowbot.dao.BindDao
import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.model.BindUser
import com.now.nowbot.qq.contact.Group
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.restrict.RestrictUtils
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.osuApiService.OsuUserApiService
import com.now.nowbot.throwable.botRuntimeException.BindException
import com.now.nowbot.throwable.botRuntimeException.PermissionException
import com.now.nowbot.util.AsyncMessageUtil
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.command.FLAG_NAME
import com.now.nowbot.util.command.FLAG_QQ_ID
import com.now.nowbot.util.command.REGEX_OSU_USERNAME
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
        val value = input.trim()
        val userID = value.toLongOrNull()

        val maybeUserID = userID != null && userID !in 100000L .. 999999L

        val maybeCaptcha = userID != null && userID in 100000L .. 999999L

        if (maybeUserID) {
            val user = runCatching {
                userApiService.getOsuUser(userID)
            }.getOrNull()

            if (user != null) {
                executeBind(event, targetID, BindUser(user))
                return
            }
        }

        if (maybeCaptcha) {
            // 优先尝试作为验证码
            val uid = captchaProvider.verifyCaptcha(value)

            if (uid != null) {
                // 验证码只会在 oauth2 授权完成后生成，正常情况下必然带有令牌
                val bindUser = bindDao.getBindUserOrNull(uid)

                if (bindUser != null && bindUser.hasToken) {
                    // 验证码验证成功。
                    // 注意：此时玩家可能已经通过 username 绑定过一次，
                    // 必须用 fromCaptcha 区分「完成 oauth2 授权」和「重复绑定」。
                    executeBind(event, targetID, bindUser, fromCaptcha = true)
                    return
                }

                // 验证码存在，但对应账号没有 oauth2 令牌（可能已被降级或解绑）
                throw BindException.BindIllegalArgumentException.IllegalVerification()
            }

            // 验证码已经被使用过：如果当前 QQ 绑定的正好是这个账号，说明是重复操作
            val usedUID = captchaProvider.getUsedCaptchaUserID(value)
            if (usedUID != null) {
                val boundUID = bindDao.getQQLiteFromQQ(targetID)?.bindUser?.userID

                if (boundUID == usedUID) {
                    throw BindException.BindResultException.BindRepeated()
                }

                throw BindException.BindIllegalArgumentException.IllegalVerification()
            }

            // 已经绑定的 QQ 不允许再把 6 位数字当成 uid 来解析，避免误绑其他玩家
            if (bindDao.getQQLiteFromQQ(targetID) != null) {
                throw BindException.BindIllegalArgumentException.IllegalVerification()
            }

            // 验证码无效，尝试作为 Osu User ID 获取玩家
            val user = runCatching { userApiService.getOsuUser(value) }
                .recoverCatching { userApiService.getOsuUser(userID) }.getOrElse {
                    throw BindException.BindIllegalArgumentException.IllegalVerification()
                }

            // 找到了玩家，二次询问确认
            AsyncMessageUtil.doubleCheck(
                event = event,
                keywords = listOf("OK"),
                onCheck = {
                    event.reply(BindException.BindConfirmException.Found(user.userID, user.username))
                },
                onOverTime = {
                    throw BindException.BindReceiveException.ReceiveOverTime()
                },
                onFailure = {
                    throw BindException.BindReceiveException.ReceiveQuestion()
                },
                onSuccess = { _ ->
                    executeBind(event, targetID, BindUser(user))
                }
            )
        } else {
            val user = userApiService.getOsuUser(value)
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
     *
     * @param fromCaptcha 是否来自 oauth2 授权完成后的六位数验证码。
     * 由于 oauth2 回调会先更新 osu_bind_user，此时 QQ 可能已经通过 username 绑定到同一个玩家，
     * 不能简单判定为「已经绑定」，需要区分「确认完成绑定」和「重复操作」。
     */
    private fun executeBind(
        event: MessageEvent,
        targetID: Long,
        bindUser: BindUser,
        fromCaptcha: Boolean = false
    ) {
        val existQQ = bindDao.getQQLiteFromQQ(targetID)
        if (existQQ != null) {
            val boundUser = existQQ.bindUser

            // 这个 QQ 已经绑定到同一个 osu! 玩家
            if (boundUser != null && boundUser.userID == bindUser.userID) {
                if (! fromCaptcha) {
                    // 用户名 / uid 的重复绑定
                    throw BindException.BoundException.YouBound()
                }

                // 验证码流程：oauth2 授权已经写入该玩家，说明本次绑定已经完成。
                // 这里应当明确回复「确认完成绑定」，而不是误报成「您已绑定」。
                bindDao.bindQQ(targetID, bindUser)

                event.replyAsync(
                    BindException.BindResultException.BindSuccessWithCaptcha(
                        bindUser.userID, bindUser.username, bindUser.mode
                    )
                )
                return
            }

            // 这个 QQ 已经绑定了其他的 osu! 玩家
            if (existQQ.qq == event.sender.contactID) {
                throw BindException.BoundException.YouBound()
            } else {
                throw BindException.BoundException.UserBound(
                    boundUser?.username ?: bindUser.username, existQQ.qq
                )
            }
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
        if (RestrictUtils.isSuperAdmin(senderID)) {
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

        return REGEX_OSU_USERNAME.findAll(nickname)
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
            keywords = emptyList(),
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
            onFailure = {},
        )
    }
}