package com.now.nowbot.throwable.ServiceException;

import com.now.nowbot.throwable.TipsException;

public class LeaderBoardException extends TipsException {
    public static enum Type {
        LIST_Player_NoBind ("对方还没有绑定呢，请提醒他输入 !bind 点击链接登录，完成绑定吧"),
        LIST_Map_BidError ("请输入正确的bid！"),
        LIST_Map_NotFound ("找不到这张谱面，请检查。"),
        LIST_Map_NotRanked ("这张谱面暂未上架呢..."),
        LIST_Score_FetchFailed ("成绩获取失败，请重试。"),
        LIST_Score_NotFound ("找不到这张谱面上的成绩，请检查。"),
        LIST_Send_Error ("榜单发送失败，请重试。")

        ;//逗号分隔
        String message;
        Type(String message) {
            this.message = message;
        }
    }
    public LeaderBoardException(LeaderBoardException.Type type){
        super(type.message);
    }
}
