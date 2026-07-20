package com.now.nowbot.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "user_coins")
@IdClass(UserCoinsKey::class)
class UserCoins {
    @Id
    @Column(name = "uid")
    var uid: Long? = null

    @Id
    @Column(name = "account_type", columnDefinition = "text")
    var accountType: String = "GLOBAL"

    @Column(name = "coins")
    var coins: Double? = null

    @Column(name = "updated_at", nullable = false, updatable = true)
    var updateAt: LocalDateTime = LocalDateTime.now()
}


@Entity
@Table(name = "coins_history")
class CoinsHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null

    @Column(name = "uid")
    var uid: Long? = null

    @Column(name = "account_type", columnDefinition = "text")
    var accountType: String = "GLOBAL"

    // 负数就是扣, 正数就是加
    @Column(name = "coins")
    var amount: Double = .0

    @Column(name = "balance_before")
    var balanceBefore: Double = .0

    @Column(name = "balance_after")
    var balanceAfter: Double = .0

    @Column(name = "remark", columnDefinition = "text")
    var remark: String = ""

    @Column(name = "created_at", nullable = false, updatable = true)
    var createdAt: LocalDateTime = LocalDateTime.now()
}

data class UserCoinsKey(
    var uid: Long = 0L,
    var accountType: String = "GLOBAL",
) : java.io.Serializable
