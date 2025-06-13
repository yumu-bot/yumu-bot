package com.now.nowbot.throwable

// 这个类存放一些公用的 Tips，比如图片发送失败，或是图片渲染失败，或是连不上渲染模块
class GeneralTipsException : TipsException {
    enum class Type (val message: String) {

        G_Restricted_NSFW("这种歌曲，还是有点唱不出口呢。"),
        G_Restricted_Group("请在群聊中使用这个功能！"),

        G_Exceed_Param("请输入正确的编号或范围！"),

        G_Null_ResultNotAccurate("没有找到可能的结果...试试输入更常见的外号或准确的歌曲编号吧。"),
        G_Null_UserName("请输入正确的玩家名！"), 
        G_Null_BID_Quotation("您在使用引号包括住玩家名时，也需要输入谱面的曲名或是编号！"),
        G_Null_Song("没有找到歌曲 %s。"),
        G_Null_PlayerTeam("没有找到玩家 %s 所属的战队。"),
        G_Null_Team("没有找到战队 %s。"),
        G_Null_Map("没有找到这张谱面。"),
        G_Null_MapLeaderBoard("这张谱面没有榜单。"),
        G_Null_MapFile("没有找到这张谱面的缓存文件。"), 
        G_Null_MatchID("请输入正确的比赛编号！"), 
        G_Null_MatchRound("没有找到这场对局。"),
        G_Null_Play("该玩家基本没玩过。"),
        G_Null_ModePlay("该玩家在 %s 模式内基本没玩过。"),
        G_Null_PlayerUnknown("没有找到玩家。"), 
        G_Null_Player("没有找到玩家 %s。"),
        G_Null_PlayerReverse("没有找到玩家 %s。\n你可能把范围和玩家名输反了。"),
        G_Null_BIDReverse("没有找到玩家 %s。\n你可能把谱面号和玩家名输反了。"),
        G_Null_Recent("无法获取玩家 %s 在 %s 模式内的最近成绩。"),
        G_Null_SelectedRecent("无法获取玩家 %s 在 %s 模式内指定的最近成绩。"),
        G_Null_BP("无法获取玩家 %s 的最好成绩..."),
        G_Null_Badge("%s 还没有主页奖牌呢..."),
        G_Null_FilterBP("无法获取玩家 %s 符合条件的最好成绩。"),
        G_Null_FilterRecent("无法获取玩家 %s 符合条件的最近成绩。"),
        G_Null_ModeBP("无法获取玩家 %s 在 %s 模式内指定的最好成绩。"),
        G_Null_TheoreticalBP("您（选中）的最好成绩已经全是理论值了！"),
        G_Null_LeaderBoard("谱面 %s 没有榜，无法获取成绩。"),
        G_Null_Score("没有找到您在谱面 %s 内的成绩。"),

        G_NotEnough_PlayTime("此玩家在 %s 模式上的游戏时长太短了，快去多玩几局吧！"),
        G_NotEnoughBP_Me("您在 %s 模式上的最好成绩数量不够呢...\n灼热分析 EX"),
        G_NotEnoughBP_Player("对方在 %s 模式上的最好成绩数量不够呢...\n灼热分析 EX"),
        
        G_Fetch_PlayerInfo("玩家信息获取失败，请重试。"), 
        G_Fetch_List("列表获取失败，请重试。"), 
        G_Fetch_BeatMap("谱面获取失败，请重试。"),
        G_Fetch_Discussion("讨论区获取失败，请重试。"),
        G_Fetch_BeatMapAttr("获取变化的谱面数据超时（太多了），如果你是第一次见到这条消息，再请求一次。通常来说会拿到结果。"),

        // G_Suggest_AnotherFunction("推荐您使用另一个功能：%s"),

        G_Malfunction_Response("没有响应呢..."),
        G_Malfunction_ppyAPI("ppy API 状态异常！"),
        G_Malfunction_Calculate("%s：数据计算失败。"), 
        G_Malfunction_Fetch("%s：数据获取失败。"), 
        G_Malfunction_Render("%s：渲染模块连接失败。"),
        G_Malfunction_APITooMany("%s：一次性输入的数据太多！获取信息的时候可能会遇到 API 瓶颈。"),
        G_Malfunction_Send("%s：图片发送失败。"),
        G_Malfunction_IOException("%s：文件读取失败。"),
        G_Malfunction_Classification("%s：分类失败。"),

        G_Success_NotOverRating("未超星。"),
        G_Success_RefreshFile("已成功刷新谱面 %s 的所有相关联的 %s 个缓存文件。"),
    }
    
    constructor(message: String?) : super(message)
    
    constructor(type: Type) : super(type.message)
    
    constructor(type: Type, vararg args: Any?) : super(type.message, *args)
    
    constructor(image: ByteArray?) : super(image)
}
