package com.now.nowbot.throwable.serviceException;

import com.now.nowbot.throwable.TipsException;

public class FriendException extends TipsException {
    public enum Type {
        FRIEND_Me_NoBind("您还没有绑定呢..."),//查询自己_玩家未绑定
        FRIEND_Me_TokenExpired("您的令牌已过期，请重新授权。(!ymbind)"),//查询自己_玩家撤销授权
        FRIEND_Me_NotFound ("找不到您绑定的 osu! 玩家！"),
        FRIEND_Me_FetchFailed("获取好友列表失败，请耐心等待问题修复。"),//获取失败
        FRIEND_Me_NoPermission("权限不足, 没有oauth授权！\n!ymbind + username 是无法使用这个功能的。\n请仅输入 !ymbind（不要输入用户名），并且点击链接绑定。"),
        FRIEND_Client_ParameterOutOfBounds("参数范围错误！\n请输入一个自然数，\n或输入差值不超过 100 的两个自然数！"),//查询_参数错误
        FRIEND_Client_NoFriend("我叫奇异博士找遍了 2000 条世界线，\n都没找到你的游戏好友。"),//查询_好友太少
        FRIEND_Send_Error("好友发送失败，请耐心等待问题修复。"),//默认报错
        ;//逗号分隔
        public final String message;
        Type(String message) {
            this.message = message;
        }
        }
    public FriendException(FriendException.Type type){
        super(type.message);
    }
}