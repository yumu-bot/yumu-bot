package com.now.nowbot.throwable.ServiceException;

import com.now.nowbot.throwable.TipsException;

public class MapStatisticsException extends TipsException {
    public enum Type {
        M_Me_TokenExpired("您的令牌已过期，请重新授权。(!ymbind)。"),
        M_Me_NotFound("找不到你的玩家信息？"),
        M_Map_NotFound("找不到这张谱面，请检查。"),
        M_Parameter_ComboError("连击参数错误，请检查。"),
        M_Parameter_AccuracyError("准确率参数错误，请检查。"),
        M_Fetch_Error("谱面获取数据失败。"),
        M_Send_Error("谱面渲染图片超时，请重试，或者将问题反馈给开发者。")
        ;//逗号分隔
        final String message;
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