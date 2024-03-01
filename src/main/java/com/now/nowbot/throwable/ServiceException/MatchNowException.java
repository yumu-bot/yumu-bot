package com.now.nowbot.throwable.ServiceException;

import com.now.nowbot.throwable.TipsException;

public class MatchNowException extends TipsException {
    public enum Type {
        MN_MatchId_Error("请输入正确的房间号 (MatchID)！"),
        MN_Match_NotFound("找不到这场比赛！\n也许是这场比赛已经被众人遗忘了，或是 ppy API 无法访问。"),
        MN_Match_Empty("TNND，为什么一张谱面都没有？"),//这场比赛是空的！
        MN_Match_ParseError("对局过滤错误，请修改参数！"),
        MN_Match_OutOfBoundsError("输入的参数已经过滤掉了所有对局！"),
        MN_Render_Error("MatchNow 渲染图片超时，请重试，或者将问题反馈给开发者。"), //发送_发送失败
        MN_Send_Error("MatchNow 发送超时，请重试，或者将问题反馈给开发者。") //发送_发送失败
        ;//逗号分隔
        final String message;
        Type(String message) {
            this.message = message;
        }
    }
    public MatchNowException(MatchNowException.Type type){
        super(type.message);
    }
}