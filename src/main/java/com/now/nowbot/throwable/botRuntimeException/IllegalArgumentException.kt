package com.now.nowbot.throwable.botRuntimeException

import com.now.nowbot.throwable.TipsRuntimeException

open class IllegalArgumentException(message: String?): TipsRuntimeException(message) {

    open class ExceedException(message: String?): IllegalArgumentException(message) {

        class ExceedFilteredScore:
            ExceedException("符合筛选要求的成绩太多了！请缩小查询范围。")


    }

    open class WrongException(message: String?): IllegalArgumentException(message) {

        class PlayerName:
            WrongException("请输入正确的玩家名！")

        class BeatmapID:
            WrongException("请输入正确的谱面编号 (BID)！")

        class BeatmapsetID:
            WrongException("请输入正确的谱面集编号 (SID)！")

        class MatchID:
            WrongException("请输入正确的比赛编号 (MID)！")

        class Rank:
            WrongException("请输入正确的评级！")

        class Mode:
            WrongException("请输入正确的游戏模式！")

        class Accuracy:
            WrongException("请输入正确的准确率！")

        class Operator:
            WrongException("请输入正确的逻辑运算符！")

        class Instruction:
            WrongException("指令错误。")

        class OperatorOnly(vararg operator: String):
            WrongException("仅支持使用 ${operator.joinToString(", ")} 逻辑运算符。")

        class Cabbage:
            WrongException("如果你给他...传递一些完全看不懂的参数...你等于...你也等于...你也有泽任吧？")

        class Henan:
            WrongException("捞翔，恁发嘞是个啥玩应啊？")

    }

    open class Not


}