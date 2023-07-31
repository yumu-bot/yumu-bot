package com.now.nowbot.throwable.ServiceException;

import com.now.nowbot.throwable.TipsException;

public class ScoreException extends TipsException {
    public static enum Type {
        SCORE_Player_NotFound("找不到此玩家，请检查。"), //获取玩家失败
        SCORE_Player_NoBind ("对方还没有绑定呢，请提醒他输入 !bind 点击链接登录，完成绑定吧"), //玩家_对方未绑定
        SCORE_Score_NotFound ("找不到这张谱面上的相应成绩，请检查。"), //分数_找不到分数
        SCORE_Recent_NotFound ("找不到24小时内的成绩。"), //分数_找不到分数
        SCORE_Mod_NotFound ("找不到这张谱面上含有相应模组的成绩，请检查。"), //模组_找不到分数
        SCORE_Mode_MainNotFound ("找不到这张谱面主游戏模式的成绩，请检查。"), //游戏模式_找不到分数
        SCORE_Mode_NotFound("找不到这张谱面对应游戏模式的成绩，请检查。\n或者，不要指定游戏模式。(!s [bid])"), //游戏模式_找不到分数
        SCORE_Send_Error ("成绩发送失败，请重试。") //发送_发送失败

        ;//逗号分隔
        String message;
        Type(String message) {
            this.message = message;
        }
    }
    public ScoreException(ScoreException.Type type){
        super(type.message);
    }
}
