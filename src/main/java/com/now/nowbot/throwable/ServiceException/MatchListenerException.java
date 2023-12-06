package com.now.nowbot.throwable.ServiceException;

import com.now.nowbot.throwable.TipsException;

public class MatchListenerException extends TipsException {
    public enum Type {
        ML_Parameter_None("欢迎使用 Yumu Match Listener 系统！指令食用方法：\n!ymmatchlistener / !ymml [matchid] (operate)\nmatchid：这场比赛的房间号。\noperate：操作。可输入 start，s，stop，p。默认开始监听（start）。"),//参数_无参数
        ML_MatchID_RangeError("输入的房间号范围错误！"),
        ML_MatchID_NotFound("小沐找不到这场比赛..."),

        ML_Match_Start("比赛 %s 已开始！谱面：\n%s"),
        ML_Match_End("比赛已经结束..."),
        ML_Listen_Start("开始监听比赛 %s"),
        ML_Listen_NotListen("您还没有监听比赛呢！请使用 !ml [matchid] start 开始监听！"),
        ML_Listen_MaxInstance("您的监听实例已到达最大数量。"),
        ML_Listen_MaxInstanceGroup("这个群的监听实例已到达最大数量。"),
        ML_Listen_AlreadyInListening("您正在监听 %s 这场比赛！如果想停止监听可输入 !ml [matchid] stop。"),
        ML_Listen_AlreadyInListeningGroup("这个群正在监听 %s 这场比赛！如果想停止监听可输入 !ml [matchid] stop。"),
        ML_Listen_AlreadyInListeningOthers("其他人正在监听 %s 这场比赛！如果想停止监听可输入 !ml [matchid] stop。"),
        ML_Listen_StopRequest("收到停止监听 %s 的请求"),
        ML_Listen_Stop("停止监听 %s：%s。"), //标准停止输出

        ML_Send_NotGroup("请在群聊中使用这个功能！"),
        ML_Send_Error("对局信息发送失败。\n请耐心等待问题修复。")

        ;//逗号分隔
        public final String message;
        Type(String message) {
            this.message = message;
        }
    }
    public MatchListenerException(MatchListenerException.Type type){
        super(type.message);
    }
    public MatchListenerException(MatchListenerException.Type type, Object... args){
        super(String.format(type.message, args));
    }
}
