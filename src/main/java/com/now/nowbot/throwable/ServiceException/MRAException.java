package com.now.nowbot.throwable.ServiceException;

import com.now.nowbot.throwable.TipsException;

public class MRAException extends TipsException {
    public enum Type {
        RATING_Parameter_None("欢迎使用Yumu Rating系统！\n指令食用方法：!ymrating / !ymra [matchid] (warmup) (delete) (r?) (f?)\nmatchid：这场比赛的房间号。\nwarmup：需要抛弃的前面场次（热手）数量，默认为0。\ndelete：需要抛弃的后面场次（表演赛）数量，默认为0。\nr?：是否清除重赛场次？\nf?：是否计算失败场次？"),//参数_无参数
        RATING_Parameter_Error("输入的参数范围错误！"),//参数_参数错误

        RATING_Parameter_MatchIDNotFound("小沐找不到这场比赛哦！\n请检查房间号是否正确、房间记录是否过期！"),//参数_MRA模式_比赛未找到
        RATING_Parameter_SeriesIDNotFound("小沐找不到这一系列比赛中的“{}”比赛哦！\n请检查房间号是否正确、房间记录是否过期！"),//参数_SRA模式_比赛未找到
        RATING_Parameter_WarmupError("你要在未来热手吗？"),//参数_热手数量负数或者小数
        RATING_Parameter_TieBreakerError("小沐无法推算未来的比赛哦"),//参数_结尾数量负数或者小数

        RATING_Client_Calculating("正在全力计算中..."),//斗力_计算
        RATING_Client_CalculatingFailed("算不出来了，请重试..."),//斗力_计算失败

        RATING_Warning_MatchNotFinished("探测到当前比赛尚未结束！\n"),//警告_比赛未结束
        RATING_Warning_MatchNotFinishedGuide("输入 !sudo ymra 强制忽略冲突并计算斗力！\n十分钟内有效"),//警告_比赛未结束指引
        RATING_Warning_UnbalancedTeam("探测到部分场次缺失玩家分数！\n"),//警告_缺失玩家
        RATING_Warning_UnbalancedTeamInformation("\n在谱面{}(第{}场)中，{}队缺失{}名玩家！"),//警告_缺失玩家信息，括号1填写谱面名称，括号2填写场次位置
        RATING_Warning_SeriesUnbalancedTeamInformation("\n在谱面{}(对局{}的第{}场)中，{}队缺失{}名玩家！"),//警告_缺失玩家信息，括号1填写谱面名称，括号2填写matchid
        RATING_Warning_UnbalancedTeamGuide("\n请按照格式 !ymrf (玩家1的uid) (玩家1的分数) (玩家1的连击) (玩家2的uid)...\n输入补足信息！\n或者输入 !sudo ymra 强制忽略冲突并计算斗力！\n十分钟内有效"),//警告_缺失玩家指引
        RATING_Warning_InconsistentWinCondition("探测到计分条件冲突！\n"),//警告_计分冲突
        RATING_Warning_InconsistentWinConditionGuide("\n输入 !sudo ymra 强制忽略冲突并计算斗力！\n十分钟内有效"),//警告_计分冲突指引

        RATING_Me_NoAuthorization("您撤销了授权呢，请输入 !bind 点击链接登录，重新授权吧"),//查询自己_玩家撤销授权
        RATING_Me_Banned("哼哼，你号没了"),//查询自己_玩家被封禁
        RATING_Me_Blacklisted("本 Bot 根本不想理你"),//查询自己_玩家黑名单

        RATING_Default_NoToken("哼，你 Token 失效啦！看在我们关系的份上，就帮你这一次吧！"),//token不存在，使用本机AccessToken

        RATING_MRA_Error("MRA 渲染图片超时，请重试。\n请耐心等待问题修复。"),
        RATING_URA_Error("URA 输出失败，请重试。\n或尝试最新版渲染 !ra <mpid>。"),
        RATING_CRA_Error("CRA 输出失败，请重试。\n或尝试文字版渲染 !ura <mpid>。"),

        RATING_CRA_MatchIDNotFound("CRA 请输入正确的房间号！"),
        RATING_CRA_NotGroup("CRA 请在群聊中使用！"),
        ;//逗号分隔
        final String message;
        Type (String message) {
            this.message = message;
        }
        }
    public MRAException(MRAException.Type type){
        super(type.message);
    }
}