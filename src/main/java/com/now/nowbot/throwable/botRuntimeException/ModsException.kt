package com.now.nowbot.throwable.botRuntimeException

import com.now.nowbot.throwable.TipsRuntimeException

open class ModsException(message: String) : TipsRuntimeException(message) {
    class CharNotPaired:
        ModsException("模组输入异常：字符数量非偶数")

    class ModConflict(vararg mods: String):
        ModsException("模组异常：以下模组冲突：${mods.joinToString(", ")}")
}
