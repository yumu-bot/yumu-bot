package com.now.nowbot.throwable.ServiceException;

import com.now.nowbot.throwable.TipsException;

public class MapStatisticsException extends TipsException {
    public enum Type {
        M_Instructions("""
                欢迎使用 Yumu Map 功能！食用方法：
                !ymmap / !ymm [BID] (acc%) (comboX) (+mod)
                BID：谱面编号。不输入默认获取玩家最近一次成绩（包括失败），并且以下所有参数均会替换成这次成绩的数据。
                acc：准确率。不输入默认为 100%。
                combo：连击。不输入默认为全连。
                mod：模组。不输入默认为 no mod。"""),//参数_无参数
        M_Me_TokenExpired("您的令牌已过期，请重新授权。(!ymbind)。"),
        M_Me_NotFound("找不到你的玩家信息？"),
        M_Map_NotFound("找不到这张谱面，请检查。"),
        M_Parameter_ComboError("连击参数错误，请检查。"),
        M_Parameter_AccuracyError("准确率参数错误，请检查。"),
        M_Fetch_Error("谱面获取数据失败。"),
        M_Send_Error("谱面渲染图片超时，请重试，或者将问题反馈给开发者。")
        ;//逗号分隔
        public final String message;
        Type(String message) {
            this.message = message;
        }
    }
    public MapStatisticsException(MapStatisticsException.Type type){
        super(type.message);
    }

    public MapStatisticsException(MapStatisticsException.Type type, Object... args){
        super(String.format(type.message, args));
    }
}