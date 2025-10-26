package com.now.nowbot.throwable.botRuntimeException

import com.now.nowbot.throwable.BotException
import com.now.nowbot.throwable.TipsRuntimeException

open class UnsupportedOperationException(message: String?): TipsRuntimeException(message), BotException {

    // class AudioNotSafeForWork:
    //    UnsupportedOperationException("这种歌曲，还是有点唱不出口呢。")

    class NotGroup:
        UnsupportedOperationException("请在群聊中使用这个功能！")

    class NotSupporter:
        UnsupportedOperationException("如果要使用这个功能，您需要成为 osu!supporter。")

    class NotOauthBind:
        UnsupportedOperationException("如果要使用这个功能，您需要使用 Oauth2 绑定 (!bi)。")

    class NoQQ(operation: String):
        UnsupportedOperationException("""
            $operation 操作必须输入 qq！
            格式：!sp $operation qq=114514 / group=1919810
        """.trimIndent())

    class OnlyStandard:
        UnsupportedOperationException("抱歉，本功能暂不支持除 Standard 模式以外的谱面！")

    class OnlyMania:
        UnsupportedOperationException("抱歉，本功能暂不支持除 Mania 模式以外的谱面！")
}