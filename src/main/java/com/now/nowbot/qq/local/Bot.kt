package com.now.nowbot.qq.local

import com.now.nowbot.qq.Bot
import com.now.nowbot.qq.contact.Friend
import com.now.nowbot.qq.contact.Group
import com.now.nowbot.qq.contact.Stranger
import com.now.nowbot.qq.message.MessageChain

class Bot : Bot {
    override fun getBotID(): Long {
        return 0
    }

    override fun getFriends(): List<Friend> {
        return listOf()
    }

    override fun getFriend(id: Long): Friend? {
        return null
    }

    override fun getGroups(): List<Group> {
        return listOf()
    }

    override fun getGroup(id: Long): Group? {
        return null
    }

    override fun getMessage(id: Long): MessageChain? {
        return null
    }

    override fun getStrangers(): List<Stranger> {
        return listOf()
    }

    override fun getStranger(id: Long): Stranger? {
        return null
    }
}
