package com.now.nowbot.mapper

import com.now.nowbot.entity.QQID
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.beans.Transient

interface QQIDMapper : JpaRepository<QQID?, Long?>, JpaSpecificationExecutor<QQID> {
    fun getByPermissionID(permissionID: Long?): List<QQID>

    @Modifying
    @Transient
    fun deleteQQIDByPermissionIDAndIsGroupAndQQ(permissionID: Long?, isGroup: Boolean?, QQ: Long?)

    @Modifying
    @Transient
    fun deleteQQIDByPermissionIDAndIsGroup(permissionID: Long?, isGroup: Boolean?)

    @Query("select id.QQ from QQID id where id.isGroup = true and id.permissionID = :pid")
    fun getQQIDByPermissionID(pid: Long?): List<Long>
}
