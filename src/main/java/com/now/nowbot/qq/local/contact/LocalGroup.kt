package com.now.nowbot.qq.local.contact

import com.now.nowbot.qq.contact.Contact
import com.now.nowbot.qq.contact.Group
import com.now.nowbot.qq.enums.Role

class LocalGroup : LocalContact(), Group {
    override val isAdmin: Boolean
        get() = true

    override fun getUser(qq: Long): com.now.nowbot.qq.contact.GroupContact {
        return GroupContact()
    }

    override val allUser: List<GroupContact>
        get() = listOf()

    override fun sendFile(data: ByteArray, name: String) {
        val path = super.saveFile(name, data)
        Contact.log.info("bot: 发送文件 {}", path)
    }

    class GroupContact : LocalContact(), com.now.nowbot.qq.contact.GroupContact {
        override val role: Role
            get() = Role.ADMIN
    }
}
