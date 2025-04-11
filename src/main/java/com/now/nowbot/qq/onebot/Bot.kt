package com.now.nowbot.qq.onebot

import com.mikuac.shiro.core.Bot
import com.mikuac.shiro.model.ArrayMsg
import com.now.nowbot.qq.contact.Stranger
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.qq.onebot.contact.Friend
import com.now.nowbot.qq.onebot.contact.Group
import com.now.nowbot.qq.onebot.event.MessageEvent.Companion.getMessageChain
import com.now.nowbot.util.JacksonUtil

class Bot(val trueBot: Bot) : com.now.nowbot.qq.Bot {
    override fun getBotID(): Long {
        return trueBot.selfId
    }

    override fun getFriends(): List<Friend> {
        val data = trueBot.friendList.data
        return data.map {
            Friend(trueBot, it.userId, it.nickname)
        }
    }

    override fun getFriend(id: Long): Friend {
        val data = trueBot.getStrangerInfo(id, false).data
        return Friend(trueBot, data.userId, data.nickname)
    }

    override fun getGroups(): List<Group> {
        val data = trueBot.groupList.data
        return data.map { Group(trueBot, it.groupId, it.groupName) }
    }

    override fun getGroup(id: Long): Group {
        val data = trueBot.getGroupInfo(id, false).data
        return Group(trueBot, data.groupId, data.groupName)
    }

    override fun getMessage(id: Long): MessageChain? {
        val action = trueBot.getMsg(id.toInt())
        val data = JacksonUtil.parseObjectList(action?.data?.message ?: return null, ArrayMsg::class.java)

        if (data.isEmpty()) return null
        return getMessageChain(data)
    }

    /***
     * 未实现
     * @return null
     */
    override fun getStrangers(): List<Stranger>? {
        return null
    }

    /***
     * 未实现
     * @return null
     */
    override fun getStranger(id: Long): Stranger? {
        return null
    }
}
