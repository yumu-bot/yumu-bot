package com.now.nowbot.restrict

import com.now.nowbot.qq.Bot
import com.now.nowbot.qq.contact.Group
import com.now.nowbot.qq.enums.Role
import com.now.nowbot.qq.event.MessageEvent
import kotlin.collections.contains

object RestrictUtils {
    fun isSuperAdmin(qq: Long?): Boolean {
        return RestrictImplement.SUPER_USERS.contains(qq)
    }

    fun isGroupAdmin(bot: Bot?, groupID: Long, qq: Long): Boolean {
        if (bot == null) return false

        val group: Group = bot.getGroup(groupID) ?: return false
        val member = group.getUser(qq)

        return member.role == Role.ADMIN || member.role == Role.OWNER
    }

    fun isNotSuperAdmin(qq: Long?): Boolean {
        return !isSuperAdmin(qq)
    }

    fun MessageEvent.isSuperAdmin(): Boolean {
        return RestrictImplement.SUPER_USERS.contains(this.sender.contactID)
    }

    fun MessageEvent.isGroupAdmin(): Boolean {
        return isGroupAdmin(
            this.bot,
            this.subject.contactID,
            this.sender.contactID
        ) || isSuperAdmin(this.sender.contactID)
    }

    fun MessageEvent.isNotSuperAdmin(): Boolean {
        return !isSuperAdmin()
    }

    fun MessageEvent.isCommonUser(): Boolean {
        return !isGroupAdmin()
    }
}