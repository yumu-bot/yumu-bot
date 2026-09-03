package com.now.nowbot.restrict

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface RestrictRecordRepository: JpaRepository<RestrictRecordEntity, Long> {

    @Query("SELECT r FROM RestrictRecordEntity r WHERE r.enabled = true AND r.service = :service")
    fun findAllRestrictingByService(service: String): List<RestrictRecordEntity>

    @Query("SELECT r FROM RestrictRecordEntity r WHERE r.enabled = true")
    fun findAllRestricting(): List<RestrictRecordEntity>

    /**
     * 查询匹配的目标（个人/群组）和特定服务（或 GLOBAL）的所有激活状态的规则
     */
    @Query("""
        SELECT b FROM RestrictRecordEntity b 
        WHERE b.enabled = true 
        AND b.targetType = :targetType 
        AND b.targetID = :targetId 
        AND b.service IN (:service, 'GLOBAL')
    """)
    fun findActiveRules(
        targetType: Byte,
        targetId: Long,
        service: String
    ): List<RestrictRecordEntity>

    @Query("""
    SELECT r FROM RestrictRecordEntity r 
    WHERE r.enabled = true 
    AND r.targetType = :targetType 
    AND r.targetID = :targetId 
    AND r.service = :service
    AND r.sourceType = :sourceType
""")
    fun findActiveRulesExact(
        targetType: Byte,
        targetId: Long,
        service: String,
        sourceType: Byte
    ): List<RestrictRecordEntity>

    /**
     * 按来源解封（比如管理员解封，或者普通用户自己重新开启服务）
     * 这能防止普通用户调用解除接口时取消了管理员的 ADMIN 封禁
     */
    @Modifying
    @Query("""
        UPDATE RestrictRecordEntity b 
        SET b.enabled = false 
        WHERE b.targetType = :targetType 
        AND b.targetID = :targetId 
        AND b.service = :service
        AND b.sourceType = :sourceType 
        AND b.enabled = true
    """)
    fun disableRules(
        targetType: Byte,
        targetID: Long,
        service: String,
        sourceType: Byte
    ): Int
}