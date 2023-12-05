package com.now.nowbot.throwable.ServiceException;

import com.now.nowbot.throwable.TipsException;

public class MatchListenerException extends TipsException {
    public enum Type {
        ML_Parameter_None("欢迎使用 Yumu Match Listener 系统！指令食用方法：\n!ymmatchlistener / !ymml [matchid] (operate)\nmatchid：这场比赛的房间号。\noperate：操作。可输入 start，s，stop，p。默认开始监听（start）。"),//参数_无参数
        ML_MatchID_RangeError("输入的房间号范围错误！"),
        ML_MatchID_NotFound("小沐找不到这场比赛..."),

        MR_Match_End("比赛已经结束..."),
        MR_Match_NotListen("您还没有监听比赛呢！请使用 !ml [matchid] start 开始监听！"),

        ;//逗号分隔
        public final String message;
        Type(String message) {
            this.message = message;
        }
    }
    public MatchListenerException(MatchListenerException.Type type){
        super(type.message);
    }
}
