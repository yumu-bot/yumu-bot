package com.now.nowbot.throwable.botRuntimeException

import com.now.nowbot.throwable.BotException
import com.now.nowbot.throwable.TipsRuntimeException

open class NetworkException(message: String?
): TipsRuntimeException(message), BotException {

    open val code: Int = 0


    open class ComponentException(message: String?): NetworkException(message) {
        override val code: Int = 0

        class NoResponse:
            ComponentException("""
                444 No Response
                没有响应。
            """.trimIndent()) {
            override val code = 444
        }
    }

    open class RenderModuleException(message: String): NetworkException(message) {
        override val code: Int = 0

        class BadRequest:
            RenderModuleException("""
                400 Bad Request
                渲染模块请求格式错误。
            """.trimIndent()) {
            override val code = 400
        }

        class RequestTimeout:
            RenderModuleException("""
                408 Request Timeout
                渲染模块连接超时，有可能是需要绘制的数据太多了。
            """.trimIndent()) {
            override val code = 408
        }

        class InternalServerError:
            RenderModuleException("""
                500 Internal Server Error
                渲染模块逻辑错误。
            """.trimIndent()) {
            override val code = 500
        }

        class BadGateway:
            RenderModuleException("""
                502 Bad Gateway
                与渲染模块之间的连接出现了问题。
            """.trimIndent()) {
            override val code = 502
        }

        class ServiceUnavailable:
            RenderModuleException("""
                503 Service Unavailable
                渲染模块未上线。
            """.trimIndent()) {
            override val code = 503
        }

        class Undefined(e: Throwable):
            RenderModuleException("""
                渲染模块出现未识别的错误。
                信息如下：${e.message}
            """.trimIndent()) {
            override val code = -1
        }
    }

    open class UserException(message: String?): NetworkException(message) {
        override val code: Int = 0

        class BadRequest:
            UserException("""
                400 Bad Request
                玩家请求格式错误。
            """.trimIndent()) {
            override val code = 400
        }

        class Unauthorized:
            UserException("""
                401 Unauthorized
                玩家未授权或是授权已过期。
            """.trimIndent()) {
            override val code = 401
        }

        class Forbidden:
            UserException("""
                403 Forbidden
                玩家已被封禁。
            """.trimIndent()) {
            override val code = 403
        }

        class NotFound:
            UserException("""
                404 Not Found
                找不到玩家。
            """.trimIndent()) {
            override val code = 404
        }

        class UnprocessableEntity:
            UserException("""
                424 Unprocessable Entity
                玩家名格式错误或超长。
            """.trimIndent()) {
            override val code = 424
        }

        class TooManyRequests:
            UserException("""
                429 Too Many Requests
                请求数已达上限。
            """.trimIndent()) {
            override val code = 429
        }

        class ServiceUnavailable:
            UserException("""
                503 Service Unavailable
                请求服务器未上线。
            """.trimIndent()) {
            override val code = 503
        }
    }


    open class BeatmapException(message: String?): NetworkException(message) {
        override val code: Int = 0

        class BadRequest:
            BeatmapException("""
                400 Bad Request
                谱面请求格式错误。
            """.trimIndent()) {
            override val code = 400
        }

        class Unauthorized:
            BeatmapException("""
                401 Unauthorized
                请求端的公用授权已过期。
            """.trimIndent()) {
            override val code = 401
        }

        class Forbidden:
            BeatmapException("""
                403 Forbidden
                谱面已被限制。
            """.trimIndent()) {
            override val code = 403
        }

        class NotFound:
            BeatmapException("""
                404 Not Found
                找不到谱面。
            """.trimIndent()) {
            override val code = 404
        }

        class TooManyRequests:
            BeatmapException("""
                429 Too Many Requests
                请求数已达上限。
            """.trimIndent()) {
            override val code = 429
        }

        class ServiceUnavailable:
            BeatmapException("""
                503 Service Unavailable
                请求服务器未上线。
            """.trimIndent()) {
            override val code = 503
        }
    }

    open class ScoreException(message: String?): NetworkException(message) {
        override val code: Int = 0

        class BadRequest:
            ScoreException("""
                400 Bad Request
                成绩请求格式错误。
            """.trimIndent()) {
            override val code = 400
        }

        class Unauthorized:
            ScoreException("""
                401 Unauthorized
                请求端的公用授权已过期。
            """.trimIndent()) {
            override val code = 401
        }

        class Forbidden:
            ScoreException("""
                403 Forbidden
                成绩已被限制。
            """.trimIndent()) {
            override val code = 403
        }

        class NotFound:
            ScoreException("""
                404 Not Found
                找不到成绩。
            """.trimIndent()) {
            override val code = 404
        }

        class UnprocessableEntity:
            ScoreException("""
                422 UnprocessableEntity
                您需要成为 osu!supporter 才能使用此功能。
            """.trimIndent()) {
            override val code = 503
        }

        class TooManyRequests:
            ScoreException("""
                429 Too Many Requests
                请求数已达上限。
            """.trimIndent()) {
            override val code = 429
        }

        class ServiceUnavailable:
            ScoreException("""
                503 Service Unavailable
                请求服务器未上线。
            """.trimIndent()) {
            override val code = 503
        }
    }

    open class MatchException(message: String?): NetworkException(message) {
        override val code: Int = 0

        class BadRequest:
            MatchException("""
                400 Bad Request
                比赛请求格式错误。
            """.trimIndent()) {
            override val code = 400
        }

        class Unauthorized:
            MatchException("""
                401 Unauthorized
                请求端的公用授权已过期。
            """.trimIndent()) {
            override val code = 401
        }

        class NotFound:
            MatchException("""
                404 Not Found
                找不到比赛。
            """.trimIndent()) {
            override val code = 404
        }

        class TooManyRequests:
            MatchException("""
                429 Too Many Requests
                请求数已达上限。
            """.trimIndent()) {
            override val code = 429
        }

        class ServiceUnavailable:
            MatchException("""
                503 Service Unavailable
                请求服务器未上线。
            """.trimIndent()) {
            override val code = 503
        }
    }
    open class DivingFishException(message: String?): NetworkException(message) {
        override val code: Int = 0

        class BadRequest:
            DivingFishException("""
                400 Bad Request
                水鱼请求格式错误。
            """.trimIndent()) {
            override val code = 400
        }

        class Unauthorized:
            MatchException("""
                401 Unauthorized
                请求端的公用授权已过期。
            """.trimIndent()) {
            override val code = 401
        }

        class Forbidden:
            DivingFishException("""
                403 Forbidden
                水鱼用户未授权或是禁止第三方获取数据。
            """.trimIndent()) {
            override val code = 403
        }

        class NotFound:
            MatchException("""
                404 Not Found
                找不到水鱼用户。
            """.trimIndent()) {
            override val code = 404
        }


        class RequestTimeout:
            DivingFishException("""
                408 Request Timeout
                水鱼连接超时。
            """.trimIndent()) {
            override val code = 408
        }

        class InternalServerError:
            DivingFishException("""
                500 Internal Server Error
                水鱼内部逻辑错误。
            """.trimIndent()) {
            override val code = 500
        }

        class BadGateway:
            DivingFishException("""
                502 Bad Gateway
                和水鱼之间的连接出现了问题。
            """.trimIndent()) {
            override val code = 502
        }

        class Undefined(e: Throwable):
            DivingFishException("""
                和水鱼之间的连接出现未识别的错误。
                信息如下：${e.message}
            """.trimIndent()) {
            override val code = -1
        }
    }

}
