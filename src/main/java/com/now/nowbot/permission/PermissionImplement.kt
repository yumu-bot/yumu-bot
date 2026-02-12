package com.now.nowbot.permission

import com.now.nowbot.aop.CheckPermission
import com.now.nowbot.aop.ServiceOrder
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
import java.lang.reflect.Method
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
        private lateinit var AllService: PermissionService

        private val DICE_REGEX = Regex("^($REG_EXCLAMATION|$REG_SLASH|(?<dice>\\d+))\\s*(?i)(ym)?(dice|roll|d(?!${REG_IGNORE})).*")
        private const val PREFIX = "!！?？#＃/\\"

        /**
         * 极速™ 筛选器：
         * 消息预处理
         */
        private fun filterMessage(trimmed: String): Boolean {
            if (trimmed.isEmpty()) return false

            val firstChar = trimmed.first()

            // 情况1：以特殊符号开头
            if (firstChar in PREFIX) {
                return true
            }

            // 情况2：以数字开头，并且包含d或dice
            if (firstChar.isDigit()) {
                return trimmed.matches(DICE_REGEX)
            }

            return false
        }

        fun onMessage(event: MessageEvent, errorHandle: BiConsumer<MessageEvent, Throwable>) {
            ASyncMessageUtil.put(event)
            val textMessage = event.textMessage.trim()

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

                    if (typedService.isHandle(event, textMessage, data)) {
                        typedService.handleMessage(event, data.value!!)
                    }
                } catch (e: Throwable) {
                    errorHandle.accept(event, e)
                }
            }
        }

        fun onTencentMessage(event: MessageEvent, onMessage: Consumer<MessageChain>) {
            val textMessage = event.textMessage.trim()

            if (!filterMessage(textMessage)) {
                return
            }

            for ((_, service) in serviceMap4TX) {
                var reply: MessageChain?

                try {
                    val data = service.accept(event, textMessage) ?: continue

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

            onMessage.accept(MessageChain(IllegalArgumentException.WrongException.Instruction(textMessage)))
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
            val globalPermission = AllService


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
            // log.debug("没有找到对应的服务 {}, {}", name, permissionMap.size)
            throw RuntimeException("没有找到这个服务")
        }
    }

    //@Resource
    //private lateinit var permissionDao: PermissionDao

    //@Resource
    //private lateinit var serviceSwitchMapper: ServiceSwitchMapper

    @Suppress("UNCHECKED_CAST")
    fun init(services: Map<String, MessageService<*>>) {
        // 初始化全局服务控制
        val globalGroupList = permissionDao.getQQList(GLOBAL_PERMISSION, PermissionType.GROUP_B)
        val globalUserList = permissionDao.getQQList(GLOBAL_PERMISSION, PermissionType.FRIEND_B)
        AllService = PermissionService(false, false, false, globalGroupList, globalUserList, emptyList())

        // 初始化顺序
        val sortServiceMap = HashMap<String, Int>()
        services.forEach { (name, service) ->
            var beansCheck: CheckPermission?
            /*
             * 获取 service 的执行函数
             */
            var method: Method? = null
            for (m in AopUtils.getTargetClass(service).methods) {
                if (m.name == "handleMessage") method = m
            }
            // 必定获取到对应函数
            checkNotNull(method) { "未找到 handleMessage 方法" }

            // 处理排序
            val sort = method.getAnnotation(ServiceOrder::class.java)
            if (sort == null) {
                sortServiceMap[name] = 0
            } else {
                sortServiceMap[name] = sort.sort
            }

            // 处理权限注解
            beansCheck = method.getAnnotation(CheckPermission::class.java)
            if (beansCheck == null) {
                try {
                    beansCheck = Permission::class.java.getDeclaredMethod("CheckPermission").getAnnotation(CheckPermission::class.java)
                } catch (_: NoSuchMethodException) {

                }
            }
            // 必定有对应注解
            checkNotNull(beansCheck) { "未找到 CheckPermission 注解" }

            if (beansCheck.isSuperAdmin) {
                superService.add(name)
                return@forEach
            }

            val groups = permissionDao.getQQList(name, if (beansCheck.isWhite) PermissionType.GROUP_W else PermissionType.GROUP_B)
            val users = permissionDao.getQQList(name, if (beansCheck.isWhite) PermissionType.FRIEND_W else PermissionType.FRIEND_B)
            val groupsSelf = permissionDao.getQQList(name, if (beansCheck.isWhite) PermissionType.FRIEND_W else PermissionType.GROUP_SELF_B)

            val param = PermissionService(beansCheck.userWhite, beansCheck.groupWhite, beansCheck.userSet, groups, users, groupsSelf)
            permissionMap[name] = param
        }

        sortServiceMap.entries
            .sortedByDescending { it.value }
            .map { it.key }
            .forEach { name ->
                val service = services[name]?.apply {
                    servicesMap[name] = this
                }

                if (service is TencentMessageService<*>) {
                    @Suppress("UNCHECKED_CAST")
                    serviceMap4TX[name] = service as TencentMessageService<Any>
                }
            }

        /*
        // 处理完服务排序
        sortServiceMap.entries
            .stream()
            .sorted(Comparator.comparingInt(ToIntFunction<Map.Entry<String, Int>> { it.value }.reversed()))
            .map { it.key }
            .forEach { name ->
                val service = services[name]
                servicesMap[name] = service
                if (service is TencentMessageService<*>) {
                    @Suppress("UNCHECKED_CAST")
                    serviceMap4TX[name] = service as TencentMessageService<Any>
                }
            }

         */

        // 初始化暗杀名单
        superList = setOf(732713726L, 3228981717L, 1340691940L, 3145729213L, 365246692L, 2480557535L, 1968035918L, 2429299722L, 447503971L, LOCAL_GROUP_ID)

        log.info("权限模块初始化完成")
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
    }

    override fun serviceSwitch(name: String, open: Boolean, time: Long?) {
        serviceSwitch(name, open)
        if (time != null) {
            futureMap.computeIfPresent(name, this::cancelFuture)
            val future = EXECUTOR.schedule({ serviceSwitch(name, !open) }, time, TimeUnit.MILLISECONDS)
            futureMap[name] = future
        }
    }

    override fun blockGroup(id: Long) {
        blockService(GLOBAL_PERMISSION, AllService, id, true, null)
    }

    override fun blockGroup(id: Long, time: Long?) {
        blockService(GLOBAL_PERMISSION, AllService, id, true, time)
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
        unblockService(GLOBAL_PERMISSION, AllService, id, true, null)
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
        blockService(GLOBAL_PERMISSION, AllService, id, false, null)
    }

    override fun blockUser(id: Long, time: Long?) {
        blockService(GLOBAL_PERMISSION, AllService, id, false, time)
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
        unblockService(GLOBAL_PERMISSION, AllService, id, false, null)
    }

    override fun unblockUser(service: String, id: Long) {
        val record = getService(service)
        unblockService(record.name, record.permission, id, false, null)
    }

    override fun unblockUser(service: String, id: Long, time: Long?) {
        val record = getService(service)
        unblockService(record.name, record.permission, id, false, time)
    }

    override fun clear(isGroup: Boolean, id: Long, time: Long?) {
        val perm = AllService

        val index = if (isGroup) "g" else "u"
        val key = "$GLOBAL_PERMISSION:${index}0"
        if (time != null && time > 0) {
            futureMap[key] = EXECUTOR.schedule({ restrict(isGroup, id, time) }, time, TimeUnit.MILLISECONDS)
        } else {
            futureMap.computeIfPresent(key, this::cancelFuture)
        }

        val all = servicesMap.map { it.key } + GLOBAL_PERMISSION

        all.forEach { name ->
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
        }
    }

    override fun restrict(isGroup: Boolean, id: Long, time: Long?) {
        val perm = AllService

        val index = if (isGroup) "g" else "u"
        val key = "$GLOBAL_PERMISSION:${index}0"
        if (time != null && time > 0) {
            futureMap[key] = EXECUTOR.schedule({ clear(isGroup, id, time) }, time, TimeUnit.MILLISECONDS)
        } else {
            futureMap.computeIfPresent(key, this::cancelFuture)
        }

        val all = servicesMap.map { it.key } + GLOBAL_PERMISSION

        all.forEach { name ->
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
        }
    }

    override fun ignoreAll(id: Long) {
        blockServiceSelf(GLOBAL_PERMISSION, AllService, id, null)
    }

    override fun ignoreAll(id: Long, time: Long?) {
        blockServiceSelf(GLOBAL_PERMISSION, AllService, id, time)
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
        unblockServiceSelf(GLOBAL_PERMISSION, AllService, id, null)
    }

    override fun unignoreAll(id: Long, time: Long?) {
        unblockServiceSelf(GLOBAL_PERMISSION, AllService, id, time)
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
        val result = ArrayList<PermissionController.LockRecord>(permissionMap.size)
        permissionMap.forEach { (name, p) ->
            result.add(
                PermissionController.LockRecord(
                    name,
                    !p.isDisable,
                    p.groupList ?: HashSet(),
                    p.userList ?: HashSet(),
                    p.groupSelfBlackList ?: HashSet()
                )
            )
        }
        return result
    }

    override fun queryGlobal(): PermissionController.LockRecord {
        return PermissionController.LockRecord(
            GLOBAL_PERMISSION,
            true,
            AllService.groupList,
            AllService.userList,
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