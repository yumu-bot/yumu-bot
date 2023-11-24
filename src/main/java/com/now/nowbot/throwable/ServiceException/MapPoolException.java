package com.now.nowbot.throwable.ServiceException;

import com.now.nowbot.throwable.TipsException;

public class MapPoolException extends TipsException {
    public enum Type {
        PO_Parameter_None("欢迎使用 Yumu Mappool 系统！指令食用方法：\n!ympool / !ympo (#name#) [[Mod] (BID)...]\nname：图池名字。\nMod：模组池，可按组多次输入。\nBID：谱面编号，可按组多次输入。"),//参数_无参数
        PO_Parse_MissingMap("看起来漏了一组谱面呢？\n这个参数之前缺失谱面：%s，错误位置：%s"),//参数_参数错误
        PO_Map_Empty("TNND，为什么一张谱面都没有？"),//这场比赛是空的！
        PO_Send_TooManyRequests("休息一下好不好"), //发送_太频繁
        PO_Send_Error("图片发送失败。") //发送_发送失败

        ;//逗号分隔
        final String message;
        Type(String message) {
            this.message = message;
        }
    }
    
    public MapPoolException(MapPoolException.Type type){
        super(type.message);
    }

    public MapPoolException(MapPoolException.Type type, Object... args){
        super(String.format(type.message, args));
    }
}
