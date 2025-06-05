package com.now.nowbot.throwable.botException;

import com.now.nowbot.throwable.TipsException;

public class FriendException extends TipsException {
    public enum Type {
        FRIEND_Me_NoBind("您还没有绑定呢..."),
        FRIEND_Me_TokenExpired("您的令牌已过期，请重新授权。(!ymbind)"),
        FRIEND_Me_NotFound ("找不到您绑定的 osu! 玩家！"),
        FRIEND_Me_FetchFailed("获取好友列表失败，请耐心等待问题修复。"),
        FRIEND_Me_NoPermission("如果没有 Oauth 授权，就拿不到好友列表呢。(!ymbind)"),
        FRIEND_Client_ParameterOutOfBounds("参数范围错误！\n请输入一个自然数，\n或输入差值不超过 100 的两个自然数！"),
        FRIEND_Client_NoFriend("我叫奇异博士找遍了 2000 条世界线，\n都没找到你的游戏好友。"),
        FRIEND_Client_NoMatch("没有找到符合你的条件的游戏好友。"),
        FRIEND_Send_Error("好友发送失败，请耐心等待问题修复。"),
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