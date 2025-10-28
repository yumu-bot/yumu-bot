package com.now.nowbot.throwable.botRuntimeException

import com.now.nowbot.throwable.BotException
import com.now.nowbot.throwable.TipsRuntimeException

open class IllegalArgumentException(message: String?): TipsRuntimeException(message), BotException {

    open class ExceedException(message: String?): IllegalArgumentException(message) {
        class RateTooLarge:
            WrongException("我觉得吧，你要是能按这个倍速去打，早就进入天空之城了。")

        class RateTooSmall:
            WrongException("这是什么奇怪的倍速？")

        class StarRating:
            WrongException("对方真的糊了那么高星的图吗？还是说你在滥用功能...")

        /*
        class FilteredScore(count: Int?):
            ExceedException("符合筛选要求的成绩太多了${if (count == null) "" else "($count 个)"}！请缩小查询范围。")

        class Version:
            ExceedException("符合筛选版本的成绩太多了！请减少版本，缩小查询范围。")

        class VersionDifficulty:
            ExceedException("符合筛选版本的成绩太多了！请指定难度（\":\" + \"b、a、e、m、r\"），缩小查询范围。")

        class GroupMembers:
            WrongException("""
                群聊人数太多！无法获取数据。
                这是一个 bug，将来修复。
            """.trimIndent())
         */

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

        class Instruction(command: String):
            WrongException("没有找到指令 $command。")

        class MatchID:
            WrongException("请输入正确的比赛编号 (MID)！")

        class Mod:
            WrongException("请输入正确的模组！")

        class Mode:
            WrongException("请输入正确的游戏模式！")

        class Operator:
            WrongException("请输入正确的逻辑运算符！")

        class OperatorOnly(vararg operator: String):
            WrongException("仅支持使用 ${operator.joinToString(", ")} 逻辑运算符。")

        class ParseMap(param: Any, position: Any):
            WrongException("""
                    看起来漏了一组谱面呢？
                    这个参数之前缺失谱面：$param，错误位置：$position
                    """.trimIndent()
            )

        class PlayerName:
            WrongException("请输入正确的玩家名！")

        class PlayerID:
            WrongException("请输入正确的玩家编号！")

        class Quotation:
            WrongException("请使用成对的引号！")

        class Range:
            WrongException("请输入正确的编号或范围！")

        class Rank:
            WrongException("请输入正确的评级！")

        class StarRating:
            WrongException("请输入正确的星数！")

        class TeamID:
            WrongException("请输入正确的战队编号！")




        class Cabbage:
            WrongException("如果你给他...传递一些完全看不懂的参数...你等于...你也等于...你也有泽任吧？")

        class Henan:
            WrongException("捞翔，恁发嘞是个啥玩应啊？")

    }


}