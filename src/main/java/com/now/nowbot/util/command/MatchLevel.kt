package com.now.nowbot.util.command

    /**
     * level 匹配等级。
     * ANY_LAZY：任意个（懒惰模式） (\*?)
     * ANY：任意个 (\*)
     * MAYBE：没有或者一个 (?)
     * MORE：一个或更多个 (+)
     * EXIST：必须存在 ()，
     */
    enum class MatchLevel {
        ANY_LAZY,
        ANY,
        MAYBE,
        MORE,
        EXIST
    }