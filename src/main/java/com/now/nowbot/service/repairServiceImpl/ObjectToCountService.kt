package com.now.nowbot.service.repairServiceImpl

import com.now.nowbot.aop.CheckPermission
import com.now.nowbot.config.Permission
import com.now.nowbot.entity.BeatmapCountLite
import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.mapper.BeatmapCountMapper
import com.now.nowbot.mapper.BeatmapObjectCountMapper
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.MessageService
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@CheckPermission(isSuperAdmin = true)
class ObjectToCountService(
    private val oldRepo: BeatmapObjectCountMapper,
    private val newRepo: BeatmapCountMapper,
): MessageService<Boolean> {
    private val log = LoggerFactory.getLogger(ObjectToCountService::class.java)

    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: MessageService.DataValue<Boolean>
    ): Boolean {
        /*
        val fix = "!" + "co"

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
        migrateOldToNew()

        return null
    }

    /**
     * 执行数据迁移的主函数
     * @param batchSize 每批次处理的数据量，默认 1000 兼顾速度与内存
     */
    @Transactional
    fun migrateOldToNew(batchSize: Int = 1000) {
        log.info("开始执行数据迁移: 从旧表(osu_beatmap_object_count) -> 新表(beatmap_count)")

        var page = 0
        var totalMigrated = 0

        while (true) {
            // 1. 分批从旧表拉取数据
            val pageRequest = PageRequest.of(page, batchSize)
            val slice = oldRepo.findAllBySlice(pageRequest)
            val oldList = slice.content

            if (oldList.isEmpty()) {
                break // 没数据了，说明迁移完毕
            }

            // 2. 将旧实体批量转换为新实体
            val newEntities = oldList.mapNotNull { oldData ->
                // 边界安全校验：如果旧数据没有 ID 或者时间戳为 null，跳过
                val bid = oldData.bid ?: return@mapNotNull null
                val oldTimestamps = oldData.timestamp ?: intArrayOf()

                // 创建新实体
                val newEntity = BeatmapCountLite(
                    beatmapID = bid,
                    hash = oldData.check,
                    density = oldData.density
                )

                newEntity.writeTimestamps(oldTimestamps)

                newEntity
            }

            // 3. 批量写入新表
            if (newEntities.isNotEmpty()) {
                newRepo.saveAllAndFlush(newEntities)
                totalMigrated += newEntities.size
                log.info("成功迁移第 {} 页，当前已累计转换并迁移 {} 条数据", page + 1, totalMigrated)
            }

            // 4. 判断是否还有下一页
            if (!slice.hasNext()) {
                break
            }
            page++
        }

        log.info("🎉 数据迁移圆满结束！共计成功迁移 {} 条记录。", totalMigrated)
    }
}