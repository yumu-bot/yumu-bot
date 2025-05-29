package com.now.nowbot.throwable.serviceException

import com.now.nowbot.throwable.TipsException

class MatchListenerException : TipsException {
    enum class Type(val message: String) {
        ML_Instructions(
            """
                欢迎使用 Yumu Match Listener 功能！食用方法：
                !ymmatchlistener / !ymml [matchid] (operate)
                matchid：这场比赛的房间号。
                operate：操作。可输入 start，s，stop，p。默认开始监听（start）。
                
                """.trimIndent()
        ),  //参数_无参数
        ML_MatchID_Null("请输入正确的房间号！"),
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
        ML_Listen_Aborted("上一场对局被强制结束了。"),
        ML_Listen_Stop("停止监听 %s：%s。"),  //标准停止输出
        ML_Listen_StopGroup("已完全停止当前群聊的对局监听实例！"),
        ML_Listen_StopAll("已完全停止所有对局监听实例！"),

        ML_Info_NoListener("本群没有监听实例。"),
        ML_Info_List("本群的监听对局有：%s"),

        ML_Send_NotGroup("请在群聊中使用这个功能！"),
        ML_Send_Error("对局监听图片发送失败。\n请耐心等待问题修复。"),
    }

    constructor(type: Type) : super(type.message)
    constructor(type: Type, vararg args: Any?) : super(String.format(type.message, *args))
}
