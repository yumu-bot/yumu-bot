package com.now.nowbot.throwable.botRuntimeException

import com.now.nowbot.throwable.BotException
import com.now.nowbot.throwable.TipsRuntimeException
import kotlin.IllegalStateException

open class IllegalStateException(message: String?): TipsRuntimeException(message), BotException {

    class Calculate(component: String):
        IllegalStateException("$component：数据计算失败。")

    class Fetch(component: String):
        IllegalStateException("$component：数据获取失败。")

    class Render(component: String):
        IllegalStateException("$component：渲染失败。")

    class Send(component: String):
        IllegalStateException("$component：发送失败。")

    class ReadFile(component: String):
        IllegalStateException("$component：文件读取失败。")

    class ClassCast(component: String):
        IllegalStateException("$component：类型转换失败。")

    class TooManyRequest(component: String):
        IllegalStateException("$component：一次性输入的数据太多！获取信息的时候可能会遇到 API 瓶颈。")

}