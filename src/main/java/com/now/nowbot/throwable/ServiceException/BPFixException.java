package com.now.nowbot.throwable.ServiceException;

import com.now.nowbot.throwable.TipsException;

public class BPFixException extends TipsException {
    public enum Type {
        BF_BP_Empty("没有最好成绩！"),
        BF_Fix_Empty("您（选中）的最好成绩已经全是理论值了！"),
        BF_Map_RankError ("请输入正确的 BP 编号或范围！"),
        BF_Exchange_TooMany("无法获取理论最好成绩：超时（太多了），\n如果你是第一次见到这条消息，第二次通常就会恢复了。\n如果你多次见到这条消息，可以尝试缩小范围。"),
        BF_Render_ConnectFailed("无法获取理论最好成绩：无法连接到绘图服务器！"),
        BF_Render_Error("理论最好成绩渲染失败。请耐心等待问题修复，或稍后重试。"),
        BF_Send_Error("理论最好成绩发送失败。请耐心等待问题修复，或稍后重试。")
        ;
        public final String message;
        Type(String message) {
            this.message = message;
        }
    }
    public BPFixException(BPFixException.Type type){
        super(type.message);
    }
    public BPFixException(BPFixException.Type type, Object... args){
        super(String.format(type.message, args));
    }
}
