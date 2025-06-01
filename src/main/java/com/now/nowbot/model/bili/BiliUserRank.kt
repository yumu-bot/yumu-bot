package com.now.nowbot.model.bili

/**
 * 5000：0级未答题
 * 10000：普通会员
 * 20000：字幕君
 * 25000：VIP
 * 30000：真·职人
 * 32000：管理员
 */
enum class BiliUserRank(number: Int) {
    PASSED_BY(5000),
    CASUAL(10000),
    DANMAKU_KUN(20000),
    VIP(25000),
    TRUE_SHOKUNIN(30000),
    ADMINISTRATOR(32000),
    ;

    companion object {
        fun getBiliUserRank(number: Int): BiliUserRank {
            return when (number) {
                5000 -> PASSED_BY
                10000 -> CASUAL
                20000 -> DANMAKU_KUN
                25000 -> VIP
                30000 -> TRUE_SHOKUNIN
                32000 -> ADMINISTRATOR

                else -> PASSED_BY
            }
        }
    }
}