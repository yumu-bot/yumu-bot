package com.now.nowbot.throwable

// 这个类存放一些公用的 Tips，比如图片发送失败，或是图片渲染失败，或是连不上渲染模块
class GeneralTipsException : TipsException {
    enum class Type (val message: String) {
        G_TokenExpired_Me("令牌失效，请重新授权。(!ymbind)"),
        G_TokenExpired_Player("玩家令牌失效，请提醒他重新授权。(!ymbind)"),
        G_TokenExpired_Official("令牌失效，请重新授权。(/bind [your name])"),
        G_TokenExpired_Cancel("绑定已经被取消了, 无法再使用了哦"),
        G_NotBind_Player("该玩家没有绑定。"),

        G_Banned_Me("你被办了。"), 
        G_Banned_Player("%s 被办了。"),

        G_Restricted_NSFW("这种歌曲，还是有点唱不出口呢。"),

        G_Maimai_YouForbidden("您已禁止其他人获取数据。"), 
        G_Maimai_PlayerForbidden("该玩家已禁止其他人获取数据。"), 
        G_Maimai_YouBadRequest("您的 QQ 似乎并没有和您的水鱼账号绑定。"), 
        G_Maimai_QQBadRequest("这个 QQ 似乎并没有和该玩家的水鱼账号绑定。"), 
        G_Maimai_NameBadRequest("找不到叫这个名字的水鱼账号。"), 
        G_Maimai_NoDeveloperToken("机器人没有配置开发者授权，暂时无法获取。"), 
        
        G_Exceed_Param("请输入正确的编号或范围！"), 
        G_Exceed_Day("你输入的天数范围太久远了！"),
        G_Exceed_Score("符合筛选要求的成绩太多了！请缩小查询范围。"),
        G_Exceed_Score_Count("符合筛选要求的成绩太多了 (%s 个)！请缩小查询范围。"),
        G_Exceed_Version("符合筛选版本的成绩太多了！请减少版本，缩小查询范围。"), 
        G_Exceed_Version_Default("符合筛选版本的成绩太多了！请指定难度（\":\" + \"b、a、e、m、r\"），缩小查询范围。"), 
        
        G_Null_Result("没有找到结果！"), 
        G_Null_ResultNotAccurate("找不到可能的结果...试试输入更常见的外号或准确的歌曲编号吧。"), 
        G_Null_Param("请输入正确的参数！"), 
        G_Null_UserName("请输入正确的玩家名！"), 
        G_Null_BID_Quotation("您在使用引号包括住玩家名时，也需要输入谱面的曲名或是编号！"),
        G_Null_Audio("请输入想要试听的 bid 或者 sid！\n(!a <bid> / !a:s <sid>)"),
        G_Null_BID("请输入正确的谱面编号 (BID)！"),
        G_Null_SID("请输入正确的谱面编号 (SID)！"),
        G_Null_AudioDownload("试听音频下载失败。\n也许是谱面输入错误，或者谱面被版权限制了。\n自己去官网听算了。"),
        G_Null_Song("找不到歌曲 %s，请检查。"),
        G_Null_PlayerTeam("找不到玩家 %s 所属的战队，请检查。"),
        G_Null_Team("找不到战队 %s，请检查。"),
        G_Null_Map("找不到这张谱面，请检查。"), 
        G_Null_MapFile("没有找到这张谱面的缓存文件。"), 
        G_Null_MatchID("请输入正确的比赛编号！"), 
        G_Null_MatchRound("找不到这场对局，请检查。"), 
        G_Null_QQ("找不到 %s 所绑定的玩家！"), 
        G_Null_Play("该玩家基本没玩过。"), 
        G_Null_PlayerUnknown("找不到玩家，请检查。"), 
        G_Null_Player("找不到玩家 %s，请检查。"), 
        G_Null_PlayerRecord("该玩家在 %s 模式内基本没玩过。"), 
        G_Null_PlayerInactive("玩家 %s 最近不活跃..."),
        G_Null_BP("无法获取玩家 %s 的最好成绩..."),
        G_Null_Badge("%s还没有主页奖牌呢..."),
        G_Null_FilterBP("无法获取玩家 %s 符合条件的最好成绩。"), 
        G_Null_ModeBP("无法获取玩家 %s 在 %s 模式内指定的最好成绩。"),
        G_Null_TheoreticalBP("您（选中）的最好成绩已经全是理论值了！"),
        G_Null_LeaderBoard("谱面 %s 没有榜，无法获取成绩。"),
        G_Null_Score("找不到您在谱面 %s 内的成绩，请检查。"), 
        G_Null_Version("找不到您在版本 %s 内的成绩，请检查。"), 
        G_Null_RecentScore("找不到玩家 %s 在 %s 模式内的最近成绩，请检查。"), 
        G_Null_SpecifiedMode("找不到这张谱面在指定 %s 模式内的成绩，请检查。\n或者，不要设定游戏模式，让机器人自行处理。"),

