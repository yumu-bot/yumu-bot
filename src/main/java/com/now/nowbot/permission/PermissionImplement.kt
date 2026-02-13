package com.now.nowbot.permission

import com.now.nowbot.aop.CheckPermission
import com.now.nowbot.config.AsyncSetting
import com.now.nowbot.config.Permission
import com.now.nowbot.dao.PermissionDao
import com.now.nowbot.entity.ServiceSwitchLite
import com.now.nowbot.mapper.ServiceSwitchMapper
import com.now.nowbot.qq.event.GroupMessageEvent
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.qq.tencent.TencentMessageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.throwable.BotException
import com.now.nowbot.throwable.botRuntimeException.IllegalArgumentException
import com.now.nowbot.throwable.botRuntimeException.NetworkException
import com.now.nowbot.util.ASyncMessageUtil
import com.now.nowbot.util.ContextUtil
import com.now.nowbot.util.command.REG_EXCLAMATION
import com.now.nowbot.util.command.REG_IGNORE
import com.now.nowbot.util.command.REG_SLASH
import org.slf4j.LoggerFactory
import org.springframework.aop.support.AopUtils
import org.springframework.stereotype.Component
import java.util.concurrent.*
import java.util.function.BiConsumer
import java.util.function.Consumer

@Component
class PermissionImplement(
    private val permissionDao: PermissionDao,
    private val serviceSwitchMapper: ServiceSwitchMapper,
) : PermissionController {
    companion object {
        private val log = LoggerFactory.getLogger(PermissionImplement::class.java)
        private val EXECUTOR = Executors.newScheduledThreadPool(Int.MAX_VALUE, AsyncSetting.THREAD_FACTORY)
        const val GLOBAL_PERMISSION = "PERMISSION_ALL"
        private const val LOCAL_GROUP_ID = -10086L

        private val superService = CopyOnWriteArraySet<String>()
        private val permissionMap = LinkedHashMap<String, PermissionService>()
        private val servicesMap = LinkedHashMap<String, MessageService<*>>()
        private val serviceMap4TX = LinkedHashMap<String, TencentMessageService<*>>()
        private val futureMap = ConcurrentHashMap<String, ScheduledFuture<*>>()

        private lateinit var superList: Set<Long>
        // private lateinit var testerList: Set<Long>
        @Volatile
        private lateinit var GlobalService: PermissionService

        private val DICE_PATTERN = Regex("^($REG_EXCLAMATION|$REG_SLASH|(?<dice>\\d+))\\s*(?i)(ym)?(dice|roll|d(?!${REG_IGNORE})).*").toPattern()
        private val PREFIX = "!！?？#＃/\\".toSet()

        /**
         * 极速™ v2 筛选器：
         * 高性能的消息预处理
         */
        private fun filterMessage(raw: String): Boolean {
            // 1. 手动扫描，零对象创建
            var i = 0
            while (i < raw.length && raw[i].isWhitespace()) i++
            if (i == raw.length) return false

            val firstChar = raw[i]

            // 2. 符号前缀 O(1) 过滤
            if (firstChar in PREFIX) return true

            // 3. 数字开头正则：使用 region 限制范围，避免全文搜索
            if (firstChar.isDigit()) {
                val matcher = DICE_PATTERN.matcher(raw)
                // 设置搜索区域并从该处开始尝试匹配前缀
                return matcher.region(i, raw.length).lookingAt()
            }

            return false
        }

        fun onMessage(event: MessageEvent, errorHandle: BiConsumer<MessageEvent, Throwable>) {
            ASyncMessageUtil.put(event)
            val textMessage = event.textMessage

            if (!filterMessage(textMessage)) {
                return
            }

            servicesMap.forEach { (serviceName, service) ->
                try {
                    // 服务截止
                    if (checkStopListener()) {
                        return@forEach
                    }
                    // super 用户不受检查
                    // 是否在黑名单内
                    if (!isSuper(event.sender.id) && isBlock(serviceName, event)) {
                        // 被黑名单禁止
                        log.debug("黑名单禁止, 请求功能: {} ,请求人: {}", serviceName, event.sender.id)
                        return@forEach
                    }

                    @Suppress("UNCHECKED_CAST")
                    val typedService = service as MessageService<Any>

                    val data = MessageService.DataValue<Any>()

                    if (typedService.isHandle(event, textMessage.trim(), data)) {
                        typedService.handleMessage(event, data.value!!)
                    }
                } catch (e: Throwable) {
                    errorHandle.accept(event, e)
                }
            }
        }

        fun onTencentMessage(event: MessageEvent, onMessage: Consumer<MessageChain>) {
            val textMessage = event.textMessage

            if (!filterMessage(textMessage)) {
                return
            }

            for ((_, service) in serviceMap4TX) {
                var reply: MessageChain?

                try {
                    val data = service.accept(event, textMessage.trim()) ?: continue

                    @Suppress("UNCHECKED_CAST")
                    val typedService = service as TencentMessageService<Any>
                    reply = typedService.reply(event, data)
                } catch (e: Throwable) {
                    reply = when (e) {
                        is BotException -> MessageChain(e.message ?: "错误")
                        is ExecutionException -> MessageChain(e.cause?.message ?: "错误")
                        is TimeoutException -> MessageChain("超时了。")
                        else -> {
                            log.error("腾讯消息类：其他错误", e)
                            continue
                        }
                    }
                }

                if (reply == null) {
                    reply = MessageChain(NetworkException.ComponentException.NoResponse())
                }
                onMessage.accept(reply)
                return
            }

            onMessage.accept(MessageChain(IllegalArgumentException.WrongException.Instruction(textMessage.trim())))
        }

        private fun checkStopListener(): Boolean {
            return ContextUtil.getContext("StopListener", Boolean::class.java) == true
        }

        private fun isSuper(id: Long): Boolean {
            return superList.contains(id)
        }

        private fun isBlock(name: String, event: MessageEvent): Boolean {
            if (superService.contains(name)) return true
            val record = getService(name)
            val servicePermission = record.permission
            val globalPermission = GlobalService


            return if (event is GroupMessageEvent) {
                val gid = event.group.id
                val uid = event.sender.id
                ! (globalPermission.check(gid, uid) && servicePermission.check(gid, uid))
            } else {
                val uid = event.sender.id
                ! (globalPermission.check(null, uid) && servicePermission.check(null, uid))
            }
        }

        private fun getService(name: String): PermissionRecord {
            for ((key, value) in permissionMap) {
                if (name.equals(key, ignoreCase = true)) {
                    return PermissionRecord.fromEntry(key to value)
                }
            }
            log.info("没有找到对应的服务 {}, {}", name, permissionMap.size)
            throw RuntimeException("没有找到这个服务")
        }
    }

    private sealed class ServiceType {
        // 超级管理员服务
        object Super : ServiceType()
        // 普通权限控制服务 (携带解析好的权限数据)
        data class Normal(val param: PermissionService) : ServiceType()
        // 无需权限控制的服务
        object None : ServiceType()
    }

    @Suppress("UNCHECKED_CAST")
    fun init(services: Map<String, MessageService<*>>) {
        // 初始化全局服务控制
        val globalGroupList = permissionDao.getQQs(GLOBAL_PERMISSION, PermissionType.GROUP_B)
        val globalUserList = permissionDao.getQQs(GLOBAL_PERMISSION, PermissionType.FRIEND_B)
        GlobalService = PermissionService(false, false, false, globalGroupList, globalUserList, emptyList())

        // 初始化顺序
        services.asSequence().sortedByDescending { it.key }.forEach { (name, service) ->
            servicesMap[name] = service

            if (service is TencentMessageService<*>) {
                @Suppress("UNCHECKED_CAST")
                serviceMap4TX[name] = service as TencentMessageService<Any>
            }

            refreshPermissionCache(name)
        }

        // 初始化暗杀名单
        superList = setOf(732713726L, 3228981717L, 1340691940L, 3145729213L, 365246692L, 2480557535L, 1968035918L, 2429299722L, 447503971L, LOCAL_GROUP_ID)

        log.info("权限模块初始化完成")
    }

    private fun refreshGlobalService() {
        val globalGroupList = permissionDao.getQQs(GLOBAL_PERMISSION, PermissionType.GROUP_B)
        val globalUserList = permissionDao.getQQs(GLOBAL_PERMISSION, PermissionType.FRIEND_B)

        // 更新全局单例对象
        GlobalService = PermissionService(
            false, false, false,
            globalGroupList, globalUserList, emptyList()
        )
    }

    private fun resolveServiceType(name: String, service: MessageService<*>): ServiceType {
        val targetClass = AopUtils.getTargetClass(service)
        // 找到 handleMessage 方法
        val method = targetClass.methods.find { it.name == "handleMessage" }
            ?: return ServiceType.None

        // 获取注解：方法优先，没有则取默认
        val check = method.getAnnotation(CheckPermission::class.java) ?: run {
            try {
                Permission::class.java.getDeclaredMethod("CheckPermission")
                    .getAnnotation(CheckPermission::class.java)
            } catch (_: Exception) { null }
        } ?: return ServiceType.None

        // 关键判断：是否为超级管理员服务
        if (check.isSuperAdmin) {
            return ServiceType.Super
        }

        // 如果不是超管，则加载数据库权限数据
        val groups = permissionDao.getQQs(name, if (check.isWhite) PermissionType.GROUP_W else PermissionType.GROUP_B)
        val users = permissionDao.getQQs(name, if (check.isWhite) PermissionType.FRIEND_W else PermissionType.FRIEND_B)
        val groupsSelf = permissionDao.getQQs(name, if (check.isWhite) PermissionType.FRIEND_W else PermissionType.GROUP_SELF_B)

        return ServiceType.Normal(
            PermissionService(check.userWhite, check.groupWhite, check.userSet, groups, users, groupsSelf)
        )
    }

    /**
     * 刷新指定服务的内存缓存
     * 当数据库中的权限（qq_id表）发生变动时调用此方法
     */
    fun refreshPermissionCache(name: String) {
        if (name == GLOBAL_PERMISSION) {
            refreshGlobalService()
            return
        }

        val service = servicesMap[name] ?: return
        when (val type = resolveServiceType(name, service)) {
            is ServiceType.Super -> {
                superService.add(name)
                permissionMap.remove(name)
            }
            is ServiceType.Normal -> {
                permissionMap[name] = type.param
                superService.remove(name)
            }
            is ServiceType.None -> {
                permissionMap.remove(name)
                superService.remove(name)
            }
        }
    }

    /**
     * 全局刷新，非必要别用
     */
    fun refreshAllPermissions() {
        servicesMap.keys.forEach { name ->
            refreshPermissionCache(name)
        }
    }

    /**
     * 锁定
     *
     * @param id      id
     * @param isGroup 是否为群
     * @param time    撤销时间
     */
    private fun blockService(name: String, perm: PermissionService, id: Long, isGroup: Boolean, time: Long?) {
        val index = if (isGroup) "g" else "u"
        val key = "$name:$index$id"
        if (time != null) {
            futureMap[key] = EXECUTOR.schedule({ unblockService(name, perm, id, isGroup, null) }, time, TimeUnit.MILLISECONDS)
        } else {
            futureMap.computeIfPresent(key, this::cancelFuture)
        }
        if (isGroup) {
            if (perm.isGroupWhite) {
                permissionDao.addGroup(name, PermissionType.GROUP_W, id)
            } else {
                permissionDao.addGroup(name, PermissionType.GROUP_B, id)
            }
            perm.addGroup(id)
        } else {
            if (perm.isUserWhite) {
                permissionDao.addUser(name, PermissionType.FRIEND_W, id)
            } else {
                permissionDao.addUser(name, PermissionType.FRIEND_B, id)
            }
            perm.addUser(id)
        }

        refreshPermissionCache(name)
    }

    /**
     * 解锁
     *
     * @param id      id
     * @param isGroup 是否为群
     * @param time    撤销时间
     */
    private fun unblockService(name: String, perm: PermissionService, id: Long, isGroup: Boolean, time: Long?) {
        val index = if (isGroup) "g" else "u"
        val key = "$name:$index$id"
        if (time != null && time > 0) {
            futureMap[key] = EXECUTOR.schedule({ blockService(name, perm, id, isGroup, null) }, time, TimeUnit.MILLISECONDS)
        } else {
            futureMap.computeIfPresent(key, this::cancelFuture)
        }

        if (isGroup) {
            if (perm.isGroupWhite) {
                permissionDao.deleteGroup(name, PermissionType.GROUP_W, id)
            } else {
                permissionDao.deleteGroup(name, PermissionType.GROUP_B, id)
            }
            perm.deleteGroup(id)
        } else {
            if (perm.isUserWhite) {
                permissionDao.deleteUser(name, PermissionType.FRIEND_W, id)
            } else {
                permissionDao.deleteUser(name, PermissionType.FRIEND_B, id)
            }
            perm.deleteUser(id)
        }

        refreshPermissionCache(name)
    }

    private fun blockServiceSelf(name: String, perm: PermissionService, id: Long, time: Long?) {
        val key = "$name:sg$id"
        if (time != null) {
            futureMap[key] = EXECUTOR.schedule({ unblockServiceSelf(name, perm, id, null) }, time, TimeUnit.MILLISECONDS)
        } else {
            futureMap.computeIfPresent(key, this::cancelFuture)
        }
        permissionDao.addGroup(name, PermissionType.GROUP_SELF_B, id)
        perm.addSelfGroup(id)

        refreshPermissionCache(name)
    }

    private fun unblockServiceSelf(name: String, perm: PermissionService, id: Long, time: Long?) {
        val key = "$name:sg$id"
        if (time != null) {
            futureMap[key] = EXECUTOR.schedule({ blockServiceSelf(name, perm, id, null) }, time, TimeUnit.MILLISECONDS)
        } else {
            futureMap.computeIfPresent(key, this::cancelFuture)
        }
        permissionDao.deleteGroup(name, PermissionType.GROUP_SELF_B, id)
        perm.deleteSelfGroup(id)

        refreshPermissionCache(name)
    }

    data class PermissionRecord(val name: String, val permission: PermissionService) {
        companion object {
            fun fromEntry(entry: Pair<String, PermissionService>): PermissionRecord {
                return PermissionRecord(entry.first, entry.second)
            }
        }
    }

    override fun serviceSwitch(name: String, open: Boolean) {
        val record = getService(name)
        serviceSwitchMapper.save(ServiceSwitchLite(name, open))
        record.permission.setEnable(open)
        futureMap.computeIfPresent(name, this::cancelFuture)
        refreshPermissionCache(name)
    }

    override fun serviceSwitch(name: String, open: Boolean, time: Long?) {
        serviceSwitch(name, open)
        if (time != null) {
            futureMap.computeIfPresent(name, this::cancelFuture)
            val future = EXECUTOR.schedule({ serviceSwitch(name, !open) }, time, TimeUnit.MILLISECONDS)
            futureMap[name] = future
        }
        refreshPermissionCache(name)
    }

    override fun blockGroup(id: Long) {
        blockService(GLOBAL_PERMISSION, GlobalService, id, true, null)
    }

    override fun blockGroup(id: Long, time: Long?) {
        blockService(GLOBAL_PERMISSION, GlobalService, id, true, time)
    }

    override fun blockGroup(service: String, id: Long) {
        val record = getService(service)
        blockService(record.name, record.permission, id, true, null)
    }

    override fun blockGroup(service: String, id: Long, time: Long?) {
        val record = getService(service)
        blockService(record.name, record.permission, id, true, time)
    }

    override fun unblockGroup(id: Long) {
        unblockService(GLOBAL_PERMISSION, GlobalService, id, true, null)
    }

    override fun unblockGroup(service: String, id: Long) {
        val record = getService(service)
        unblockService(record.name, record.permission, id, true, null)
    }

    override fun unblockGroup(service: String, id: Long, time: Long?) {
        val record = getService(service)
        unblockService(record.name, record.permission, id, true, time)
    }

    override fun blockUser(id: Long) {
        blockService(GLOBAL_PERMISSION, GlobalService, id, false, null)
    }

    override fun blockUser(id: Long, time: Long?) {
        blockService(GLOBAL_PERMISSION, GlobalService, id, false, time)
    }

    override fun blockUser(service: String, id: Long) {
        val record = getService(service)
        blockService(record.name, record.permission, id, false, null)
    }

    override fun blockUser(service: String, id: Long, time: Long?) {
        val record = getService(service)
        blockService(record.name, record.permission, id, false, time)
    }

    override fun unblockUser(id: Long) {
        unblockService(GLOBAL_PERMISSION, GlobalService, id, false, null)
    }

    override fun unblockUser(service: String, id: Long) {
        val record = getService(service)
        unblockService(record.name, record.permission, id, false, null)
    }

    override fun unblockUser(service: String, id: Long, time: Long?) {
        val record = getService(service)
        unblockService(record.name, record.permission, id, false, time)
    }


    override fun clearUser(id: Long) {
        permissionDao.deleteQQAll(id, false)

        refreshGlobalService()
        refreshAllPermissions()
    }

    override fun clearGroup(id: Long) {
        permissionDao.deleteQQAll(id, true)

        refreshGlobalService()
        refreshAllPermissions()
    }

    override fun ignoreAll(id: Long) {
        blockServiceSelf(GLOBAL_PERMISSION, GlobalService, id, null)
    }

    override fun ignoreAll(id: Long, time: Long?) {
        blockServiceSelf(GLOBAL_PERMISSION, GlobalService, id, time)
    }

    override fun ignoreAll(service: String, id: Long) {
        val record = getService(service)
        blockServiceSelf(record.name, record.permission, id, null)
    }

    override fun ignoreAll(service: String, id: Long, time: Long?) {
        val record = getService(service)
        blockServiceSelf(record.name, record.permission, id, time)
    }

    override fun unignoreAll(id: Long) {
        unblockServiceSelf(GLOBAL_PERMISSION, GlobalService, id, null)
    }

    override fun unignoreAll(id: Long, time: Long?) {
        unblockServiceSelf(GLOBAL_PERMISSION, GlobalService, id, time)
    }

    override fun unignoreAll(service: String, id: Long) {
        val record = getService(service)
        unblockServiceSelf(record.name, record.permission, id, null)
    }

    override fun unignoreAll(service: String, id: Long, time: Long?) {
        val record = getService(service)
        unblockServiceSelf(record.name, record.permission, id, time)
    }

    @Suppress("UNUSED")
    private fun cancelFuture(name: String, future: ScheduledFuture<*>): ScheduledFuture<*>? {
        future.cancel(true)
        return null
    }

    /**
     * 全局的在 queryGlobal 查看
     */
    override fun queryAllBlock(): List<PermissionController.LockRecord> {
        return permissionMap.map { (name, p) ->
            PermissionController.LockRecord(
                name,
                !p.isDisable,
                p.groupList ?: HashSet(),
                p.userList ?: HashSet(),
                p.groupSelfBlackList ?: HashSet()
            )
        }
    }

    override fun queryGlobal(): PermissionController.LockRecord {
        return PermissionController.LockRecord(
            GLOBAL_PERMISSION,
            true,
            GlobalService.groupList,
            GlobalService.userList,
            HashSet()
        )
    }

    override fun queryBlock(service: String): PermissionController.LockRecord {
        val p = getService(service)

        return PermissionController.LockRecord(
            p.name,
            !p.permission.isDisable,
            p.permission.groupList ?: HashSet(),
            p.permission.userList ?: HashSet(),
            p.permission.groupSelfBlackList ?: HashSet()
        )
    }
}