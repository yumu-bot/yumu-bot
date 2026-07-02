package com.now.nowbot.cache

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.stats.CacheStats
import com.mikuac.shiro.core.BotContainer
import com.mikuac.shiro.dto.event.message.GroupMessageEvent
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.TimeUnit

@Component
class QQMessageCacheProvider(
    private val botContainer: BotContainer
) {
    data class MessageKey(
        val messageID: Long,
        val groupID: Long,
        val senderID: Long
    )

    private val messageCache: Cache<MessageKey, GroupMessageEvent> = Caffeine.newBuilder()
        .expireAfterWrite(2, TimeUnit.MINUTES)
        .recordStats() // 为了性能，请注释
        .evictionListener<MessageKey, GroupMessageEvent> { key, _, cause ->
            log.debug("普通消息 Key: {}, 原因: {}", key, cause.name)
            key?.let { removeIndex(it) }
        }
        .build()

    private val botSelfCache: Cache<MessageKey, GroupMessageEvent> = Caffeine.newBuilder()
        .expireAfterWrite(24, TimeUnit.HOURS)
        .recordStats() // 为了性能，请注释
        .evictionListener<MessageKey, GroupMessageEvent> { key, _, cause ->
            log.debug("机器消息 Key: {}, 原因: {}", key, cause.name)
            key?.let { removeIndex(it) }
        }
        .build()

    private val groupIndex = ConcurrentHashMap<Long, MutableSet<MessageKey>>()

    /**
     * 写入普通消息（原方法，内部默认 senderID 传入 event 里的发送者）
     */
    fun putMessage(message: GroupMessageEvent) {
        val messageID = message.messageId?.toLong() ?: 0L
        val senderID = message.sender?.userId ?: 0L
        val groupID = message.groupId ?: 0L

        val key = MessageKey(messageID, groupID, senderID)

        log.debug("g: {} m: {} s: {}", groupID, messageID, senderID)

        if (senderID in botContainer.robots.keys) {
            botSelfCache.put(key, message)
        } else {
            messageCache.put(key, message)
        }

        groupIndex.computeIfAbsent(groupID) { ConcurrentHashMap.newKeySet() }.add(key)
    }

    /**
     * 高效查询：捞取某个群【所有缓存】（普通消息 + 机器人自身消息），按 ID 正序
     */
    fun getMessagesByGroup(groupID: Long): List<GroupMessageEvent> {
        val keys = groupIndex[groupID] ?: return emptyList()

        val normalMsgs = messageCache.getAllPresent(keys).values
        val botSelfMsgs = botSelfCache.getAllPresent(keys).values

        // 合并并根据消息 ID 正序排列
        return (normalMsgs + botSelfMsgs).sortedBy { it.messageId }
    }

    /**
     * 新增：仅仅捞取某个群内【机器人自己】的数据
     */
    fun getBotMessagesByGroup(groupID: Long): List<GroupMessageEvent> {
        val keys = groupIndex[groupID] ?: return emptyList()

        return botSelfCache.getAllPresent(keys).values
            .sortedBy { it.messageId }
    }

    /**
     * 精准查询（先查普通，再查自身）
     */
    fun getMessage(groupID: Long, messageID: Long, senderID: Long): GroupMessageEvent? {
        val key = MessageKey(messageID, groupID, senderID)
        return messageCache.getIfPresent(key) ?: botSelfCache.getIfPresent(key)
    }

    fun getStats(): CacheStats {
        return messageCache.stats()
    }

    /**
     * 内部方法：清理索引
     */
    private fun removeIndex(key: MessageKey) {
        val keys = groupIndex[key.groupID]
        if (keys != null) {
            keys.remove(key)
            if (keys.isEmpty()) {
                groupIndex.remove(key.groupID)
            }
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(QQMessageCacheProvider::class.java)
    }
}
