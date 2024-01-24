package com.now.nowbot.throwable.ServiceException;

import com.now.nowbot.throwable.TipsException;

public class CustomException extends TipsException {

    public enum Type {
        CUSTOM_Instruction_Deprecated("自定义图片功能 (!ymset) 已经移动至 !ymc。"),
        CUSTOM_Instructions("""
                欢迎使用 Yumu Custom 功能！食用方法：
                !ymcustom / !ymc [type]
                - type：想要自定义的种类。可以输入 banner 或 card。默认 banner。
                
                之后回复你自己发出消息并附带图片即可。推荐尺寸：
                - banner：(1920*320)
                - card：(430*210)
                """),
        CUSTOM_Me_Nobind("未绑定无法使用。(!ymbind)"),
        CUSTOM_Receive_NoPicture("没有接收到图片，或读取图片失败。\n如果一直失败，请重发图片。"),
        CUSTOM_Receive_PictureFetchFailed("下载图片失败, 请稍后尝试。"),
        CUSTOM_Send_Success("设置成功！"),


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
