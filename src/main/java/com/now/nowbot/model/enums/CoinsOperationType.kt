package com.now.nowbot.model.enums

import kotlin.math.abs

enum class CoinsOperationType(
    val isIncrease: Boolean,
    val description: String,
) {
    // 有别的直接加枚举
    ACCOUNT_INIT_REWARD(
        isIncrease = true,
        description = "创建账号初始奖励"
    );

    infix fun signed(amount: Double): Double {
        val normalized = abs(amount)
        return if (isIncrease) normalized else -normalized
    }


    val remark: String = this.name
}

