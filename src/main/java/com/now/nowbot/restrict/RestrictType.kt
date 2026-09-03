package com.now.nowbot.restrict

enum class RestrictTargetType(val byte: Byte) {
    ALL(1), USER(2), GROUP(4);

    companion object {
        fun getByIndex(index: Byte): RestrictTargetType {
            return entries.first { it.byte == index }
        }
    }
}

enum class RestrictSourceType(val byte: Byte) {
    ADMIN(1), USER(2), GROUP(4);


    companion object {
        fun getByIndex(index: Byte): RestrictSourceType {
            return entries.first { it.byte == index }
        }
    }
}