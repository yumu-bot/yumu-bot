package com.now.nowbot.throwable;

// 这个类存放一些公用的 Tips，比如图片发送失败，或是图片渲染失败，或是连不上渲染模块
public class GeneralTipsException extends TipsException {
    public enum Type {
        G_TokenExpired_Me("您的令牌已过期，请重新授权。(!ymbind)"),
        G_TokenExpired_Player("此玩家的令牌已过期，请提醒他重新授权。(!ymbind)"),

        G_Banned_Me("号被办了。"),
        G_Banned_Player("他号被办了。"),

        G_Exceed_Param("请输入正确的编号或范围！"),
        G_Exceed_Day("你输入的天数范围太久远了！"),

        G_Null_Param("请输入编号或范围！"),
        G_Null_QQ("找不到 %s 所绑定的玩家！"),
        G_Null_Player("找不到玩家 %s，请检查。"),
        G_Null_PlayerRecord("该玩家在 %s 模式内基本没玩过。"),
        G_Null_PlayerInactive("玩家 %s 最近不活跃...\n如果这不是你想查询的玩家，请在末尾添加 #1。"),
        G_Null_BP("无法获取玩家 %s 的 BP...\n如果这不是你想查询的玩家，请在末尾添加 #1。"),
        G_Empty_TodayBP("玩家 %s 今天之内没有新增的 BP 呢...\n尝试扩大搜索天数吧。"),
        G_Empty_PeriodBP("玩家 %s 这段时间之内都没有新增的 BP 呢...\n尝试扩大搜索天数吧。"),

        G_Fetch_PlayerInfo("玩家信息获取失败，请重试。"),
        G_Fetch_List("列表获取失败，请重试。"),

        G_Malfunction_RenderModule("%s 渲染模块失败。"),
        G_Malfunction_Send("%s 发送失败。\n请耐心等待问题修复。"),
        
        ;
        public final String message;
        
        Type(String message) {
            this.message = message;
        }
    }
    

    public GeneralTipsException(Type type) {
        super(type.message);
    }

    public GeneralTipsException(Type type, Object... args) {
        super(type.message, args);
    }

    public GeneralTipsException(byte[] image) {
        super(image);
    }
}
