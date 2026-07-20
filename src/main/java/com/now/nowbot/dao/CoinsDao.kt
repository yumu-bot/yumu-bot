package com.now.nowbot.dao

import com.now.nowbot.entity.CoinsHistory
import com.now.nowbot.entity.UserCoins
import com.now.nowbot.mapper.CoinsHistoryRepository
import com.now.nowbot.mapper.UserCoinsRepository
import com.now.nowbot.model.enums.CoinsOperationType
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import kotlin.math.abs

@Component
class CoinsDao(
    private val userCoinsRepository: UserCoinsRepository,
    private val coinsHistoryRepository: CoinsHistoryRepository,
) {
    companion object {
        // 默认的金币池
        const val DEFAULT_ACCOUNT_TYPE = "GLOBAL"
        // 后续特别活动直接加常量
    }

    /**
     * 开户, 然后送点
     */
    @Transactional
    fun openAccount(uid: Long, initialCoins: Double): UserCoins =
        openAccount(uid, DEFAULT_ACCOUNT_TYPE, initialCoins)

    /**
     * 开户 (指定账户类型)
     */
    @Transactional
    fun openAccount(uid: Long, accountType: String, initialCoins: Double): UserCoins {
        val normalizedType = normalizeAccountType(accountType)

        userCoinsRepository.findByUidAndAccountType(uid, normalizedType)?.let { return it }

        val now = LocalDateTime.now()
        val normalized = abs(initialCoins)

        val account = UserCoins().apply {
            this.uid = uid
            this.accountType = normalizedType
            this.coins = normalized
            this.updateAt = now
        }

        val saved = userCoinsRepository.save(account)

        coinsHistoryRepository.save(
            CoinsHistory().apply {
                this.uid = uid
                this.accountType = normalizedType
                this.amount = CoinsOperationType.ACCOUNT_INIT_REWARD signed normalized
                this.balanceBefore = 0.0
                this.balanceAfter = normalized
                this.remark = CoinsOperationType.ACCOUNT_INIT_REWARD.description
                this.createdAt = now
            }
        )

        return saved
    }

    /**
     * 获取当前金币数
     */
    fun getCoins(uid: Long): Double? {
        return getCoins(uid, DEFAULT_ACCOUNT_TYPE)
    }

    fun getCoins(uid: Long, accountType: String): Double? {
        return userCoinsRepository.findByUidAndAccountType(uid, normalizeAccountType(accountType))?.coins
    }

    /**
     * 获取历史记录
     */
    fun getRecentHistory(uid: Long, limit: Int = 20): List<CoinsHistory> {
        return getRecentHistory(uid, DEFAULT_ACCOUNT_TYPE, limit)
    }

    fun getRecentHistory(uid: Long, accountType: String, limit: Int = 20): List<CoinsHistory> {
        return coinsHistoryRepository.findRecentByUidAndAccountType(
            uid,
            normalizeAccountType(accountType),
            PageRequest.of(0, limit.coerceAtLeast(1))
        )
    }

    /**
     * 同上, 就是加了起始时间
     */
    fun getRecentHistory(uid: Long, from: LocalDateTime, limit: Int = 20): List<CoinsHistory> {
        return getRecentHistory(uid, DEFAULT_ACCOUNT_TYPE, from, limit)
    }

    fun getRecentHistory(uid: Long, accountType: String, from: LocalDateTime, limit: Int = 20): List<CoinsHistory> {
        return coinsHistoryRepository.findRecentByUidAndAccountTypeFrom(
            uid,
            normalizeAccountType(accountType),
            from,
            PageRequest.of(0, limit.coerceAtLeast(1))
        )
    }

    /**
     * 执行操作
     */
    @Transactional
    fun operate(uid: Long, type: CoinsOperationType, coins: Double): CoinsHistory {
        return operate(uid, DEFAULT_ACCOUNT_TYPE, type, coins)
    }

    @Transactional
    fun operate(uid: Long, accountType: String, type: CoinsOperationType, coins: Double): CoinsHistory {
        val normalized = abs(coins)
        require(normalized > 0) { "coins must be greater than 0" }
        val normalizedType = normalizeAccountType(accountType)

        val now = LocalDateTime.now()

        val account = userCoinsRepository.findByUidAndAccountTypeForUpdate(uid, normalizedType)
            ?: throw IllegalArgumentException("user coins account not found: uid=$uid, accountType=$normalizedType")

        val before = account.coins ?: 0.0
        val delta = type signed normalized
        val after = before + delta

        account.coins = after
        account.updateAt = now

        userCoinsRepository.save(account)

        return coinsHistoryRepository.save(
            CoinsHistory().apply {
                this.uid = uid
                this.accountType = normalizedType
                this.amount = delta
                this.balanceBefore = before
                this.balanceAfter = after
                this.remark = type.description
                this.createdAt = now
            }
        )
    }

    /**
     * 回滚数据的, 万一什么时候爆了说不定用得到
     */
    @Transactional
    fun rollbackUserTo(uid: Long, time: LocalDateTime): Int {
        return rollbackUserTo(uid, DEFAULT_ACCOUNT_TYPE, time)
    }

    @Transactional
    fun rollbackUserTo(uid: Long, accountType: String, time: LocalDateTime): Int {
        val normalizedType = normalizeAccountType(accountType)

        val account = userCoinsRepository.findByUidAndAccountTypeForUpdate(uid, normalizedType) ?: return 0
        val rollbackHistories = coinsHistoryRepository.findAfterTimeByUidAndAccountType(uid, normalizedType, time)

        if (rollbackHistories.isEmpty()) return 0

        val rollbackBalance = rollbackHistories.minBy { it.createdAt }.balanceBefore

        account.coins = rollbackBalance
        account.updateAt = LocalDateTime.now()
        userCoinsRepository.save(account)

        coinsHistoryRepository.deleteByUidAndAccountTypeAfterTime(uid, normalizedType, time)

        return rollbackHistories.size
    }

    /**
     * 全爆了的回滚
     */
    @Transactional
    fun rollbackAllTo(time: LocalDateTime): Int {
        return rollbackAllTo(DEFAULT_ACCOUNT_TYPE, time)
    }

    @Transactional
    fun rollbackAllTo(accountType: String, time: LocalDateTime): Int {
        val normalizedType = normalizeAccountType(accountType)
        val rollbackHistories = coinsHistoryRepository.findAfterTimeByAccountType(normalizedType, time)
        if (rollbackHistories.isEmpty()) return 0

        val now = LocalDateTime.now()

        rollbackHistories.groupBy { it.uid }
            .forEach { (uid, histories) ->
                if (uid == null) return@forEach

                val rollbackBalance = histories.minBy { it.createdAt }.balanceBefore

                val account = userCoinsRepository.findByUidAndAccountType(uid, normalizedType) ?: UserCoins().apply {
                    this.uid = uid
                    this.accountType = normalizedType
                }

                account.coins = rollbackBalance
                account.updateAt = now

                userCoinsRepository.save(account)
            }

        coinsHistoryRepository.deleteAllByAccountTypeAfterTime(normalizedType, time)

        return rollbackHistories.size
    }

    private fun normalizeAccountType(accountType: String): String {
        return accountType.trim().ifBlank { DEFAULT_ACCOUNT_TYPE }
    }
}
