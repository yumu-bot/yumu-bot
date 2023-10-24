package com.now.nowbot.throwable.ServiceException;

import com.now.nowbot.throwable.TipsException;

public class ScoreException extends TipsException {
    public enum Type {
        SCORE_Me_TokenExpired("您的令牌已过期，请重新授权。(!ymbind)"),//查询自己_玩家撤销授权
        SCORE_Player_NotFound("找不到此玩家，请检查。"), //获取玩家失败
        SCORE_Player_Banned("你号没了，或是 ppy API 无法访问。"), //获取玩家失败
        SCORE_Player_TokenExpired("此玩家的令牌已过期，请提醒他重新授权。(!ymbind)"), //玩家_对方未绑定
        SCORE_Player_NoScore ("找不到此玩家的成绩，请检查。"), //玩家_找不到分数
        SCORE_Score_FetchFailed ("成绩列表获取失败！"),
        SCORE_Score_RankError ("请输入正确的成绩编号！"),
        SCORE_Score_NotFound ("找不到这张谱面上的相应成绩，请检查。"), //分数_找不到分数
        SCORE_Score_OutOfRange ("超出查询范围！"), //分数_找不到分数
        SCORE_Recent_NotFound ("找不到 24 小时内的成绩。"), //分数_找不到分数
        SCORE_Mod_NotFound ("找不到这张谱面上含有相应模组的成绩，请检查。"), //模组_找不到分数
        SCORE_Mode_NotFound("找不到这张谱面所属的游戏模式的成绩，请检查。"), //游戏模式_找不到分数
        SCORE_Mode_SpecifiedNotFound("找不到这张谱面特定的游戏模式的成绩，请检查。\n或者，不要指定游戏模式。(!s [bid])"), //游戏模式_找不到分数
        SCORE_Send_Error ("成绩发送失败，请重试。") //发送_发送失败

        ;//逗号分隔
        public final String message;
        Type(String message) {
            this.message = message;
        }
    }
    public ScoreException(ScoreException.Type type){
        super(type.message);
    }
}
