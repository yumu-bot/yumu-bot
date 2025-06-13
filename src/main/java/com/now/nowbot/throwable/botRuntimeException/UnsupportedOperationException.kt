package com.now.nowbot.throwable.botRuntimeException

import com.now.nowbot.throwable.TipsRuntimeException

open class UnsupportedOperationException(message: String?): TipsRuntimeException(message) {

    class AudioNotSafeForWork:
        UnsupportedOperationException("这种歌曲，还是有点唱不出口呢。")

    class NotGroup:
        UnsupportedOperationException("请在群聊中使用这个功能！")

}