package com.now.nowbot.throwable.ServiceException;

import com.now.nowbot.throwable.TipsException;

public class MonitorNowException extends TipsException {
    public enum Type {
        MN_MatchId_Error("请输入正确的 MatchID！"),
        MN_Match_NotFound("找不到这场比赛！\n也许是这场比赛已经被众人遗忘了。"),
        MN_Match_Empty("TNND，为什么一张谱面都没有？"),//这场比赛是空的！
        MN_Match_OutOfBoundsError("输入的参数已经过滤掉了所有对局！"),
        MN_Send_Error("MonitorNow 渲染图片超时，请重试，或者将问题反馈给开发者。") //发送_发送失败
        ;//逗号分隔
        final String message;
        Type(String message) {
            this.message = message;
        }
    }
    public MonitorNowException(MonitorNowException.Type type){
        super(type.message);
    }
}