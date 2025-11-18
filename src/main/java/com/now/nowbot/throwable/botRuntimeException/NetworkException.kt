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
                渲染模块内部错误。
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

        class GatewayTimeout:
            RenderModuleException("""
                504 Gateway Timeout
                网关超时。
            """.trimIndent()) {
            override val code = 504
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
                服务器禁止获取玩家数据。
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

        class RequestTimeout:
            UserException("""
                408 Request Timeout
                玩家连接超时。
            """.trimIndent()) {
            override val code = 408
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

        /*
        class TokenExpired:
            UserException("""
                498 Token Expired/Invalid
                玩家的令牌已过期，请重新尝试 !bi 绑定。
            """.trimIndent()) {
            override val code = 498
        }

         */

        class InternalServerError:
            UserException("""
                500 Internal Server Error
                请求服务器内部错误。
            """.trimIndent()) {
            override val code = 500
        }

        class BadGateWay:
            UserException("""
                502 Bad GateWay
                无法访问请求服务器。
            """.trimIndent()) {
            override val code = 502
        }

        class ServiceUnavailable:
            UserException("""
                503 Service Unavailable
                请求服务器未上线。
            """.trimIndent()) {
            override val code = 503
        }

        class GatewayTimeout:
            MatchException("""
                504 Gateway Timeout
                网关超时。
            """.trimIndent()) {
            override val code = 504
        }

        class Undefined(e: Throwable):
            MatchException("""
                连接出现未识别的错误。
                信息如下：${e.message}
            """.trimIndent()) {
            override val code = -1
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
                服务器禁止获取谱面数据。
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

        class RequestTimeout:
            BeatmapException("""
                408 Request Timeout
                谱面连接超时。
            """.trimIndent()) {
            override val code = 408
        }

        class TooManyRequests:
            BeatmapException("""
                429 Too Many Requests
                请求数已达上限。
            """.trimIndent()) {
            override val code = 429
        }

        class InternalServerError:
            BeatmapException("""
                500 Internal Server Error
                请求服务器内部错误。
            """.trimIndent()) {
            override val code = 500
        }

        class BadGateWay:
            BeatmapException("""
                502 Bad GateWay
                无法访问请求服务器。
            """.trimIndent()) {
            override val code = 502
        }

        class ServiceUnavailable:
            BeatmapException("""
                503 Service Unavailable
                请求服务器未上线。
            """.trimIndent()) {
            override val code = 503
        }

        class GatewayTimeout:
            MatchException("""
                504 Gateway Timeout
                网关超时。
            """.trimIndent()) {
            override val code = 504
        }

        class Undefined(e: Throwable):
            MatchException("""
                连接出现未识别的错误。
                信息如下：${e.message}
            """.trimIndent()) {
            override val code = -1
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
                服务器禁止获取成绩数据。
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

        class RequestTimeout:
            ScoreException("""
                408 Request Timeout
                成绩连接超时。
            """.trimIndent()) {
            override val code = 408
        }

        class UnprocessableEntity:
            ScoreException("""
                422 UnprocessableEntity
                您需要成为 osu!supporter 才能使用此功能。
            """.trimIndent()) {
            override val code = 422
        }

        class TooManyRequests:
            ScoreException("""
                429 Too Many Requests
                请求数已达上限。
            """.trimIndent()) {
            override val code = 429
        }

        class InternalServerError:
            ScoreException("""
                500 Internal Server Error
                请求服务器内部错误。
            """.trimIndent()) {
            override val code = 500
        }

        class BadGateway:
            ScoreException("""
                502 Bad GateWay
                无法访问请求服务器。
            """.trimIndent()) {
            override val code = 502
        }

        class ServiceUnavailable:
            ScoreException("""
                503 Service Unavailable
                请求服务器未上线。
            """.trimIndent()) {
            override val code = 503
        }

        class GatewayTimeout:
            ScoreException("""
                504 Gateway Timeout
                网关超时。
            """.trimIndent()) {
            override val code = 504
        }

        class Undefined(e: Throwable):
            ScoreException("""
                连接出现未识别的错误。
                信息如下：${e.message}
            """.trimIndent()) {
            override val code = -1
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

        class Forbidden:
            MatchException("""
                403 Forbidden
                服务器禁止获取比赛数据。
            """.trimIndent()) {
            override val code = 403
        }

        class NotFound:
            MatchException("""
                404 Not Found
                找不到比赛。
            """.trimIndent()) {
            override val code = 404
        }

        class RequestTimeout:
            MatchException("""
                408 Request Timeout
                比赛连接超时。
            """.trimIndent()) {
            override val code = 408
        }

        class TooManyRequests:
            MatchException("""
                429 Too Many Requests
                请求数已达上限。
            """.trimIndent()) {
            override val code = 429
        }

        class InternalServerError:
            MatchException("""
                500 Internal Server Error
                请求服务器内部错误。
            """.trimIndent()) {
            override val code = 500
        }

        class BadGateway:
            MatchException("""
                502 Bad GateWay
                无法访问请求服务器。
            """.trimIndent()) {
            override val code = 502
        }

        class ServiceUnavailable:
            MatchException("""
                503 Service Unavailable
                请求服务器未上线。
            """.trimIndent()) {
            override val code = 503
        }

        class GatewayTimeout:
            MatchException("""
                504 Gateway Timeout
                网关超时。
            """.trimIndent()) {
            override val code = 504
        }

        class Undefined(e: Throwable):
            MatchException("""
                连接出现未识别的错误。
                信息如下：${e.message}
            """.trimIndent()) {
            override val code = -1
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
            DivingFishException("""
                401 Unauthorized
                请求端的公用授权已过期。
            """.trimIndent()) {
            override val code = 401
        }

        class Forbidden:
            DivingFishException("""
                403 Forbidden
                水鱼用户未授权或是服务器禁止获取数据。
            """.trimIndent()) {
            override val code = 403
        }

        class NotFound:
            DivingFishException("""
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

    open class LxnsException(message: String?): NetworkException(message) {
        override val code: Int = 0

        class BadRequest:
            LxnsException("""
                400 Bad Request
                落雪请求格式错误。
            """.trimIndent()) {
            override val code = 400
        }

        class Unauthorized:
            LxnsException("""
                401 Unauthorized
                请求端的公用授权已过期。
            """.trimIndent()) {
            override val code = 401
        }

        class Forbidden:
            LxnsException("""
                403 Forbidden
                落雪用户未授权或是服务器禁止获取数据。
            """.trimIndent()) {
            override val code = 403
        }

        class NotFound:
            LxnsException("""
                404 Not Found
                找不到落雪用户。
            """.trimIndent()) {
            override val code = 404
        }

        class RequestTimeout:
            LxnsException("""
                408 Request Timeout
                落雪连接超时。
            """.trimIndent()) {
            override val code = 408
        }

        class TooManyRequests:
            LxnsException("""
                429 Too Many Requests
                落雪请求数已达上限。
            """.trimIndent()) {
            override val code = 429
        }

        class InternalServerError:
            LxnsException("""
                500 Internal Server Error
                落雪内部逻辑错误。
            """.trimIndent()) {
            override val code = 500
        }

        class BadGateway:
            LxnsException("""
                502 Bad Gateway
                和落雪之间的连接出现了问题。
            """.trimIndent()) {
            override val code = 502
        }

        class ServiceUnavailable:
            LxnsException("""
                503 Service Unavailable
                落雪服务器未上线。
            """.trimIndent()) {
            override val code = 503
        }

        class GatewayTimeout:
            LxnsException("""
                504 Gateway Timeout
                网关超时。
            """.trimIndent()) {
            override val code = 504
        }

        class Undefined(e: Throwable):
            LxnsException("""
                和落雪之间的连接出现未识别的错误。
                信息如下：${e.message}
            """.trimIndent()) {
            override val code = -1
        }
    }

}
