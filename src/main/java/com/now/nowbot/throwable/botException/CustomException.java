package com.now.nowbot.throwable.botException;

import com.now.nowbot.throwable.TipsException;

public class CustomException extends TipsException {

    public enum Type {
        CUSTOM_Instruction_Deprecated("自定义图片功能 (!ymset) 已经移动至 !ymc。"),
        CUSTOM_Instructions("""
                欢迎使用 Yumu Custom 功能！食用方法：
                !ymcustom / !ymc (:save) (type)
                - save：保存或删除。可以输入 save (add) 或 clear (delete)。不输入默认保存。
                  - 如果想要删除自定义图片，可无需发送回复类型的消息。
                - type：想要自定义的种类。可以输入 banner 或 card。不输入默认 banner。
                
                回复任意一张图片消息即可执行操作。推荐尺寸：
                - banner：(1920*320)
                - card：(430*210)
                """),
        CUSTOM_Me_TokenExpired("您的令牌已过期，请重新授权。(!ymbind)"),
        CUSTOM_Question_Clear("没有接收到图片，或读取图片失败。\n是想要清除你的自定义图片吗？回复 OK 确认。如果并不想，请无视。\n如果您想设置自定义图片，请使用指令回复一张图片。"),
        CUSTOM_Receive_NoBot("连接失败。请一会儿重试。"),
        CUSTOM_Receive_NoPicture("没有接收到图片，或读取图片失败。\n如果一直失败，请重新发送图片，并回复它。"),
        CUSTOM_Receive_PictureFetchFailed("下载图片失败, 请稍后再尝试。"),
        CUSTOM_Set_Failed("设置 %s 失败。\n未知错误已上报。"),
        CUSTOM_Set_Success("设置 %s 成功！"),
        CUSTOM_Clear_NoSuchFile("清除 %s 失败。\n数据库里并没有保存你的自定义图片呢。"),
        CUSTOM_Clear_Failed("清除 %s 失败。\n未知错误已上报。"),
        CUSTOM_Clear_Success("清除 %s 成功！"),

        ;//逗号分隔
        public final String message;
        Type (String message) {
            this.message = message;
        }
    }
    public CustomException(CustomException.Type type){
        super(type.message);
    }
    public CustomException(CustomException.Type type, Object... args){
        super(String.format(type.message, args));
    }

}
