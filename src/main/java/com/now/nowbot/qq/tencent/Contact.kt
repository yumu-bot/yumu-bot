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

class Contact(val userID: Long, val send:(Command.Response) -> Unit) : Group, GroupContact, Stranger {
    override val contactID = -userID

    override val name = "QQ用户"

    override fun sendMessage(msg: MessageChain): MessageReceipt {
        send(YumuServer.messageToResponse(msg))
        return FakeReceipt
    }

    override val role = Role.MEMBER

    override val isAdmin = false

    override fun getUser(qq: Long): GroupContact = this

    override val allUser: List<GroupContact> = mutableListOf(this)

    override fun sendFile(data: ByteArray, name: String) {
        sendText("无法发送文件哦.")
    }

    object FakeReceipt : MessageReceipt() {
        override fun recall() {}

        override fun recallIn(time: Long) {}

        override fun getTarget(): Contact? = null

        override fun reply(): ReplyMessage? = null
    }
}
