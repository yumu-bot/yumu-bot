package com.now.nowbot.throwable.serviceException;

import com.now.nowbot.throwable.TipsException;

public class RatingException extends TipsException {
    public static enum Type {
        RATING_Parameter_None("欢迎使用YuMu Rating功能！\n指令食用方法：!ymrating / !ymra (:ratingsystem) [matchid] (warmup) (isincludefail?) (isLeftRedTeam?)\n:ratingsystem：斗力系统。可选：木斗力:m，光斗力:g。不输入默认为木斗力。\nmatchid：这场比赛的房间号。\nwarmup：热手数量，默认为0。\nisincludefail?：是否计算失败，默认为1。\nisLeftRedTeam?：是否左红右蓝，默认为1."),//参数_无参数
        RATING_Parameter_RatingSystemWrong("阿叭叭叭叭叭\nratingsystem：斗力系统。可选：木斗力:m，光斗力:g。不输入默认为木斗力。记得带冒号。"),//参数_评分系统错误
        RATING_Parameter_MatchIDNotFound("小沐找不到这场比赛哦！\n请检查房间号是否正确、房间记录是否过期！"),//参数_MRA模式_比赛未找到
        RATING_Parameter_SeriesIDNotFound("小沐找不到这一系列比赛中的“{}”比赛哦！\n请检查房间号是否正确、房间记录是否过期！"),//参数_SRA模式_比赛未找到
        RATING_Parameter_WarmupWrong("你要在未来热手吗？"),//参数_热手数量负数或者小数

        RATING_Client_Calculating("正在全力计算中..."),//斗力_计算
        RATING_Client_CalculatingFailed("一七得七，二七一十四，三八妇女节，五一劳动节...\n算不出来了..."),//斗力_计算失败

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
        RATING_Default_PictureRenderFailed("我...我画笔坏了画不出图呃"),//图片渲染失败，或者绘图出错
        RATING_Default_PictureSendFailed("图片被麻花疼拿去祭天了"),//图片发送失败
        RATING_Default_DefaultException("我好像生病了，需要休息一会..."),//默认报错
        ;//逗号分隔
        String message;
        Type(String message) {
            this.message = message;
        }
        }
    public RatingException(RatingException.Type type){
        super(type.message);
    }
}