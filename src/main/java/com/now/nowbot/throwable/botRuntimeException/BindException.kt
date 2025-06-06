package com.now.nowbot.throwable.botRuntimeException

import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.throwable.TipsRuntimeException

open class BindException(message: String) : TipsRuntimeException(message) {

    open class TokenExpiredException(message: String): BindException(message) {
        class YouTokenExpiredException:
            TokenExpiredException("令牌失效，请重新授权。(!bi)")
        class UserTokenExpiredException:
            TokenExpiredException("该玩家令牌失效，请提醒他重新授权。(!bi)")
        class OfficialTokenExpiredException:
            TokenExpiredException("令牌失效，请重新授权。(/bind osu username)")
    }

    open class NotBindException(message: String): BindException(message) {

        class YouNotBind:
            NotBindException("你还没有绑定。")
        class UserNotBind:
            NotBindException("该玩家没有绑定。")
    }

    open class UnBindException(message: String): BindException(message) {

        class UnbindSuccess :
            UnBindException("您已成功解绑。TuT")
        class UnbindFailed:
            UnBindException("该玩家没有绑定。")
        class UnbindNotFound:
            UnBindException("请提供要解绑的对象。")
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
        class ConfirmThis:
            BindConfirmException("你要绑定此 Bot 吗？回复 OK 确认。\n如果并不是，请无视此条消息。")

        class NoNeedReBind(id: Long, name: String):
            BindConfirmException("""
            您已绑定 ($id) $name，但是令牌可能还没有失效。
            如果要改绑，请回复 OK。
            """.trimIndent())

        class NeedReBind(id: Long, name: String):
            BindConfirmException("""
            您已绑定 ($id) $name，但是令牌已经失效。
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
                请 ctrl+c 并 ctrl+v 到其他 browser 完成绑定。
                """.trimIndent())

        class BindSuccess(qq: Long, id: Long, name: String):
            BindResultException("已将 ($id) $name 绑定到 $qq 上！")

        class BindSuccessTip:
            BindResultException("已绑定成功！")

        class BindSuccessWithMode(mode: OsuMode):
            BindResultException("""
                已绑定成功！
                当前绑定模式为：${mode.fullName}
                """.trimIndent())

        class BindFailed:
            BindResultException("绑定失败！")
    }
}