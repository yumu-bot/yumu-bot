package com.now.nowbot.mapper

import com.now.nowbot.entity.PermissionLite
import com.now.nowbot.permission.PermissionType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.transaction.annotation.Transactional


interface PermissionMapper : JpaRepository<PermissionLite, Long>, JpaSpecificationExecutor<PermissionLite> {
    @Query("select p.id from PermissionLite p where p.service = :service and p.type = :type")
    fun getPermissionID(@Param("service") service: String?, @Param("type") type: PermissionType?): Long?

    fun getByServiceAndType(service: String?, type: PermissionType?): PermissionLite?

    @Transactional
    @Modifying
    fun deleteByServiceAndType(service: String?, type: PermissionType?)
}
