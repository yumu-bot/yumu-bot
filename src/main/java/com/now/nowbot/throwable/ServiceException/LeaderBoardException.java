package com.now.nowbot.throwable.ServiceException;

import com.now.nowbot.throwable.TipsException;

public class LeaderBoardException extends TipsException {
    public enum Type {
        LIST_Parameter_NoBid("请输入 bid！"),
        LIST_Parameter_BidError("请输入正确的 bid！"),
        LIST_Parameter_RangeError("请输入正确的范围！(1~50)"),
        LIST_Map_NotFound ("找不到这张谱面，请检查。"),
        LIST_Map_NotRanked ("这张谱面暂未上架呢..."),
        LIST_Score_FetchFailed ("成绩获取失败，请重试。"),
        LIST_Score_NotFound ("找不到这张谱面上的成绩，请检查。"),
        LIST_Send_Error ("榜单发送失败，请耐心等待问题修复。")

        ;//逗号分隔
        final String message;
        Type(String message) {
            this.message = message;
        }
    }
    public LeaderBoardException(LeaderBoardException.Type type){
        super(type.message);
    }
}
