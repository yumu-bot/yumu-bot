package com.now.nowbot.qq.enums

enum class Role {
    OWNER, ADMIN, MEMBER;

    companion object {
        fun getRole(s: String?): Role {
            return when (s?.trim()?.lowercase()) {
                "owner" -> OWNER
                "admin" -> ADMIN
                null -> MEMBER
                else -> MEMBER
            }
        }
    }
}