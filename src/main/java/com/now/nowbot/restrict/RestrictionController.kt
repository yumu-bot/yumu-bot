package com.now.nowbot.restrict

import com.now.nowbot.restrict.RestrictTargetType.*
import com.now.nowbot.throwable.botRuntimeException.PermissionException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant

@Service
class RestrictionController(
    private val restrictRecordRepository: RestrictRecordRepository
) {

    @Transactional
    fun findAllRestrictingByService(service: String): List<RestrictRecordEntity> {
        return restrictRecordRepository.findAllRestrictingByService(service)
    }

    @Transactional
    fun findAllRestricting(): List<RestrictRecordEntity> {
        return restrictRecordRepository.findAllRestricting()
    }

    /**
     * 校验用户/群组是否有权使用指定的 Service
     * 只要命中了任何一条“当前仍然在有效期限内”的封禁记录，即拒绝访问
     */
    @Transactional(readOnly = true)
    fun isBlocked(userID: Long, groupID: Long?, service: String): Boolean {
        val now = Clock.System.now()

        // 1. 检查针对用户的拦截（全局服务 + 当前特定服务）
        val userRules = restrictRecordRepository.findActiveRules(
            USER.byte, userID, service
        )

        if (userRules.any { it.isCurrentlyActive(now) }) {
            return true
        }

        // 2. 如果存在群组，检查针对群组的拦截
        if (groupID != null) {
            val groupRules = restrictRecordRepository.findActiveRules(
                GROUP.byte, groupID, service
            )
            if (groupRules.any { it.isCurrentlyActive(now) }) {
                return true
            }
        }

        return false
    }

    /**
     * 实施封禁 / 屏蔽
     * @param duration 封禁时长（毫秒），传递 null 代表无限期封禁
     * @param startTime 封禁生效时间点，默认为当前时间
     */
    @Transactional
    fun block(
        targetType: RestrictTargetType,
        targetID: Long,
        service: String = "GLOBAL",
        sourceType: RestrictSourceType,
        operatorID: Long,
        duration: Duration? = null,
        startTime: Instant = Clock.System.now(),
        reason: String? = null
    ) {

        val now = Clock.System.now()

        val existingRules = restrictRecordRepository.findActiveRulesExact(
            targetType.byte, targetID, service, sourceType.byte
        )

        // 过滤出当前时间点确实还在有效期内的记录
        val isAlreadyBlocked = existingRules.any { it.isCurrentlyActive(now) }

        if (isAlreadyBlocked) {
            when(targetType) {
                ALL -> throw PermissionException.BlackListException.Off(service)
                USER -> throw PermissionException.BlackListException.Duplicate(targetID)
                GROUP -> throw PermissionException.BlackListException.DuplicateGroup(targetID)
            }
        }

        // 2. 没有生效规则时，才插入新数据
        val record = RestrictRecordEntity(
            targetType = targetType.byte,
            targetID = targetID,
            service = service,
            sourceType = sourceType.byte,
            operatorID = operatorID,
            duration = duration?.inWholeMilliseconds,
            startTime = startTime,
            createdAt = now,
            reason = reason,
            enabled = true
        )
        restrictRecordRepository.save(record)
    }

    /**
     * 解封
     * 重点：解封需要指定 BlockSourceType。
     * - 用户执行“开启服务”时，只能传入 sourceType = USER_OPT_OUT，这只会软删除用户自己设定的关闭记录。
     * - 如果被管理员 ADMIN 封禁了，用户即便调用了开启，ADMIN 的记录依然在数据库中生效，用户仍然保持被封禁状态。
     */
    @Transactional
    fun unblock(
        targetType: RestrictTargetType,
        targetID: Long,
        service: String = "GLOBAL",
        sourceType: RestrictSourceType
    ) {
        val updatedCount = restrictRecordRepository.disableRules(
            targetType.byte, targetID, service, sourceType.byte
        )

        if (updatedCount == 0) {
            throw PermissionException.WhiteListException.On(service)
        }

        val now = Clock.System.now()

        val remainingActiveRules = restrictRecordRepository
            .findActiveRules(targetType.byte, targetID, service)
            .filter { it.isCurrentlyActive(now) }

        if (remainingActiveRules.isNotEmpty()) {
            // 如果还有生效记录，判断是否有来自管理员 (ADMIN) 的封禁
            val hasAdminBlock = remainingActiveRules.any { it.sourceType == RestrictSourceType.ADMIN.byte }

            if (hasAdminBlock) {
                when (targetType) {
                    ALL -> throw PermissionException.WhiteListException.Failed(service)
                    USER -> throw PermissionException.WhiteListException.Banned(targetID)
                    GROUP -> throw PermissionException.WhiteListException.BannedGroup(targetID)
                }
            } else {
                throw PermissionException.WhiteListException.Other(service)
            }
        }
    }
}