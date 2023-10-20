package com.now.nowbot.throwable.ServiceException;

import com.now.nowbot.throwable.TipsRuntimeException;

public class BindException extends TipsRuntimeException{
    public enum Type {
        BIND_Client_RelieveBindSuccess("您已成功解绑。TuT"),//解绑_成功
        BIND_Client_RelieveBindFailed("解绑失败！请重试。OwOb"),//解绑_失败
        BIND_Client_BindingNoName("你叫啥名呀？告诉我吧。"),//绑定_玩家未输入用户名
        BIND_Client_AlreadyBound("不要重复绑定哟，小沐已经记住你啦！\n(如果要改绑，请输入 !ymbind。"),//绑定_玩家早已绑定
        BIND_Client_AlreadyBoundByName("禁止在仅绑定玩家名时，再次使用玩家名绑定。\n若要改绑请输入 !ymbind。"),
        BIND_Client_BindingOvertime("绑定超时！请重试。OwOb"),//绑定_绑定超时
        BIND_Client_BindingRefused("已取消绑定。OwOb"),//绑定_取消绑定

        BIND_Me_TokenExpired("您的令牌已过期，请重新授权。(!ymbind)"),//查询自己_玩家未绑定
        BIND_Me_Banned("你号没了。"),//查询自己_玩家被封禁
        BIND_Me_Blacklisted("本 Bot 根本不想理你。"),//查询自己_玩家黑名单

        BIND_Player_NoBind("对方还没有绑定呢，请提醒他输入 !ymbind 点击链接登录，完成绑定吧。"),//查询他人_玩家未绑定
        BIND_Player_HadNotBind("对方还没有绑定。"),//查询他人_玩家未绑定
        BIND_Player_NoAuthorization("他撤销了授权呢，请提醒他输入 !ymbind 点击链接登录，重新授权吧。"),//查询他人_玩家撤销授权
        BIND_Player_NoData("你查的不会是我的机器人同类吧！"),//查询他人_玩家无数据
        BIND_Player_NoQQ("请输入正确的 QQ！"),//查询他人_未搜到QQ
        BIND_Player_NotFound("这是谁呀，小沐找不到他哦？"),//查询他人_未搜到玩家
        BIND_Player_Banned("哼哼，他号没了"),//查询他人_玩家被封禁
        BIND_Player_Blacklisted("我不想和他一起玩！"),//查询他人_玩家黑名单

        BIND_Default_NoToken("哼，你 Token 失效啦！看在我们关系的份上，就帮你这一次吧！"),//token不存在，使用本机AccessToken
        BIND_Default_PictureRenderFailed("我...我画笔坏了画不出图呃。"),//图片渲染失败，或者绘图出错
        BIND_Default_PictureSendFailed("图片被麻花疼拿去祭天了。"),//图片发送失败
        BIND_Default_DefaultException("我好像生病了，需要休息一会..."),//默认报错

        BIND_Me_Success ("您已绑定成功！"),//查询自己_绑定成功
        BIND_Player_Success ("已绑定成功！"),

        ;//逗号分隔
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