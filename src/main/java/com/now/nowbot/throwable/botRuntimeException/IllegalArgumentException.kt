package com.now.nowbot.throwable.botRuntimeException

import com.now.nowbot.throwable.TipsRuntimeException

open class IllegalArgumentException(message: String?): TipsRuntimeException(message) {

    open class ExceedException(message: String?): IllegalArgumentException(message) {
        class FilteredScore(count: Int?):
            ExceedException("符合筛选要求的成绩太多了${if (count == null) "" else "($count 个)"}！请缩小查询范围。")

        class StarRating:
            WrongException("对方真的糊了那么高星的图吗？还是说你在滥用功能...")

        class Version:
            ExceedException("符合筛选版本的成绩太多了！请减少版本，缩小查询范围。")

        class VersionDifficulty:
            ExceedException("符合筛选版本的成绩太多了！请指定难度（\":\" + \"b、a、e、m、r\"），缩小查询范围。")

    }

    open class WrongException(message: String?): IllegalArgumentException(message) {

        class Audio:
            WrongException("""
                请输入想要试听的谱面编号！
                (!a <bid> / !a:s <sid>)
            """.trimIndent())

        class Accuracy:
            WrongException("请输入正确的准确率！")

        class BeatmapID:
            WrongException("请输入正确的谱面编号 (BID)！")

        class BeatmapsetID:
            WrongException("请输入正确的谱面集编号 (SID)！")

        class Instruction:
            WrongException("指令错误。")

        class MatchID:
            WrongException("请输入正确的比赛编号 (MID)！")

        class Mode:
            WrongException("请输入正确的游戏模式！")

        class ParseMap(param: Any, position: Any):
                WrongException("""
                    看起来漏了一组谱面呢？
                    这个参数之前缺失谱面：$param，错误位置：$position
                    """.trimIndent()
                )

        class Operator:
            WrongException("请输入正确的逻辑运算符！")

        class OperatorOnly(vararg operator: String):
            WrongException("仅支持使用 ${operator.joinToString(", ")} 逻辑运算符。")

        class PlayerName:
            WrongException("请输入正确的玩家名！")

        class Range:
            WrongException("请输入正确的编号或范围！")

        class Rank:
            WrongException("请输入正确的评级！")

        class StarRating:
            WrongException("请输入正确的星数！")




        class Cabbage:
            WrongException("如果你给他...传递一些完全看不懂的参数...你等于...你也等于...你也有泽任吧？")

        class Henan:
            WrongException("捞翔，恁发嘞是个啥玩应啊？")

    }

    open class Not


}