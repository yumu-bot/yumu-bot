package com.now.nowbot.qq.onebot

import com.mikuac.shiro.model.ArrayMsg
import com.now.nowbot.qq.contact.Stranger
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.qq.onebot.contact.Friend
import com.now.nowbot.qq.onebot.contact.Group
import com.now.nowbot.qq.onebot.event.MessageEvent.Companion.getMessageChain
import com.now.nowbot.util.JacksonUtil

class Bot(private val selfBot: Long) : com.now.nowbot.qq.Bot {
    val trueBot: com.mikuac.shiro.core.Bot
        get() = BotManager.getBot(selfBot)
            ?: throw IllegalStateException("Bot with ID $selfBot is not online or does not exist.")

    override fun getBotID(): Long {
        return selfBot
    }

    override fun getFriends(): List<Friend> {
        throw UnsupportedOperationException("not supported")
    }

    override fun getFriend(id: Long): Friend {
        throw UnsupportedOperationException("not supported")
    }

    override fun getGroups(): List<Group> {
        val trueBot = BotManager.getBot(selfBot)
            ?: throw IllegalStateException("Bot with ID $selfBot is not online or does not exist.")
        val data = trueBot.groupList.data
        return data.map { Group(selfBot, it.groupId, it.groupName) }
    }

    override fun getGroup(id: Long): Group {
        val trueBot = BotManager.getBot(selfBot)
            ?: throw IllegalStateException("Bot with ID $selfBot is not online or does not exist.")
        val data = trueBot.getGroupInfo(id, false).data
        return Group(selfBot, data.groupId, data.groupName)
    }

    override fun getMessage(id: Long): MessageChain? {
        val trueBot = BotManager.getBot(selfBot)
            ?: throw IllegalStateException("Bot with ID $selfBot is not online or does not exist.")
        val action = trueBot.getMsg(id.toInt())
        val data = JacksonUtil.parseObjectList(action?.data?.message ?: return null, ArrayMsg::class.java)

        if (data.isEmpty()) return null
        return getMessageChain(data)
    }

    /***
     * 未实现
     * @return null
     */
    override fun getStrangers(): List<Stranger> {
        return listOf()
    }

    /***
     * 未实现
     * @return null
     */
    override fun getStranger(id: Long): Stranger? {
        return null
    }
}
