package com.now.nowbot.throwable.botRuntimeException

import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.throwable.BotException
import com.now.nowbot.throwable.TipsRuntimeException

open class NoSuchElementException(message: String?): TipsRuntimeException(message), BotException {

    class Audio:
        NoSuchElementException("""
            试听音频下载失败。
            也许是谱面输入错误，或者谱面被版权限制了。
            自己去官网听算了。
            """.trimIndent())

    class Avatar:
        NoSuchElementException("没有找到玩家的头像。")

    class Beatmap(name: Any):
        NoSuchElementException("没有找到谱面 $name。")

    class BeatmapCache(name: Any):
        NoSuchElementException("没有找到谱面 $name 的缓存文件。")

    class BeatmapScore(name: Any):
        NoSuchElementException("没有找到您在谱面 $name 内的成绩。")

    class BeatmapRecentScore(name: Any):
        NoSuchElementException("没有找到您在谱面 $name 最近 100 个成绩之内的成绩。")

    class BeatmapScoreFiltered(name: Any):
        NoSuchElementException("没有找到您在谱面 $name 内符合条件的成绩。")

    class BestScore(name: String):
        NoSuchElementException("无法获取玩家 $name 的最好成绩...")

    class BestScoreWithMode(name: String, mode: OsuMode):
        NoSuchElementException("无法获取玩家 $name 在 ${mode.fullName} 内的最好成绩...")

    class BestScoreFiltered(name: Any):
        NoSuchElementException("无法获取玩家 $name 符合条件的最好成绩...")

    class BestScoreTheoretical:
        NoSuchElementException("您（选中）的最好成绩已经全是理论值了！")

    class Data:
        NoSuchElementException("数据是空的！")

    class GuestDiff:
        NoSuchElementException("你没有做过客串难度谱面，别人也没有给你赠送过客串难度谱面。")

    class Leaderboard:
        NoSuchElementException("这张谱面没有在线排行榜。")

    class LeaderboardScore:
        NoSuchElementException("这张谱面的在线排行榜内没有成绩。")

    class MatchRound:
        NoSuchElementException("没有找到这场对局。")

    class PassedScore(name: String, mode: OsuMode):
        NoSuchElementException("没有找到玩家 $name 在 ${mode.fullName} 模式内的最近通过成绩。")

    class PassedScoreFiltered(name: String, mode: OsuMode):
        NoSuchElementException("无法获取玩家 $name 在 ${mode.fullName} 模式内符合条件的最近通过成绩...")

    class PeriodBestScore(name: String):
        NoSuchElementException("""
            玩家 $name 这段时间之内没有新增的 BP 呢...
            尝试修改范围，或尝试扩大搜索天数吧。
        """.trimIndent())

    class Player(name: String? = null):
        NoSuchElementException("没有找到玩家${
            if (name.isNullOrBlank()) { 
                "" 
            } else { 
                " $name" 
            }}。")

    class PlayerBadge(name: String? = null):
        NoSuchElementException("没有找到玩家${
            if (name.isNullOrBlank()) {
                ""
            } else {
                " $name "
            }}的主页奖牌。")

    class PlayerBestScore(name: String, mode: OsuMode):
        NoSuchElementException("""
            玩家 $name 在 ${mode.fullName} 模式上的最好成绩数量不够呢...
            灼热分析 EX
            """.trimIndent())

    class PlayerInactive(name: String):
        NoSuchElementException("玩家 $name 最近不活跃...")

    class PlayerPlay(name: String):
        NoSuchElementException("玩家 $name 的游戏时长太短了，快去多玩几局吧！")

    class PlayerPlayWithMode(name: String, mode: OsuMode):
        NoSuchElementException("玩家 $name 在 ${mode.fullName} 模式上的游戏时长太短了，快去多玩几局吧！")

    class PlayerTeam(name: String):
        NoSuchElementException("没有找到玩家 $name 所属的战队。")

    class PlayerWithRange(name: String):
        NoSuchElementException(
            """
            没有找到玩家 $name。
            你可能把范围和玩家名输反了。
            """.trimIndent())

    class PlayerWithBeatmapID(name: String):
        NoSuchElementException(
            """
            没有找到玩家 $name。
            你可能把谱面号和玩家名输反了。
            """.trimIndent())

    class RecentScore(name: String, mode: OsuMode):
        NoSuchElementException("没有找到玩家 $name 在 ${mode.fullName} 模式内的最近成绩。")

    class RecentScoreFiltered(name: String, mode: OsuMode):
        NoSuchElementException("无法获取玩家 $name 在 ${mode.fullName} 模式内符合条件的最近成绩...")

    class Result:
        NoSuchElementException("没有找到结果！")

    class ResultNotAccurate:
        NoSuchElementException("""
            没有找到可能的结果...
            试试输入更常见的外号或准确的歌曲编号吧。
        """.trimIndent())

    class Song(name: Any):
        NoSuchElementException("没有找到歌曲 $name。")

    class Team(name: String):
        NoSuchElementException("没有找到战队 $name。")

    class TeamID(id: Number):
        NoSuchElementException("没有找到编号为 $id 的战队。")

    class TodayBestScore(name: String):
        NoSuchElementException("""
            玩家 $name 今天之内没有新增的 BP 呢...
            尝试修改范围，或尝试扩大搜索天数吧。
        """.trimIndent())

    class UnrankedBeatmapScore(name: String):
        NoSuchElementException("谱面 $name 没有榜，无法获取成绩。")

    class VersionScore(name: String):
        NoSuchElementException("没有找到您在版本 $name 内的成绩。")



}