        G_NotEnoughBP_Me("您在 %s 模式上的最好成绩数量不够呢...\n灼热分析 EX"),
        G_NotEnoughBP_Player("对方在 %s 模式上的最好成绩数量不够呢...\n灼热分析 EX"),

        G_Wrong_Instruction("指令错误，请检查。"),
        G_Wrong_S("%s错误，请检查。"),
        G_Wrong_ParamOnly("仅支持使用 %s 逻辑运算符。"),
        G_Wrong_Cabbage("如果你给他...传递一些完全看不懂的参数...你等于...你也等于...你也有泽任吧？"),

        G_Empty_Score("您的成绩是空的！"),
        G_Empty_TodayBP("玩家 %s 今天之内没有新增的 BP 呢...\n尝试修改范围，或尝试扩大搜索天数吧。"), 
        G_Empty_PeriodBP("玩家 %s 这段时间之内，在 %s 模式里都没有新增的 BP 呢...\n尝试修改范围，或扩大搜索天数吧。"), 
        
        G_Fetch_PlayerInfo("玩家信息获取失败，请重试。"), 
        G_Fetch_List("列表获取失败，请重试。"), 
        G_Fetch_BeatMap("谱面获取失败，请重试。"),
        G_Fetch_Discussion("讨论区获取失败，请重试。"),
        G_Fetch_BeatMapAttr("获取变化的谱面数据超时（太多了），如果你是第一次见到这条消息，第二次通常就会恢复了。"),

        G_Suggest_AnotherFunction("推荐您使用另一个功能：%s"),

        G_Malfunction_Response("没有响应呢，请稍后再试。"),
        G_Malfunction_ppyAPI("ppy API 状态异常！"),
        G_Malfunction_Calculate("%s：数据计算失败。"), 
        G_Malfunction_Fetch("%s：数据获取失败。"), 
        G_Malfunction_Render("%s：渲染模块连接失败。"),
        G_Malfunction_RenderTooMany("%s：渲染模块连接超时（请求数据太多了）。\n如果你是第一次见到这条消息，第二次通常就会恢复了。\n如果你多次见到这条消息，可以尝试一会后重试。"),
        G_Malfunction_Send("%s：图片发送失败。\n请耐心等待问题修复。"), 
        G_Malfunction_IOException("%s：遇到了文件读取异常，请重试。"), 
        G_Malfunction_Classification("%s：分类失败。"), 
        
        G_Permission_Super("权限不足！只有超级管理员 (OP，原批) 可以使用此功能！"), 
        G_Permission_Group("权限不足！只有群聊管理员或群主可以使用此功能！"), 
        
        G_Success_RefreshFile("已成功刷新谱面 %s 的所有相关联的 %s 个缓存文件。"),
    }
    
    constructor(message: String?) : super(message)
    
    constructor(type: Type) : super(type.message)
    
    constructor(type: Type, vararg args: Any?) : super(type.message, *args)
    
    constructor(image: ByteArray?) : super(image)
}
