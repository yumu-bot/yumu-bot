package com.now.nowbot.restrict

enum class RestrictTargetType(val byte: Byte) {
    ALL(1), USER(2), GROUP(4);

    companion object {
        fun getByIndex(index: Byte): RestrictTargetType {
            return when (index.toInt()) {
                1 -> ALL
                2 -> USER
                4 -> GROUP
                else -> throw IllegalArgumentException("限制目标类型：非法下标：$index")
            }
        }
    }
}

enum class RestrictSourceType(val byte: Byte) {
    ADMIN(1), USER(2), GROUP(4);

    companion object {
        fun getByIndex(index: Byte): RestrictSourceType {
            return when (index.toInt()) {
                1 -> ADMIN
                2 -> USER
                4 -> GROUP
                else -> throw IllegalArgumentException("限制来源类型：非法下标：$index")
            }
        }
    }
}