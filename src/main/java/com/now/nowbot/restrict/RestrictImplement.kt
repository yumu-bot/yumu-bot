package com.now.nowbot.restrict

import com.now.nowbot.qq.event.GroupMessageEvent
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.qq.tencent.TencentMessageService
import com.now.nowbot.restrict.RestrictTargetType.*
import com.now.nowbot.service.MessageService
import com.now.nowbot.util.AsyncMessageUtil
import com.now.nowbot.util.command.PATTERN_EXCLAMATION
import com.now.nowbot.util.command.PATTERN_IGNORE
import com.now.nowbot.util.command.PATTERN_SLASH
import com.now.nowbot.util.ContextUtil
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Clock

@Component
class RestrictImplement(
    private val restrictRecordRepository: RestrictRecordRepository
) {
    companion object {
        private val log = LoggerFactory.getLogger(RestrictImplement::class.java)

        @Volatile
        private var instance: RestrictImplement? = null

        // 全局超级管理员列表
        // 如果你已经看到这里了，就随便你改咯，反正我认为你应该能遵守我们的协议，对吧？

        val SUPER_USERS: Set<Long> = setOf(
            -17064371, 3228981717, 1340691940, 3145729213, 1563653406, 2070701187
        )

        // 服务映射表
        private val serviceMap = LinkedHashMap<String, MessageService<Any>>()
        private val serviceMap4TX = LinkedHashMap<String, TencentMessageService<Any>>()

        // 前置极速判断缓存：记录当前存在“有效封禁/屏蔽”的用户与群组 ID
        private val restrictedUsers = ConcurrentHashMap.newKeySet<Long>()
        private val restrictedGroups = ConcurrentHashMap.newKeySet<Long>()
        private val restrictedServices = ConcurrentHashMap.newKeySet<String>()

        // 指令快速匹配正则与前缀
        private val DICE_PATTERN = Regex("^($PATTERN_EXCLAMATION|$PATTERN_SLASH|(?<dice>\\d+))\\s*(?i)(ym)?(dice|roll|d(?!${PATTERN_IGNORE})).*").toPattern()
        private val PREFIX = "!！?？#＃/\\".toSet()

        /**
         * 极速消息预筛选器 (O(1) 过滤非指令消息)
         */
        private fun filterMessage(raw: String): Boolean {
            var i = 0
            while (i < raw.length && raw[i].isWhitespace()) i++
            if (i == raw.length) return false

            val firstChar = raw[i]
            if (firstChar in PREFIX) return true

            if (firstChar.isDigit()) {
                val matcher = DICE_PATTERN.matcher(raw)
                return matcher.region(i, raw.length).lookingAt()
            }
            return false
        }

        fun onMessage(event: MessageEvent, errorHandle: (MessageEvent, Throwable) -> Unit) {
            val delegate = instance ?: throw IllegalStateException("RestrictImplement 未被 Spring 初始化，请检查启动流程！")
            delegate.onMessage(event, errorHandle)
        }

        fun onTencentMessage(event: MessageEvent, onMessage: (MessageChain) -> Unit) {
            val delegate = instance ?: throw IllegalStateException("RestrictImplement 未被 Spring 初始化，请检查启动流程！")
            delegate.onTencentMessage(event, onMessage)
        }
    }

    /**
     * 应用启动时加载所有的服务与初始化内存黑名单缓存
     */
    @Synchronized
    fun init(services: Map<String, MessageService<*>>) {
        serviceMap.clear()
        serviceMap4TX.clear()

        @Suppress("UNCHECKED_CAST")
        services.asSequence().sortedBy { it.key }.forEach { (name, service) ->
            serviceMap[name] = service as MessageService<Any>
            if (service is TencentMessageService<*>) {
                serviceMap4TX[name] = service as TencentMessageService<Any>
            }
        }

        refreshCache()
        log.info("RestrictImplement 权限拦截服务初始化完成，受限用户数: {}, 受限群组数: {}", restrictedUsers.size, restrictedGroups.size)
    }

    @PostConstruct
    fun initSelf() {
        instance = this
    }

    /**
     * 从数据库刷新 restrictedUsers 和 restrictedGroups 内存缓存
     */
    fun refreshCache() {
        val now = Clock.System.now()
        val allEnabledRecords = restrictRecordRepository.findAllRestricting()

        // 过滤出当前时间点真正处于生效状态的记录
        val activeRecords = allEnabledRecords.filter { it.isCurrentlyActive(now) }

        restrictedUsers.clear()
        restrictedGroups.clear()
        restrictedServices.clear()

        activeRecords.forEach { record ->
            when (RestrictTargetType.getByIndex(record.targetType)) {
                USER -> restrictedUsers.add(record.targetID)
                GROUP -> restrictedGroups.add(record.targetID)
                ALL -> restrictedServices.add(record.service)
            }
        }
    }

    /**
     * 判断当前请求是否应该被拦截
     */
    fun isBlocked(userID: Long, groupID: Long?, service: String): Boolean {
        // 1. 超级管理员拥有绝对豁免权
        if (userID in SUPER_USERS) return false

        // 2. 极速前置过滤：如果用户和群聊都不在受限名单中，说明没有任何拦截规则，直接放行
        val isUserPossiblyRestricted = userID in restrictedUsers
        val isGroupPossiblyRestricted = groupID != null && groupID in restrictedGroups

        if (!isUserPossiblyRestricted && !isGroupPossiblyRestricted) {
            return false
        }

        // 3. 详细校验：查询数据库中的具体规则（包含 GLOBAL 与指定服务）
        val now = Clock.System.now()

        if (isUserPossiblyRestricted) {
            val userRules = restrictRecordRepository.findActiveRules(USER.byte, userID, service)
            if (userRules.any { it.isCurrentlyActive(now) }) return true
        }

        if (isGroupPossiblyRestricted) {
            val groupRules = restrictRecordRepository.findActiveRules(GROUP.byte, groupID, service)
            if (groupRules.any { it.isCurrentlyActive(now) }) return true
        }

        return false
    }

    /**
     * 标准消息监听入口
     */
    fun onMessage(event: MessageEvent, errorHandle: (MessageEvent, Throwable) -> Unit) {
        AsyncMessageUtil.put(event)
        val textMessage = event.textMessage

        if (!filterMessage(textMessage)) return

        val messageText = textMessage.trim()
        val uid = event.sender.contactID
        val gid = (event as? GroupMessageEvent)?.group?.contactID

        for ((name, service) in serviceMap) {
            try {
                if (checkStopListener()) break

                val data = MessageService.DataValue<Any>()

                if (service.isHandle(event, messageText, data)) {
                    // 核心拦截校验
                    if (isBlocked(uid, gid, name)) {
                        log.debug("请求被权限拦截: 服务名={}, 用户={}, 群组={}", name, uid, gid)
                        continue
                    }

                    service.handleMessage(event, data.value!!)
                    break
                }
            } catch (e: Throwable) {
                errorHandle(event, e)
                break
            }
        }
    }

    /**
     * 腾讯平台消息入口
     */
    fun onTencentMessage(event: MessageEvent, onMessage: (MessageChain) -> Unit) {
        val textMessage = event.textMessage
        if (!filterMessage(textMessage)) return

        val trim = textMessage.trim()
        val uid = event.sender.contactID
        val gid = (event as? GroupMessageEvent)?.group?.contactID

        for ((name, service) in serviceMap4TX) {
            try {
                val data = service.accept(event, trim) ?: continue

                if (isBlocked(uid, gid, name)) {
                    log.debug("腾讯消息请求被权限拦截: 服务名={}, 用户={}", name, uid)
                    onMessage(MessageChain("您或当前群聊已被限制使用 $name 服务。"))
                    return
                }

                val reply = service.reply(event, data) ?: MessageChain("服务 $name 无响应。")
                onMessage(reply)
                return
            } catch (e: Throwable) {
                // 异常处理逻辑同原有保持一致
                onMessage(MessageChain("服务 $name 运行出现异常。"))
                return
            }
        }
    }

    private fun checkStopListener(): Boolean {
        return ContextUtil.getContext("StopListener", Boolean::class.java) == true
    }
}