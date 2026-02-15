package com.now.nowbot.qq.onebot.contact

import com.mikuac.shiro.core.Bot
import com.now.nowbot.qq.contact.Friend

class Friend(bot: Bot, id: Long, override var name: String?) : Contact(bot, id), Friend
