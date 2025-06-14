package com.now.nowbot.throwable.botException

import com.now.nowbot.throwable.TipsException

class MapPoolException : TipsException {
    enum class Type(val message: String) {
        GP_Instructions(
            """
                欢迎使用 Yumu GetPool 功能！食用方法：
                !ymgetpool / !ymgp (#name#) [[Mod] (BID)...]
                - name：图池名字。
                - Mod：模组池，必填，可按组多次输入。
                - BID：谱面编号，必填，可按组多次输入。
                
                """.trimIndent()
        ),  //参数_无参数
        GP_Parse_MissingMap("看起来漏了一组谱面呢？\n这个参数之前缺失谱面：%s，错误位置：%s"),  //参数_参数错误
        GP_Map_Empty("TNND，为什么一张谱面都没有？"),  //这场比赛是空的！
        GP_Send_TooManyRequests("休息一下好不好"),  //发送_太频繁
        GP_Send_Error("图片发送失败。") //发送_发送失败
        //逗号分隔
    }

    constructor(type: Type) : super(type.message)

    constructor(type: Type, vararg args: Any?) : super(String.format(type.message, *args))
}
