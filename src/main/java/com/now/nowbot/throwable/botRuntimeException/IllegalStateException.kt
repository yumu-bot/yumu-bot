package com.now.nowbot.throwable.botRuntimeException

import com.now.nowbot.throwable.BotException
import com.now.nowbot.throwable.TipsRuntimeException
import kotlin.IllegalStateException

open class IllegalStateException(message: String?): TipsRuntimeException(message), BotException {

    class Calculate(component: Any):
        IllegalStateException("$component：数据计算失败。")

    class ClassCast(component: Any):
        IllegalStateException("$component：类型转换失败。")

    class Fetch(component: Any):
        IllegalStateException("$component：数据获取失败。")

    class ReadFile(component: Any):
        IllegalStateException("$component：文件读取失败。")

    class Render(component: Any):
        IllegalStateException("$component：渲染失败。")

    class Revoke(component: Any):
        IllegalStateException("$component：撤回失败。")

    class Send(component: Any):
        IllegalStateException("$component：发送失败。")

    class TooManyRequest(component: Any):
        IllegalStateException("$component：一次性输入的数据太多！获取信息的时候可能会遇到 API 瓶颈。")

}