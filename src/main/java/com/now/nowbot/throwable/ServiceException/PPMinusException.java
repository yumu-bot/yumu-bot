package com.now.nowbot.throwable.ServiceException;

import com.now.nowbot.throwable.TipsException;

public class PPMinusException extends TipsException {
    public enum Type {
        PPM_Me_TokenExpired("您的令牌已过期，请重新授权。(!ymbind)"),//查询自己_玩家未绑定
        PPM_Me_PlayTimeTooShort("你在 %s 模式上的游戏时长太短了，快去多玩几局吧！"),//查询自己_玩家号太新
        PPM_Me_NotFound("找不到你所绑定的玩家。"), //获取玩家失败
        PPM_Me_FetchFailed("获取你的 PPM 失败，有可能是你输错了玩家名，或是指定了错误的模式。"), //获取PPM失败
        PPM_Player_TokenExpired("此玩家的令牌已过期，请提醒他重新授权。(!ymbind)"), //玩家_对方未绑定
        PPM_Player_NotFound("找不到此玩家，请检查。"), //获取玩家失败
        PPM_Player_VSNotFound("哩个瓜娃子到底要 VS 哪个哦！"),//未搜到vs玩家
        PPM_Player_PlayTimeTooShort("他在 %s 模式上的游戏时长太短了，快去催他多玩几局吧！"),//查询他人_玩家号太新
        PPM_Player_FetchFailed("获取对方的 PPM 失败，有可能是你输错了玩家名，或是指定了错误的模式。"), //获取PPM失败
        //PPM_Default_ManiaComingSoon("等哪天 mania 社区风气变好了，或许就有 PPM-mania 了吧...\n要不你给我出出主意（色眯眯的笑容）"),//Mania模式查询未开放

        PPM_Calculate_Error("PPM 计算出错，请等待问题修复。"),//默认报错
        PPM_Default_Error("PPM 渲染图片超时，请重试。\n请耐心等待问题修复。"),//默认报错
        PPM_Test_Empty("数据是空的！"),
        PPM_Test_SendError("TESTPPM 文件发送失败，请重试。\n请耐心等待问题修复。"),
        PPM_Test_NotGroup("请在群聊中使用这个功能！"),
        ;//逗号分隔
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
