package com.now.nowbot.dao

import com.now.nowbot.entity.PermissionLite
import com.now.nowbot.entity.QQID
import com.now.nowbot.mapper.PermissionMapper
import com.now.nowbot.mapper.QQIDMapper
import com.now.nowbot.permission.PermissionType
import org.springframework.stereotype.Component

@Component
class PermissionDao(
    private val permissionMapper: PermissionMapper,
    private val qqMapper: QQIDMapper
) {

    /**
     * 是用户还是群取决于 type
     */
    fun getQQs(service: String?, type: PermissionType?): List<Long> {
        val perm = permissionMapper.getByServiceAndType(service, type) ?: permissionMapper.save(PermissionLite(service, type))
        return qqMapper.getByPermissionID(perm.id)
            .filter {
                when(type) {
                    PermissionType.GROUP_SELF_B, PermissionType.GROUP_B, PermissionType.GROUP_W -> it.isGroup == true
                    else -> it.isGroup != true
                }
            }
            .mapNotNull { it.QQ }
    }

    fun addGroup(service: String, type: PermissionType?, id: Long?) {
        val pid = permissionMapper.getPermissionID(service, type)
        val data = QQID()
        data.isGroup = true
        data.permissionID = pid
        data.QQ = id
        qqMapper.saveAndFlush(data)
    }

    fun getGroups(service: String, type: PermissionType): List<Long> {
        val pid = permissionMapper.getPermissionID(service, type)
        return qqMapper.getGroupByPermissionID(pid)
    }

    fun deleteGroup(service: String?, type: PermissionType?, id: Long?) {
        val pid = permissionMapper.getPermissionID(service, type)
        qqMapper.deleteQQIDByPermissionIDAndIsGroupAndQQ(pid, true, id)
    }

    fun addUser(service: String?, type: PermissionType?, id: Long?) {
        val pid = permissionMapper.getPermissionID(service, type)
        val data = QQID()
        data.isGroup = false
        data.permissionID = pid
        data.QQ = id
        qqMapper.saveAndFlush(data)
    }

    fun deleteUser(service: String?, type: PermissionType?, id: Long?) {
        val pid = permissionMapper.getPermissionID(service, type)
        qqMapper.deleteQQIDByPermissionIDAndIsGroupAndQQ(pid, false, id)
    }

    fun deleteQQAll(qq: Long, isGroup: Boolean) {
        qqMapper.deleteQQIDByQQAndIsGroup(qq, isGroup)
    }

    fun deleteAllQQ(service: String?, type: PermissionType?) {
        val pid = permissionMapper.getPermissionID(service, type)
        qqMapper.deleteQQIDByPermissionID(pid)
    }
}
