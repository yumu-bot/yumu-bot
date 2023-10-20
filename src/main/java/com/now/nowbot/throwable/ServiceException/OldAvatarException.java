package com.now.nowbot.throwable.ServiceException;

import com.now.nowbot.throwable.TipsException;

public class OldAvatarException extends TipsException {
    public enum Type {
        OA_Me_TokenExpired("您的绑定状态已经失效！\n请输入 !ymbind 重新绑定。\n除此之外，还可以试试 !oa [username]"),//查询自己_绑定失效
        OA_Me_FetchFailed("获取信息失败。"), //玩家_获取失败
        OA_Parameter_Error("输入参数错误。"),//查询他人
        OA_Player_NotFound("这是谁呀，小沐找不到他哦。"),//查询他人_未搜到玩家
        OA_Player_FetchFailed("获取对方的信息失败，请重试。"), //玩家_获取失败
        OA_Send_Error("OldAvatar 渲染图片超时，请重试，或者将问题反馈给开发者。") //发送_发送失败
        ;//逗号分隔
        final String message;
        Type(String message) {
            this.message = message;
        }
    }
    public OldAvatarException(OldAvatarException.Type type){
        super(type.message);
    }
}