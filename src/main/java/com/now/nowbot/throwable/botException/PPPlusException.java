package com.now.nowbot.throwable.botException;

import com.now.nowbot.throwable.TipsException;

public class PPPlusException extends TipsException {
    public enum Type {
        PL_Player_VSNotFound("哩个瓜娃子到底要 VS 哪个哦！"),
        PL_Function_NotSupported("抱歉，本功能暂不支持除 Standard 模式以外的谱面！"),
        PL_User_NotFound("找不到玩家。"),
        PL_User_Banned("玩家被 Ban 了。"),
        PL_API_NotAccessible("ppy API 无法访问。"),
        PL_Map_BIDParseError("请输入正确的 BID！"),
        PL_Map_NotFound("找不到谱面，或是 ppy API 无法访问。"),
        PL_Fetch_APIConnectFailed("PP+ 计算失败, 正在火速解决中!"),
        PL_Render_Error("PP+ 渲染失败，请重试，或耐心等待问题修复。"),
        PL_Send_Error("PP+ 发送失败，请重试。"),
        ;
        public final String message;
        Type(String message) {
            this.message = message;
        }
        }
    public PPPlusException(PPPlusException.Type type){
        super(type.message);
    }
}