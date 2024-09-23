package com.now.nowbot.throwable.serviceException;

import com.now.nowbot.throwable.TipsException;

public class MRAException extends TipsException {
    public enum Type {
        RATING_Instructions("""
        欢迎使用 Yumu Rating 功能！食用方法：
        !ymrating / !ra [matchid] (warmup) (delete) (r?) (f?)
        matchid：这场比赛的房间号。
        warmup：需要抛弃的前面场次（热手）数量，默认为0。
        delete：需要抛弃的后面场次（表演赛）数量，默认为0。
        r?：清除重赛场次？
        f?：不计失败成绩？
        """),//参数_无参数

        RATING_Series_Instructions("""
        欢迎使用 Yumu Series Rating 功能！食用方法：
        !ymseries / !sa [[matchid] (warmup) (delete)] (r?) (f?)
        matchid：这场比赛的房间号，可按组多次输入。
        warmup：需要抛弃的前面场次（热手）数量，默认为0，可按组多次输入。
        delete：需要抛弃的后面场次（表演赛）数量，默认为0，可按组多次输入。
        r?：清除重赛场次？
        f?：不计失败成绩？
        """),//参数_无参数

        RATING_Permission_OnlyGroupAdmin("只有群聊管理员或群主可以使用此功能！"),

        RATING_Parameter_MatchIDError("输入的比赛房间参数范围错误！"),//参数_参数错误
        RATING_Parameter_SkipError("你要在虚空中热手吗？"),//参数_热手数量负数或者小数
        RATING_Parameter_SkipEndError("小沐无法推算未来的比赛哦"),//参数_结尾数量负数或者小数

        RATING_Parameter_EasyError("输入的 Easy 参数范围错误！"),
        RATING_Parameter_EasyTooLarge("你是 Easy 皇帝吗？这么高的倍率。"),//参数_参数错误
        RATING_Parameter_EasyTooSmall("你是 HardRock 皇帝吗？这么低的倍率。"),//参数_参数错误

        RATING_Parse_ParameterError("输入的参数范围错误！\n错误参数：%s，错误位置：%s"),//参数_参数错误
        RATING_Parse_MissingMatch("看起来漏了一场比赛呢？\n这个参数之前缺失比赛房间号：%s，错误位置：%s"),//参数_参数错误
        RATING_Parse_MissingRemove("看起来漏了移除结束符呢？\n这个参数之前缺失结束符：%s，错误位置：%s"),//参数_参数错误

        RATING_Round_Empty("房间内没有对局呢..."),//参数_RRA模式_比赛未找到
        RATING_Round_NotFound("小沐找不到这场对局..."),//参数_RRA模式_比赛未找到
        RATING_Round_BeatmapNotFound("小沐找不到这场对局使用的谱面..."),//参数_RRA模式_谱面未找到
        RATING_Match_NotFound("小沐找不到这场比赛哦！\n请检查房间号是否正确、房间记录是否过期！"),//参数_MRA模式_比赛未找到
        RATING_Series_TooManyMatch("一次性输入的对局太多！计算的时候可能会遇到 API 瓶颈。"),//参数_SRA模式_比赛太多
        RATING_Series_TooManyRequest("API 调用达到上限。请稍后重试。\n当前已经查询到：%s"),//参数_SRA模式_比赛太多
        RATING_Series_ReachThreshold("遇到 API 瓶颈！等待 10 秒后再次尝试获取！"),//参数_SRA模式_比赛太多
        RATING_Series_NotFound("小沐找不到这一系列比赛中的 %s 哦！\n请检查房间号是否正确、房间记录是否过期！"),//参数_SRA模式_比赛未找到
        RATING_Series_FetchFailed("系列比赛获取失败！"),//参数_SRA模式_比赛未找到
        RATING_Series_SleepingInterrupted("触发休眠时异常中断！请重试！"),//参数_SRA模式_休眠中断

        RATING_Series_Progressing("正在处理系列赛"),//斗力_计算

        RATING_Rating_Calculating("正在全力计算中..."),//斗力_计算
        RATING_Rating_CalculatingFailed("算不出来了，请重试..."),//斗力_计算失败

        RATING_Warning_MatchNotFinished("探测到当前比赛尚未结束！\n"),//警告_比赛未结束
        RATING_Warning_MatchNotFinishedGuide("输入 !sudo ymra 强制忽略冲突并计算斗力！\n十分钟内有效"),//警告_比赛未结束指引
        RATING_Warning_UnbalancedTeam("探测到部分场次缺失玩家分数！\n"),//警告_缺失玩家
        RATING_Warning_UnbalancedTeamInformation("\n在谱面{}(第{}场)中，{}队缺失{}名玩家！"),//警告_缺失玩家信息，括号1填写谱面名称，括号2填写场次位置
        RATING_Warning_SeriesUnbalancedTeamInformation("\n在谱面{}(对局{}的第{}场)中，{}队缺失{}名玩家！"),//警告_缺失玩家信息，括号1填写谱面名称，括号2填写matchid
        RATING_Warning_UnbalancedTeamGuide("\n请按照格式 !ymrf (玩家1的uid) (玩家1的分数) (玩家1的连击) (玩家2的uid)...\n输入补足信息！\n或者输入 !sudo ymra 强制忽略冲突并计算斗力！\n十分钟内有效"),//警告_缺失玩家指引
        RATING_Warning_InconsistentWinCondition("探测到计分条件冲突！\n"),//警告_计分冲突
        RATING_Warning_InconsistentWinConditionGuide("\n输入 !sudo ymra 强制忽略冲突并计算斗力！\n十分钟内有效"),//警告_计分冲突指引

        RATING_Me_TokenExpired("您的令牌已过期，请重新授权。(!ymbind)"),//查询自己_玩家撤销授权
        RATING_Me_Banned("你号没了，或是 ppy API 无法访问。"),//查询自己_玩家被封禁
        RATING_Me_Blacklisted("本 Bot 根本不想理你。"),//查询自己_玩家黑名单

        RATING_Default_NoToken("哼，你 Token 失效啦！看在我们关系的份上，就帮你这一次吧！"),//token不存在，使用本机AccessToken

        RATING_Send_SRAFailed("SRA 渲染图片超时，请重试。\n请耐心等待问题修复。"),
        RATING_Send_MRAFailed("MRA 渲染图片超时，请重试。\n请耐心等待问题修复。"),
        RATING_Send_USAFailed("USA 输出失败，请重试。\n或尝试 !sa <mpids>。"),
        RATING_Send_CSAFailed("CSA 输出失败，请重试。\n或尝试 !sa <mpids>。"),
        RATING_Send_URAFailed("URA 输出失败，请重试。\n或尝试 !ra <mpid>。"),
        RATING_Send_CRAFailed("CRA 输出失败，请重试。\n或尝试 !ura <mpid>。"),

        RATING_Send_NotGroup("请在群聊中使用这个功能！"),
        ;//逗号分隔
        public final String message;
        Type (String message) {
            this.message = message;
        }
    }
    public MRAException(MRAException.Type type){
        super(type.message);
    }
    public MRAException(MRAException.Type type, Object... args){
        super(String.format(type.message, args));
    }
}