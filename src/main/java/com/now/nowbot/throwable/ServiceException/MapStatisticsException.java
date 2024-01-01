package com.now.nowbot.throwable.ServiceException;

import com.now.nowbot.throwable.TipsException;

public class MapStatisticsException extends TipsException {
    public enum Type {
        M_Me_TokenExpired("您的令牌已过期，请重新授权。(!ymbind)。"),
        M_Me_NotFound("找不到你的玩家信息？"),
        M_Map_NotFound("找不到这张谱面，请检查。"),
        M_Parameter_None("欢迎使用 Yumu Map 系统！指令食用方法：\n!ymmap / !ymm [BID] (acc%) (comboX) (+mods)\nBID：谱面编号。\nacc：准确率。不输入默认为 100%。\ncombo：连击。不输入默认为全连。\nMod：模组。不输入默认为 NoMod。\n提示：如果只想输入连击，不想输入准确率，可以输入 !m bid 1 combo，或是 !m bid xcombo。x可帮助区分。"),//参数_无参数
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