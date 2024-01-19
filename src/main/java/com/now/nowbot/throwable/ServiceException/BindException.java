package com.now.nowbot.throwable.ServiceException;

import com.now.nowbot.throwable.TipsRuntimeException;

public class BindException extends TipsRuntimeException{
    public enum Type {
        BIND_Client_UnBindSuccess("您已成功解绑。TuT"),
        BIND_Client_UnBindFailed("解绑失败！请重试。OwOb"),
        BIND_Client_UnBindNull("请输入要解绑的对象。"),
        BIND_Client_NullName("你叫啥名呀？告诉我吧。"),
        BIND_Client_AlreadyBound("不要重复绑定哟，小沐已经记住你啦！\n(如果要改绑，请输入 !ymbind。"),
        BIND_Client_AlreadyBoundByName("禁止在仅绑定玩家名时，再次使用玩家名绑定。\n若要改绑请输入 !ymbind。"),
        BIND_Client_Overtime("绑定超时！请重试。OwOb"),
        BIND_Client_Refused("已取消绑定。OwOb"),
        BIND_Client_Unbind("解绑请联系管理员，\n或可以直接去个人主页撤销授权。"),

        BIND_Me_NotBind("您还从未绑定过呢，请授权。(!ymbind)"),
        BIND_Me_TokenExpired("您的令牌已过期，请重新授权。(!ymbind)"),
        BIND_Me_AuthorizationRevoked("您已撤销授权，请重新授权。(!ymbind)"),
        BIND_Me_Banned("你号没了，或是 ppy API 无法访问。"),
        BIND_Me_TooManyRequests("ppy API 访问量超限，暂时不能绑定呢..."),
        BIND_Me_Blacklisted("本 Bot 根本不想理你。"),

        BIND_Player_NoBind("对方还没有绑定呢，请提醒他输入 !ymbind 点击链接登录，完成绑定吧。"),
        BIND_Player_HadNotBind("对方还没有绑定。"),
        BIND_Player_TokenExpired("此玩家的令牌已过期，请提醒他重新授权。(!ymbind)"),
        BIND_Player_NoData("你查的不会是我的机器人同类吧！"),
        BIND_Player_NoQQ("请输入正确的 QQ！"),
        BIND_Player_NotFound("这是谁呀，小沐找不到他哦？"),
        BIND_Player_Banned("哼哼，他号没了"),
        BIND_Player_Blacklisted("我不想和他一起玩！"),

        BIND_Default_NoToken("哼，你 Token 失效啦！看在我们关系的份上，就帮你这一次吧！"),
        BIND_Default_PictureRenderFailed("我...我画笔坏了画不出图呃。"),
        BIND_Default_PictureSendFailed("图片被麻花疼拿去祭天了。"),
        BIND_Default_DefaultException("我好像生病了，需要休息一会..."),

        BIND_Me_Success ("您已绑定成功！"),
        BIND_Player_Success ("已绑定成功！"),

        ;
        final String message;
        Type(String message) {
            this.message = message;
        }
        }

    public BindException(BindException.Type type){
        super(type.message);
    }

    public BindException(BindException.Type type, String Str){
        super(type.message + Str);
    }
}