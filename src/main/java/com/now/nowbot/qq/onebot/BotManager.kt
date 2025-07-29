package com.now.nowbot.qq.onebot

import com.mikuac.shiro.core.Bot
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component
import org.springframework.web.socket.WebSocketSession
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.PriorityBlockingQueue

@Component
@Primary
class BotManager : com.mikuac.shiro.core.CoreEvent() {

    override fun online(bot: Bot) {
        botOnline(bot)
    }

    override fun offline(account: Long) {
        botOffline(account)
    }

    override fun session(session: WebSocketSession?): Boolean = true

    companion object {
        private val log = LoggerFactory.getLogger("BotManager")
        private val BotMap = mutableMapOf<Long, Bot>()

        // 查找状态缓存
        private val BotStatusMap = ConcurrentHashMap<Long, BotStatus>()
        private val GroupSortedBots = ConcurrentHashMap<Long, PriorityBlockingQueue<BotStatus>>()
        private val GroupSortedBotsSet = ConcurrentHashMap<Long, MutableSet<Long>>()

        @JvmStatic
        fun onMessage(botId: Long, groupId: Long) {
            val set = GroupSortedBotsSet.computeIfAbsent(groupId) { mutableSetOf() }
            if (set.contains(botId)) {
                return
            }
            set.add(botId)
            GroupSortedBots.computeIfAbsent(groupId) { PriorityBlockingQueue() }
                .add(BotStatus(botId = botId))
        }

        fun botOnline(bot: Bot) {
            val id = bot.selfId

            BotMap[id] = bot
            BotStatusMap[id] = BotStatus(botId = id)
        }

        fun botOffline(id: Long) {
            BotMap.remove(id)
            BotStatusMap.remove(id)

            GroupSortedBotsSet.forEach { (groupId, set) ->
                if (!set.remove(id)) return@forEach
                val bots = GroupSortedBots[groupId] ?: return@forEach
                bots.removeIf { it.botId == id }
                if (bots.isEmpty()) {
                    GroupSortedBots.remove(groupId)
                }
            }
            log.info("Bot $id offline")
        }

        fun getBot(id: Long): Bot? {
            return BotMap[id]
        }

        fun getBestBot(groupId: Long): Bot? {
            val status = GroupSortedBots[groupId]?.firstOrNull() ?: return null
            return BotMap[status.botId]
        }

        fun updateStatus(botId: Long, isSuccess: Boolean) {
            val status = BotStatusMap.getOrPut(botId) { BotStatus(botId = botId) }
            if (isSuccess) {
                status.weight += 3
                if (status.failCount != 0) status.failCount = 0
            } else {
                status.failCount++
                status.weight -= 5 * status.failCount
            }

            for ((_, bots) in GroupSortedBots) {
                bots.removeIf { it.botId == botId }
                bots.add(status)
            }
        }
    }
}

data class BotStatus(
    var botId: Long = 0,
    var weight: Int = 100,
    var failCount: Int = 0,
) : Comparable<BotStatus> {

    override fun compareTo(other: BotStatus) = weight - other.weight
    override fun hashCode() = botId.hashCode()
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BotStatus) return false

        if (botId != other.botId) return false

        return true
    }
}