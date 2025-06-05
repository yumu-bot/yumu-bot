package com.now.nowbot.throwable.botException;

import com.now.nowbot.throwable.TipsException;

// 负责 InfoCardService 和 ScorePRCardService
public class MiniCardException extends TipsException {
    public enum Type {
        MINI_Me_TokenExpired("您的令牌已过期，请重新授权。(!ymbind)"),
        MINI_QQ_NotFound("找不到 %s 绑定的玩家！"),
        MINI_Player_TokenExpired("此玩家的令牌已过期，请提醒他重新授权。(!ymbind)"),
        MINI_Player_NotFound("找不到玩家 %s！"),
        MINI_Recent_NotFound("找不到玩家 %s 在 24 小时内的成绩。"),
        MINI_Score_RankError("请输入正确的成绩编号！"),
        MINI_Map_FetchError("获取谱面数据失败。"),
        MINI_Classification_Error("迷你面板分类失败。\n请重试，或者将信息反馈给开发者。"),
        MINI_Render_Error("迷你面板渲染失败。"),
        MINI_Send_Error("迷你面板发送失败。\n请耐心等待问题修复。"),
        MINI_Deprecated_X("!ymx 迷你成绩面板已移至 !rc 或 !pc。"),
        MINI_Deprecated_Y("!ymy 迷你信息面板已移至 !ic。"),

        ;
        public final String message;
        Type(String message) {
            this.message = message;
        }
    }
    public MiniCardException(MiniCardException.Type type){
        super(type.message);
    }

    public MiniCardException(MiniCardException.Type type, Object... args){
        super(String.format(type.message, args));
    }
}