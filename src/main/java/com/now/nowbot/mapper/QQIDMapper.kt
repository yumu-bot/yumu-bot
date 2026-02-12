package com.now.nowbot.mapper

import com.now.nowbot.entity.QQID
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.transaction.annotation.Transactional

interface QQIDMapper : JpaRepository<QQID, Long>, JpaSpecificationExecutor<QQID> {
    fun getByPermissionID(permissionID: Long?): List<QQID>

    @Query("select id.QQ from QQID id where id.isGroup = true and id.permissionID = :pid")
    fun getGroupByPermissionID(pid: Long?): List<Long>

    @Modifying
    @Transactional
    fun deleteQQIDByPermissionIDAndIsGroupAndQQ(permissionID: Long?, isGroup: Boolean?, QQ: Long?)

    @Modifying
    @Transactional
    fun deleteQQIDByPermissionIDAndIsGroup(permissionID: Long?, isGroup: Boolean?)

    @Modifying
    @Transactional
    fun deleteQQIDByPermissionID(permissionID: Long?)

    @Modifying
    @Transactional
    fun deleteQQIDByQQAndIsGroup(QQ: Long, isGroup: Boolean)
}
