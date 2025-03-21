package com.now.nowbot.throwable.serviceException

import com.now.nowbot.throwable.TipsRuntimeException

class BindException : TipsRuntimeException {
    enum class Type(@JvmField val message: String) {
        BIND_UnBind_Successes("%s 已成功解绑。TuT"),
        BIND_UnBind_Success("您已成功解绑。TuT"),
        BIND_UnBind_Failed("解绑失败！请重试。OwOb"),
        BIND_UnBind_Null("请输入要解绑的对象。"),
        BIND_Receive_NoName("你叫啥名呀？告诉我吧。"),
        BIND_Receive_Overtime("绑定超时！请重试。OwOb"),
        BIND_Receive_Refused("已取消绑定授权。OwOb"),

        BIND_Question_BindRetreat("你要绑定此 Bot 吗？回复 OK 确认。\n如果并不是，请无视。"),
        BIND_Question_BindByName("""
            不推荐使用直接绑定用户名的方法。请直接发送 !ymbind 绑定，并且不带任何参数。
            如果您执意使用, 请回答:
            设随机变量 X 与 Y 相互独立，且服从 U(0,1), 则 P(X+Y<1) 为？
             """.trimIndent()
        ),

        BIND_Question_Wrong("回答错误。"),
        BIND_Question_Overtime("回答超时，撤回绑定请求。"),

        BIND_Progress_Binding("已经将 %s 与 (%s) %s 相绑定"),
        BIND_Progress_BindingRecoverInfo("""
            您已绑定 (%s) %s，但是令牌可能还没有失效。
            如果要改绑，请回复 OK。
            """.trimIndent()
        ),
        BIND_Progress_NeedToReBindInfo("""
            您已绑定 (%s) %s，但是令牌已经失效。
            如果要改绑，请回复 OK。
            """.trimIndent()
        ),
        BIND_Progress_BindingRecover("正在将 %s 绑定在 QQ %s 上，是否覆盖？回复 OK 生效。"),

        BIND_Me_NotBind("您还从未绑定过呢，请授权。(!ymbind)"),
        BIND_Me_TokenExpired("您的令牌已过期，请重新授权。(!ymbind)"),
        BIND_Me_TokenRevoked("您已撤销授权，请重新授权。(!ymbind)"),
        BIND_Me_TokenExpiredButBindID("您已经绑定玩家名，但是令牌已经过期。"),
        BIND_Me_Banned("你号没了，或是 ppy API 无法访问。"),
        BIND_Me_Blacklisted("本 Bot 根本不想理你。"),

        BIND_Player_NoBind("对方还没有绑定呢，请（让他）输入 !ymbind 点击链接登录，完成绑定吧。"),
        BIND_Player_HadNotBind("对方还没有绑定。"),
        BIND_Player_TokenExpired("此玩家的令牌已过期，请重新授权。(!ymbind)"),
        BIND_Player_NoQQ("请输入正确的 QQ！"),
        BIND_Player_NoEnoughInfo("输入的信息不足，无法完成超管绑定！"),
        BIND_Player_NotFound("这是谁呀，小沐找不到他哦？"),
        BIND_Player_Banned("哼哼，他号没了"),

        BIND_API_TooManyRequests("ppy API 访问量超限，暂时不能绑定呢..."),
        BIND_TooManyRequests("尝试次数过多, 禁止答题, 请使用不带名字的方式来绑定."),

        BIND_Response_AlreadyBound("""
            您已绑定，并且 Oauth2 令牌仍旧生效。
            如果要改绑，请先输入 !unbind 解绑。
            """.trimIndent()
        ),
        BIND_Response_AlreadyBoundInfo("""
             %s 已绑定 %s，并且 Oauth2 令牌仍旧生效（也许？）。
             如果要改绑，请先使用原来的QQ账号输入 !unbind 解绑。
             """.trimIndent()
        ),

        BIND_Response_Success("已绑定成功！"),
    }

    constructor(type: Type) : super(type.message)

    constructor(type: Type, vararg args: Any?) : super(String.format(type.message, *args))
}