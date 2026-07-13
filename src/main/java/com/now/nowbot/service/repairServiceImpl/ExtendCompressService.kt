package com.now.nowbot.service.repairServiceImpl

/*
import com.now.nowbot.aop.CheckPermission
import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.MessageService
import com.now.nowbot.util.IntArrayCompressor
import com.now.nowbot.util.JacksonUtil
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate

@Service
@CheckPermission(isSuperAdmin = true)
class ExtendCompressService(
    private val entityManager: EntityManager,
    private val transactionTemplate: TransactionTemplate
): MessageService<Boolean> {

    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: MessageService.DataValue<Boolean>
    ): Boolean {
        /*
        val fix = "!" + "es"

        if (messageText.contains(fix) && Permission.isSuperAdmin(event.sender.contactID)) {
            data.value = true
            return true
        }

         */

        return false
    }

    override fun handleMessage(
        event: MessageEvent,
        param: Boolean
    ): ServiceCallStatistic? {
        try {
            // 使用 TransactionTemplate 执行
            transactionTemplate.execute { _ ->
                migrateJsonbToBytea()
                null
            }
            event.replyAsync("转换完成")
        } catch (e: Exception) {
            event.replyAsync("转换失败: ${e.message}")
            throw e
        }
        return null
    }

    private val objectMapper = JacksonUtil.mapper

    fun migrateJsonbToBytea() {
        // 1. 添加新列
        entityManager.createNativeQuery("""
            ALTER TABLE osu_extend_beatmap 
            ADD COLUMN IF NOT EXISTS fails BYTEA,
            ADD COLUMN IF NOT EXISTS exits BYTEA
        """).executeUpdate()

        // 2. 查询所有需要迁移的数据
        val results = entityManager.createNativeQuery("""
            SELECT beatmap_id, fail_times 
            FROM osu_extend_beatmap 
            WHERE fail_times IS NOT NULL
        """).resultList

        // 3. 逐条转换并更新
        results.forEach { row ->
            val rowArray = row as Array<*>
            val beatmapId = rowArray[0] as Long
            val jsonStr = rowArray[1] as String

            val jsonNode = objectMapper.readTree(jsonStr)

            val failArray = jsonNode.get("fail")?.let {
                IntArray(it.size()) { i -> it[i].asInt() }
            }
            val exitArray = jsonNode.get("exit")?.let {
                IntArray(it.size()) { i -> it[i].asInt() }
            }

            val failBytes = failArray?.let { IntArrayCompressor.intArrayToByteArray(it) }
            val exitBytes = exitArray?.let { IntArrayCompressor.intArrayToByteArray(it) }

            entityManager.createNativeQuery("""
                UPDATE osu_extend_beatmap 
                SET fails = ?1, exits = ?2 
                WHERE beatmap_id = ?3
            """)
                .setParameter(1, failBytes)
                .setParameter(2, exitBytes)
                .setParameter(3, beatmapId)
                .executeUpdate()
        }
    }
}

 */