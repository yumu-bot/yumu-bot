package com.now.nowbot.throwable.botRuntimeException

import com.now.nowbot.model.match.Match
import com.now.nowbot.model.match.MatchListener
import com.now.nowbot.throwable.BotException
import com.now.nowbot.throwable.TipsRuntimeException

open class MatchException(message: String) : TipsRuntimeException(message), BotException {
    open class NormalOperate(message: String) : MatchException(message) {
        class Instructions: NormalOperate("""
            欢迎使用 Yumu 比赛监听功能！食用方法：
            !ymml [matchid] (operate)
            matchid：这场比赛的房间号。
            operate：操作。
              可输入 start，s，stop，p。
              默认开始监听（start）。
        """.trimIndent()
        )

        class Start(match: Match): NormalOperate("""
            开始监听比赛 ${match.statistics.matchID}：
            当前状态：${match.getCurrentDetails()}
        """.trimIndent())

        class Continue(matchID: Any, counts: Any): NormalOperate("""
            比赛 $matchID 已监听 $counts 轮。
            想要继续，请在 60s 内回复 OK。
        """.trimIndent())

        class Stop(matchID: Any, stopType: MatchListener.StopType): NormalOperate("""
            停止监听 ${matchID}：${stopType.tips}。
        """.trimIndent())

        class StopGroup(matchIDs: Collection<Any>): NormalOperate(
            StringBuilder("""
                已完全停止当前群聊中所有对局监听实例！
                以下是被停止的对局 ID：
                
                """.trimIndent()
            ).append(matchIDs.collectionToString()).toString()
        )

        class StopAll(matchIDs: Collection<Any>) : NormalOperate(
            StringBuilder("""
                已完全停止所有对局监听实例！
                以下是被停止的对局 ID：
                
                """.trimIndent()
            ).append(matchIDs.collectionToString()).toString()
        )
    }

    open class NoListener:
        MatchException("本群没有监听实例。")

    open class ListListener(matchIDs: Collection<Any>):
        MatchException("本群的监听对局有：\n${matchIDs.collectionToString()}")

    open class MatchAborted(matchID: Any, beatmapID: Any):
        MatchException("比赛 $matchID 的上一场对局 (${beatmapID}) 被强制结束了。")

    open class MatchAlreadyEnd(matchID: Any):
        MatchException("比赛 $matchID 早就结束了...")

    open class MatchAlreadyListen(matchID: Any):
        MatchException("比赛 $matchID 已经在监听中...")

    open class MaxListenerInGroup:
        MatchException("这个群的监听实例已到达最大数量。")

    open class MaxListenerByUser:
        MatchException("您的监听实例已到达最大数量。")


    companion object {
        private fun Collection<Any>.collectionToString(): String {
            val ellipsis = if (this.size > 10) {
                "..."
            } else ""

            return this.take(10).joinToString(", ").plus(ellipsis)
        }
    }
}