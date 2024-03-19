package com.now.nowbot.throwable.ServiceException;

import com.now.nowbot.throwable.TipsException;

public class CsvInfoException extends TipsException {
    public enum Type {
        CI_Instructions("""
        欢迎使用 Yumu Csv Info 功能！食用方法：
        !ymcsvinfo / !ci (:mode) [users]
        mode：游戏模式。
        users：多个用分隔符隔开的玩家名称。
        """),
        CI_Player_FetchFailed("获取玩家 %s 失败。"),

        CI_Fetch_TooManyUser("一次性输入的玩家太多！获取信息的时候可能会遇到 API 瓶颈。"),
        CI_Fetch_Exceed("一次性输入的玩家太多！请缩减到 200 个以下！"),
        CI_Fetch_TooManyRequest("API 调用达到上限。请稍后重试。\n当前已经查询到：%s"),
        CI_Fetch_ReachThreshold("遇到 API 瓶颈！等待 10 秒后再次尝试获取！"),
        CI_Fetch_SleepingInterrupted("触发休眠时异常中断！请重试！"),

        CI_Send_Failed("玩家信息表输出失败，请重试。"),
        CI_Send_NotGroup("请在群聊中使用这个功能！"),
        ;
        public final String message;
        Type(String message) {
            this.message = message;
        }
    }
    public CsvInfoException(CsvInfoException.Type type, Object... args){
        super(String.format(type.message, args));
    }
    public CsvInfoException(CsvInfoException.Type type){
        super(type.message);
    }
}