package com.now.nowbot.throwable.botRuntimeException

import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.throwable.BotException
import com.now.nowbot.throwable.TipsRuntimeException

open class IllegalArgumentException(message: String?): TipsRuntimeException(message), BotException {

    open class ExceedException(message: String?): IllegalArgumentException(message) {
        class RateTooLarge:
            WrongException("我觉得吧，你要是能按这个倍速去打，早就进入天空之城了。")

        class RateTooSmall:
            WrongException("这是什么奇怪的倍速？")

        class StarRating:
            WrongException("可能这个是特殊图？还是说你在滥用功能...")

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

        class Alias(alias: String?):
            WrongException("请输入正确的别名！${alias}")

        class AliasAdjustSingle(title: String, alias: String):
            WrongException("""
                「$title」 -> 「$alias」
                该标题只有一个别名，无法调整位置。
            """.trimIndent())

        class AliasPair:
            WrongException("""
                参数数量不匹配！请按「标题 = 别名」成对输入，多组之间用逗号等分隔。
                例如：!aa 标题1 = 别名1, 标题2 = 别名2
            """.trimIndent())

        class AliasImportFile:
            WrongException("请发送要导入的 JSON 文件，或回复一条包含该文件的消息。")

        class AliasImportNotFound(location: String):
            WrongException("""
                找不到文件：$location
                如果刚用 !fetch a 导出，文件通常保存在运行目录的 debug 文件夹里，可以直接用 !ai aliasDB.json。
            """.trimIndent())

        class AliasImportJson:
            WrongException("""
                JSON 解析失败！请确认文件内容与 !fetch a 导出的格式一致。
                正确格式：{ "标题": "别名" }
            """.trimIndent())

        class BeatmapID:
            WrongException("请输入正确的谱面编号 (BID)！")

        class BeatmapsetID:
            WrongException("请输入正确的谱面集编号 (SID)！")

        class Dimension:
            WrongException("请输入正确的谱面四维 (CS、AR、OD、HP)！")

        class DimensionValue:
            WrongException("请输入正确的谱面四维参数 (CS、AR、OD、HP)！")

        class Instruction(command: String):
            WrongException("没有找到指令 $command。")

        class MatchID:
            WrongException("请输入正确的比赛编号 (MID)！")

        class MaiVersionPlate:
            NoSuchElementException("""
                请输入正确的游戏版本与牌子种类组合！
                如：舞神、堇将、splsss 等。
            """.trimIndent())

        class Mod:
            WrongException("请输入正确的模组！")

        class Mode:
            WrongException("请输入正确的游戏模式！")

        /**
         * 指令里明确指定的模式和筛选条件暗示的模式对不上，且两者之间不能互相转谱。
         */
        class ModeConflict(mode: OsuMode, condition: OsuMode):
            WrongException("""
                模式冲突：你指定了「${mode.fullName}」，但筛选条件只能用于「${condition.fullName}」。
                请统一模式，或去掉其中一方。
            """.trimIndent())

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

        class QQ:
            WrongException("请输入正确的 QQ！")

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