package com.now.nowbot.qq.tencent

import com.now.nowbot.qq.contact.Contact
import com.now.nowbot.qq.contact.Group
import com.now.nowbot.qq.contact.GroupContact
import com.now.nowbot.qq.contact.Stranger
import com.now.nowbot.qq.enums.Role
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.qq.message.MessageReceipt
import com.now.nowbot.qq.message.ReplyMessage
import com.yumu.model.packages.Command

class Contact(val UID: Long, val send:(Command.Response) -> Unit) : Group, GroupContact, Stranger {
    override fun getId() = -UID

    override fun getName() = "QQ用户"

    override fun sendMessage(msg: MessageChain): MessageReceipt {
        send(YumuServer.messageToResponse(msg))
        return FakeReceipt
    }

    override fun getRole() = Role.MEMBER

    override fun isAdmin() = false

    override fun getUser(qq: Long): GroupContact = this

    override fun getAllUser(): MutableList<out GroupContact> = mutableListOf(this)

    override fun sendFile(data: ByteArray?, name: String?) {
        sendText("无法发送文件哦.")
    }

    object FakeReceipt : MessageReceipt() {
        override fun recall() {}

        override fun recallIn(time: Long) {}

        override fun getTarget(): Contact? = null

        override fun reply(): ReplyMessage? = null
    }
}
