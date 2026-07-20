package com.now.nowbot.mapper

import com.now.nowbot.entity.CoinsHistory
import com.now.nowbot.entity.UserCoins
import com.now.nowbot.entity.UserCoinsKey
import jakarta.persistence.LockModeType
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

interface UserCoinsRepository : JpaRepository<UserCoins, UserCoinsKey> {
    fun findByUidAndAccountType(uid: Long, accountType: String): UserCoins?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from UserCoins u where u.uid = :uid and u.accountType = :accountType")
    fun findByUidAndAccountTypeForUpdate(uid: Long, accountType: String): UserCoins?

    @Transactional
    @Modifying
    @Query(
        value = """
            INSERT INTO user_coins (uid, account_type, coins, updated_at)
            VALUES (:uid, :accountType, :coins, :updatedAt)
            ON CONFLICT (uid, account_type)
            DO UPDATE SET
                coins = EXCLUDED.coins,
                updated_at = EXCLUDED.updated_at
        """,
        nativeQuery = true
    )
    fun upsert(uid: Long, accountType: String, coins: Double, updatedAt: LocalDateTime): Int
}

interface CoinsHistoryRepository : JpaRepository<CoinsHistory, Long> {
    @Query("select h from CoinsHistory h where h.uid = :uid and h.accountType = :accountType order by h.createdAt desc")
    fun findRecentByUidAndAccountType(uid: Long, accountType: String, pageable: Pageable): List<CoinsHistory>

    @Query("select h from CoinsHistory h where h.uid = :uid and h.accountType = :accountType and h.createdAt >= :from order by h.createdAt desc")
    fun findRecentByUidAndAccountTypeFrom(
        uid: Long,
        accountType: String,
        from: LocalDateTime,
        pageable: Pageable
    ): List<CoinsHistory>

    @Query("select h from CoinsHistory h where h.uid = :uid and h.accountType = :accountType and h.createdAt > :time order by h.createdAt asc")
    fun findAfterTimeByUidAndAccountType(uid: Long, accountType: String, time: LocalDateTime): List<CoinsHistory>

    @Query("select h from CoinsHistory h where h.accountType = :accountType and h.createdAt > :time order by h.createdAt asc")
    fun findAfterTimeByAccountType(accountType: String, time: LocalDateTime): List<CoinsHistory>

    @Transactional
    @Modifying
    @Query("delete from CoinsHistory h where h.uid = :uid and h.accountType = :accountType and h.createdAt > :time")
    fun deleteByUidAndAccountTypeAfterTime(uid: Long, accountType: String, time: LocalDateTime): Int

    @Transactional
    @Modifying
    @Query("delete from CoinsHistory h where h.accountType = :accountType and h.createdAt > :time")
    fun deleteAllByAccountTypeAfterTime(accountType: String, time: LocalDateTime): Int
}
