package com.now.nowbot.throwable.botRuntimeException

import com.now.nowbot.throwable.TipsRuntimeException

open class NetworkException(message: String): TipsRuntimeException(message) {

    class TooManyRequestException(): NetworkException("ppy API 访问量超限，暂时不能绑定呢...")

}