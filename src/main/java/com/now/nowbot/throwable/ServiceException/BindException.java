package com.now.nowbot.throwable.ServiceException;

import com.now.nowbot.throwable.TipsRuntimeException;

public class BindException extends TipsRuntimeException{
    public enum Type {
        BIND_UnBind_Success("您已成功解绑。TuT"),
        BIND_UnBind_Failed("解绑失败！请重试。OwOb"),
        BIND_UnBind_Null("请输入要解绑的对象。"),

        BIND_Receive_NoName("你叫啥名呀？告诉我吧。"),
        BIND_Receive_Overtime("绑定超时！请重试。OwOb"),
        BIND_Receive_Refused("已取消绑定授权。OwOb"),

        BIND_Question_BindByName("""
                不推荐使用直接绑定用户名的方法。请直接发送 !ymbind 绑定，并且不带任何参数。
                如果您执意使用, 请回答:
                设随机变量 X 与 Y 相互独立，且服从 U(0,1), 则 P(X+Y<1) 为？
                """),

        BIND_Question_Wrong("回答错误。"),
        BIND_Question_Overtime("回答超时，撤回绑定请求。"),

        BIND_Progress_Binding("正在将 %s 绑定到 (%s) %s 上"),
        BIND_Progress_BindingRecoverInfo("""
                您已绑定 (%s) %s。
                如果要改绑，请回复 OK。
                """),
        BIND_Progress_BindingRecover("正在将 %s 绑定在 QQ %s 上，是否覆盖？回复 OK 生效。"),

        BIND_Me_NotBind("您还从未绑定过呢，请授权。(!ymbind)"),
        BIND_Me_TokenExpired("您的令牌已过期，请重新授权。(!ymbind)"),
        BIND_Me_TokenRevoked("您已撤销授权，请重新授权。(!ymbind)"),
        BIND_Me_TokenExpiredButBindID("您已经绑定玩家名，但是令牌已经过期。"),
        BIND_Me_Banned("你号没了，或是 ppy API 无法访问。"),
        BIND_Me_Blacklisted("本 Bot 根本不想理你。"),

        BIND_Player_NoBind("对方还没有绑定呢，请提醒他输入 !ymbind 点击链接登录，完成绑定吧。"),
        BIND_Player_HadNotBind("对方还没有绑定。"),
        BIND_Player_TokenExpired("此玩家的令牌已过期，请提醒他重新授权。(!ymbind)"),
        BIND_Player_NoQQ("请输入正确的 QQ！"),
        BIND_Player_NotFound("这是谁呀，小沐找不到他哦？"),
        BIND_Player_Banned("哼哼，他号没了"),

        BIND_API_TooManyRequests("ppy API 访问量超限，暂时不能绑定呢..."),


        BIND_Response_AlreadyBound("""
                不要重复绑定哟，小沐已经记住你啦！
                如果要改绑，请输入 !unbind 解绑后再操作。
                """),
        BIND_Response_AlreadyBoundInfo("""
                %s 已绑定 %s。
                如果要改绑，请输入 !unbind 解绑后再操作。
                """),
        BIND_Response_Success("已绑定成功！"),

        ;
        public final String message;

        Type(String message) {
            this.message = message;
        }
    }

    public BindException(BindException.Type type){
        super(type.message);
    }

    public BindException(BindException.Type type, Object... args){
        super(String.format(type.message, args));
    }
}