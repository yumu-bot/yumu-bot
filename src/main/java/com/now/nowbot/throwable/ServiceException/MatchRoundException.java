package com.now.nowbot.throwable.ServiceException;

import com.now.nowbot.throwable.TipsException;

public class MatchRoundException extends TipsException {
    public enum Type {
        MR_Parameter_None("欢迎使用 Yumu Match Round 系统！指令食用方法：\n!ymmatchround / !ymro [matchid] (rounds) (keyword)\nmatchid：这场比赛的房间号。\nkeyword：需要查询的关键字，可以查询谱面艺术家、曲名、难度名。"),//参数_无参数

        MR_MatchID_RangeError("输入的房间号范围错误！"),//参数_参数错误
        MR_MatchID_NotFound("小沐找不到这场比赛..."),//参数_参数错误

        MR_Round_Empty("房间内没有对局呢..."),//参数_比赛未找到
        MR_Round_RangeError("输入的对局参数范围错误！"),//参数_参数错误
        MR_Round_NotFound("小沐找不到这场对局..."),//参数_参数错误

        MR_KeyWord_NotFound("小沐找不到含有此关键字的对局..."),//参数_参数错误
        MR_Fetch_Error("对局信息图片获取失败。\n请耐心等待问题修复。"), //发送_获取失败
        MR_Send_Error("对局信息发送失败。\n请耐心等待问题修复。") //发送_发送失败
        ;//逗号分隔
        public final String message;
        Type(String message) {
            this.message = message;
        }
    }
    public MatchRoundException(MatchRoundException.Type type){
        super(type.message);
    }
}