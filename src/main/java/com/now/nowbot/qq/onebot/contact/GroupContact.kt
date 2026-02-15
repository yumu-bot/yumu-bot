package com.now.nowbot.qq.onebot.contact

import com.mikuac.shiro.core.Bot
import com.now.nowbot.qq.contact.GroupContact
import com.now.nowbot.qq.enums.Role

/**
 * 群发起私聊
 */
class GroupContact(bot: Bot, userID: Long, groupID: Long, name: String?, role: String? = null) : Contact(bot, userID), GroupContact {
    var groupID: Long
    override var role: Role?

    init {
        this.name = name
        this.groupID = groupID
        this.role = Role.getRole(role)
    }
}
