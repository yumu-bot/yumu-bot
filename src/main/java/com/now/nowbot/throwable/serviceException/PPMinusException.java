package com.now.nowbot.throwable.serviceException;

import com.now.nowbot.throwable.TipsException;

public class PPMinusException extends TipsException {
    public enum Type {
        PM_Me_TokenExpired("您的令牌已过期，请重新授权。(!ymbind)"),
        PM_Me_PlayTimeTooShort("你在 %s 模式上的游戏时长太短了，快去多玩几局吧！"),
        PM_Me_NotFound("找不到你所绑定的玩家。"),
        PM_Me_FetchFailed("获取你的 PP- 失败，有可能是你输错了玩家名，或是指定了错误的模式。"),
        PM_Player_TokenExpired("对方玩家的令牌已过期，请提醒他重新授权。(!ymbind)"), //玩家_对方未绑定
        PM_Player_NotFound("找不到此玩家，请检查。"), //获取玩家失败
        PM_Player_VSNotFound("哩个瓜娃子到底要 VS 哪个哦！"),//未搜到vs玩家
        PM_Player_PlayTimeTooShort("此玩家在 %s 模式上的游戏时长太短了，快去多玩几局吧！"),//查询他人_玩家号太新
        PM_Player_FetchFailed("获取对方的 PP- 失败，有可能是你输错了玩家名，或是指定了错误的模式。"), //获取PPM失败
        //PPM_Default_ManiaComingSoon("等哪天 mania 社区风气变好了，或许就有 PPM-mania 了吧...\n要不你给我出出主意（色眯眯的笑容）"),//Mania模式查询未开放
        PM_BPList_FetchFailed("获取 BP 失败，请重试。"),

        PM_Render_Error("PP- 渲染失败，请重试，或耐心等待问题修复。"),
        PM_Send_Error("PP- 发送失败，请重试。"),

        PM_Calculate_Error("PP- 计算出错，请等待问题修复。"),

        PM_Test_Empty("数据是空的！"),
        PM_Test_SendError("TESTPPM 文件发送失败，请重试。\n请耐心等待问题修复。"),
        PM_Test_NotGroup("请在群聊中使用这个功能！"),

        ;
        public final String message;
        Type(String message) {
            this.message = message;
        }
    }
    public PPMinusException(Type type){
        super(type.message);
    }
    public PPMinusException(Type type, Object... args){
        super(String.format(type.message, args));
    }
}
