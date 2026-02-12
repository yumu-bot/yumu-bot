package com.now.nowbot.permission

interface PermissionController {
    /**
     * 功能开关控制
     * 
     * @param name 名
     * @param open true:开; false:关
     */
    fun serviceSwitch(name: String, open: Boolean)

    /**
     * 功能开关控制
     * 
     * @param name 名
     * @param open true:开; false:关
     * @param time 到指定时间撤销修改
     */
    fun serviceSwitch(name: String, open: Boolean, time: Long?)

    /**
     * 拉黑群
     * 
     * @param id 群id
     */
    fun blockGroup(id: Long)

    /**
     * 拉黑群一段时间
     * 
     * @param id   群id
     * @param time 时间, 单位毫秒
     */
    fun blockGroup(id: Long, time: Long?)

    fun blockGroup(service: String, id: Long)

    fun blockGroup(service: String, id: Long, time: Long?)

    fun unblockGroup(id: Long)

    fun unblockGroup(service: String, id: Long)

    fun unblockGroup(service: String, id: Long, time: Long?)

    /**
     * 拉黑个人
     * 
     * @param id qq
     */
    fun blockUser(id: Long)

    /**
     * 拉黑个人一段时间
     * 
     * @param id   qq
     * @param time 多少时间
     */
    fun blockUser(id: Long, time: Long?)

    fun blockUser(service: String, id: Long)

    fun blockUser(service: String, id: Long, time: Long?)

    fun unblockUser(id: Long)

    fun unblockUser(service: String, id: Long)

    fun unblockUser(service: String, id: Long, time: Long?)

    /* ****************************************************************** */
    /**
     * 忽略群
     * 
     * @param id 群号
     */
    fun ignoreAll(id: Long)

    fun ignoreAll(id: Long, time: Long?)

    fun ignoreAll(service: String, id: Long)

    fun ignoreAll(service: String, id: Long, time: Long?)

    /**
     * 取消忽略群
     * 
     * @param id 群号
     */
    fun unignoreAll(id: Long)

    fun unignoreAll(id: Long, time: Long?)

    fun unignoreAll(service: String, id: Long)

    fun unignoreAll(service: String, id: Long, time: Long?)

    fun queryAllBlock(): List<LockRecord>

    fun queryGlobal(): LockRecord?

    fun queryBlock(service: String): LockRecord?

    @JvmRecord
    data class LockRecord(
        val name: String?,
        val enable: Boolean,
        val groups: MutableSet<Long?>?,
        val users: MutableSet<Long?>?,
        val ignores: MutableSet<Long?>?
    )
}
