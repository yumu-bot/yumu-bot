package com.now.nowbot.qq.message

import com.mikuac.shiro.common.utils.MsgUtils
class AtMessage : Message {
    val target: Long

    constructor(qq: Long) {
        this.target = qq
    }

    constructor() {
        this.target = -1L
    }

    val isAll: Boolean
        get() = target == -1L

    override fun toString(): String {
        return "[@${target}]"
    }

    override fun getCQ(): String {
        return MsgUtils().at(target).build()
    }

    override fun toJson(): JsonMessage {
        val qq: Any = if (isAll) {
            "all"
        } else {
            target
        }
        return JsonMessage("at", mapOf("qq" to qq))
    }
}
