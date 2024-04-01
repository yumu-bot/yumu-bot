package com.now.nowbot.throwable.ServiceException;

import com.now.nowbot.throwable.TipsException;

public class ScoreException extends TipsException {
    public enum Type {
        SCORE_Me_TokenExpired("您的令牌已过期，请重新授权。(!ymbind)"),
        SCORE_QQ_NotFound("找不到 %s 所绑定的玩家！"),
        SCORE_Player_NotFound("找不到玩家 %s，请检查。"),
        SCORE_Player_Banned("你号没了。"),
        SCORE_Player_TokenExpired("此玩家的令牌已过期，请提醒他重新授权。(!ymbind)"),
        SCORE_Player_NoScore("找不到玩家 %s 的成绩，请检查。"),
        SCORE_Score_FetchFailed("成绩列表获取失败！"),
        SCORE_Score_RankError("请输入正确的成绩编号！"),
        SCORE_Score_NotFound("找不到 %s 这张谱面上的相应成绩，请检查。"),
        SCORE_Score_OutOfRange("超出查询范围！"),
        SCORE_Recent_NotFoundDefault("找不到 24 小时内的成绩。"),
        SCORE_Recent_NotFound("找不到玩家 %s 在 24 小时内的成绩。"),
        SCORE_Mod_NotFound("找不到这张谱面上含有相应模组的成绩，请检查。"),
        SCORE_ModList_NotFound("找不到这张谱面上含有 %s 的成绩，请检查。"),
        SCORE_Mode_NotFound("找不到谱面在默认游戏模式下的成绩，请检查。"),
        SCORE_Mode_SpecifiedNotFound("找不到这张谱面在指定 %s 模式内的成绩，请检查。\n或者，不要指定游戏模式，让 Bot 自行处理。"),
        SCORE_Render_Error("成绩渲染失败，请重试。"),
        SCORE_Send_Error("成绩发送失败，请重试。"),

        ;
        public final String message;

        Type(String message) {
            this.message = message;
        }
    }
    public ScoreException(ScoreException.Type type){
        super(type.message);
    }

    public ScoreException(ScoreException.Type type, Object... args){
        super(String.format(type.message, args));
    }
}
