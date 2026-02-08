package com.now.nowbot.throwable.botRuntimeException

import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.throwable.BotException
import com.now.nowbot.throwable.TipsRuntimeException

open class BindException(message: String) : TipsRuntimeException(message), BotException {

    open class TokenExpiredException(message: String): BindException(message) {
        class YourTokenExpired:
            TokenExpiredException("你的令牌失效或是没有绑定。请授权。(!bi)")
        class UserTokenExpired:
            TokenExpiredException("该玩家的令牌失效或是没有绑定。请提醒对方授权。(!bi)")
        class SBYourTokenExpired:
            TokenExpiredException("你没有绑定偏偏要上班。请授权。(?bi)")
        class SBUserTokenExpired:
            TokenExpiredException("该玩家没有绑定偏偏要上班。请提醒对方授权。(?bi)")
        class OfficialTokenExpired:
            TokenExpiredException("你的令牌失效或是没有绑定。请重新授权。(/bind username)")
    }


    open class Oauth2Exception(message: String): BindException(message) {
        class NeedUpgrade:
            Oauth2Exception("你需要完成 Oauth2 绑定（不是名称绑定）才能使用这个功能或这类查询。")
        class NeedRefresh:
            Oauth2Exception("你的 Oauth2 令牌已经完全过期，无法刷新。请尝试重新完成 Oauth2 绑定（不是名称绑定）。")
    }


    open class NotBindException(message: String): BindException(message) {

        class YouNotBind:
            NotBindException("你还没有绑定。")
        class UserNotBind:
            NotBindException("该玩家没有绑定。")
    }

    open class UnBindException(message: String): BindException(message) {

        class UnbindSuccess:
            UnBindException("您已成功解绑。TuT")
        class UnbindFailed:
            UnBindException("该玩家没有绑定。")
    }

    open class BindReceiveException(message: String): BindException(message) {

        class ReceiveNoName:
            BindReceiveException("你叫啥名呀？告诉我吧。")
        class ReceiveOverTime:
            BindReceiveException("绑定超时！请重试。OwOb")
        class ReceiveRefused:
            BindReceiveException("您已撤回本次绑定授权。OwOb")
    }

    open class BoundException(message: String): BindException(message) {

        class YouBound:
            BoundException("""
                您已绑定，并且令牌仍旧生效。
                如果要更改绑定信息，请先输入 !ub 解除绑定关系。
                """.trimIndent())

        class UserBound(name: String, qq: Long):
            BoundException("""
                $name 已绑定 $qq，并且令牌仍旧生效。
                如果想要更改绑定信息，请先使用原来的 QQ 账号，输入 !ub 解除绑定关系。
                """.trimIndent())
    }

    open class BindConfirmException(message: String): BindException(message) {
        class NeedConfirm:
            BindConfirmException("""
                你要绑定这个机器人吗？回复 OK 确认。
                如果并不是，请无视此条消息。
            """.trimIndent())

        class MaybeAvailable(id: Long, name: String):
            BindConfirmException("""
            您已绑定 ($id) $name，但是令牌可能还没有失效。
            如果要改绑，请回复 OK。
            """.trimIndent())

        class StillAvailable(id: Long, name: String):
            BindConfirmException("""
            您已绑定 ($id) $name，但是令牌依旧有效。
            如果要改绑，请回复 OK。
            """.trimIndent())

        class Unavailable(id: Long, name: String):
            BindConfirmException("""
            您已绑定 ($id) $name，但是没有有效的 Oauth2 绑定令牌，或是令牌已经失效。
            如果要改绑，请回复 OK。
            """.trimIndent())

        class RecoverBind(name: String, name2: String, qq: Long):
            BindConfirmException("""
            正在将 $name 替代 $name2 绑定在 $qq 上，是否覆盖？
            如果要覆盖，请回复 OK。
            """.trimIndent())
    }

    open class BindIllegalArgumentException(message: String): BindException(message) {

        class IllegalQQ:
            BindIllegalArgumentException("请输入正确的 QQ！")

        class IllegalUser:
            BindIllegalArgumentException("这是谁呀，小沐找不到他哦？")

        class IllegalUserState:
            BindIllegalArgumentException("哼哼，他号没了。")
    }

    open class BindNetworkException : BindException("ppy API 访问量超限，暂时不能绑定呢...")

    open class BindResultException(message: String): BindException(message) {

        class BindUrl(url: String):
            BindResultException("""
                $url
                请在获取六位数的验证码后，回来发送 !bi 验证码 完成绑定。
                """.trimIndent())

        class BindSuccess(qq: Long, id: Long, name: String, mode: OsuMode):
            BindResultException("""
                已将 ($id) $name 绑定到 $qq 上！
                当前绑定模式为：${mode.fullName}
                您可以输入 !help 获取简洁的帮助信息。
            """.trimIndent())

        class BindSuccessWithMode(mode: OsuMode):
            BindResultException("""
                已绑定成功！
                当前绑定模式为：${mode.fullName}。
                您可以输入 !help 获取简洁的帮助信息。
                """.trimIndent())
    }
}