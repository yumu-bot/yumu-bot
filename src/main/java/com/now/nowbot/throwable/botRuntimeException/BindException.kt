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

        class YouNotBindException:
            NotBindException("你还没有绑定。")
        class UserNotBindException:
            NotBindException("该玩家没有绑定。")
    }

    open class UnBindException(message: String): BindException(message) {

        class UnbindSuccessException :
            UnBindException("您已成功解绑。TuT")
        class UnbindFailedException:
            UnBindException("该玩家没有绑定。")
        class UnbindNotFoundException:
            UnBindException("请提供要解绑的对象。")
    }

    open class BindReceiveException(message: String): BindException(message) {

        class ReceiveNameException:
            BindReceiveException("你叫啥名呀？告诉我吧。")
        class ReceiveOverTimeException:
            BindReceiveException("绑定超时！请重试。OwOb")
        class ReceiveRefusedException:
            BindReceiveException("您已撤回本次绑定授权。OwOb")
    }

    open class BoundException(message: String): BindException(message) {

        class YouBoundException:
            BoundException("""
                您已绑定，并且令牌仍旧生效。
                如果要更改绑定信息，请先输入 !ub 解除绑定关系。
                """.trimIndent())

        class UserBoundException(name: String, qq: Long):
            BoundException("""
                $name 已绑定 $qq，并且令牌仍旧生效。
                如果想要更改绑定信息，请先使用原来的 QQ 账号，输入 !ub 解除绑定关系。
                """.trimIndent())
    }

    open class BindConfirmException(message: String): BindException(message) {
        class ConfirmThisException:
            BindConfirmException("你要绑定此 Bot 吗？回复 OK 确认。\n如果并不是，请无视此条消息。")

        class NoNeedReBindException(id: Long, name: String):
            BindConfirmException("""
            您已绑定 ($id) $name，但是令牌可能还没有失效。
            如果要改绑，请回复 OK。
            """.trimIndent())

        class NeedReBindException(id: Long, name: String):
            BindConfirmException("""
            您已绑定 ($id) $name，但是令牌已经失效。
            如果要改绑，请回复 OK。
            """.trimIndent())

        class RecoverBindException(name: String, qq: Long):
            BindConfirmException("""
            正在将 $name 绑定在 QQ $qq 上，是否覆盖？
            如果要覆盖，请回复 OK。
            """.trimIndent())
    }

    open class BindIllegalArgumentException(message: String): BindException(message) {

        class IllegalQQException:
            BindIllegalArgumentException("请输入正确的 QQ！")

        class IllegalUserException:
            BindIllegalArgumentException("这是谁呀，小沐找不到他哦？")

        class IllegalUserStateException:
            BindIllegalArgumentException("哼哼，他号没了。")
    }

    open class BindNetworkException : BindException("ppy API 访问量超限，暂时不能绑定呢...")

    open class BindResultException(message: String): BindException(message) {

        class BindUrlException(url: String):
            BindResultException("""
                $url
                请 ctrl+c 并 ctrl+v 到其他 browser 完成绑定。
                """.trimIndent())

        class BindSuccessException(qq: Long, id: Long, name: String):
            BindResultException("已将 ($id) $name 绑定到 $qq 上！")

        class BindSuccessTipException:
            BindResultException("已绑定成功！")

        class BindSuccessWithModeException(mode: OsuMode):
            BindResultException("""
                已绑定成功！
                当前绑定模式为：${mode.fullName}
                """.trimIndent())

        class BindFailedException:
            BindResultException("绑定失败！")
    }
}