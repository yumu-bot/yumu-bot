package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.dao.BindDao
import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.osu.BeatmapsetSearch
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.service.osuApiService.OsuUserApiService

import com.now.nowbot.throwable.botRuntimeException.IllegalArgumentException
import com.now.nowbot.throwable.botRuntimeException.IllegalStateException
import com.now.nowbot.util.AsyncMethodExecutor
import com.now.nowbot.util.DataUtil
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.command.FLAG_MODE
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.regex.Matcher
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt

@Service("QUALIFIED_MAP") class QualifiedMapService(
    private val beatmapApiService: OsuBeatmapApiService,
    private val userApiService: OsuUserApiService,
    private val imageService: ImageService,
    private val bindDao: BindDao
) : MessageService<Matcher> {

    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: DataValue<Matcher>,
    ): Boolean {
        val m = Instruction.QUALIFIED_MAP.matcher(messageText)
        if (m.find()) {
            data.value = m
            return true
        } else return false
    }

    @Throws(Throwable::class) override fun handleMessage(event: MessageEvent, param: Matcher): ServiceCallStatistic? { // 获取参数
        val statusStr = param.group("status") ?: "q"
        val sortStr = param.group("sort") ?: "ranked_asc"
        val rangeStr = param.group("range") ?: "12"

        val mode = OsuMode.getMode(OsuMode.getMode(param.group(FLAG_MODE)), null, bindDao.getGroupModeConfig(event))

        val range = rangeStr.toIntOrNull() ?: throw IllegalArgumentException.WrongException.Henan()

        if (range !in 1..999) {
            throw IllegalArgumentException.WrongException.Range()
        }

        val tries = max(floor(range / 50.0).roundToInt() + 1, 10)

        val status = DataUtil.getStatus(statusStr)
        val sort = DataUtil.getSort(sortStr)

        val query = mapOf<String, Any>(
            "m" to mode.modeValue,
            "s" to if (status == "any" || status == null) "qualified" else status,
            "sort" to sort,
            "page" to 1,
        )

        val search: BeatmapsetSearch

        try {
            search = beatmapApiService.searchBeatmapset(query, tries)

            AsyncMethodExecutor.awaitPairCallableExecute(
                { beatmapApiService.applyBeatmapsetRankedTime(search.beatmapsets) },
                { userApiService.applyUserForBeatmapset(search.beatmapsets) }
            )

            // 给完整面板整点头像
            if (search.resultCount <= 12) {
                AsyncMethodExecutor.asyncRunnableExecute {
                    userApiService.asyncDownloadAvatarFromBeatmapsets(search.beatmapsets)
                }
            }

            val img = imageService.getPanel(search, "A2")
            event.reply(img)
        } catch (e: Exception) {
            log.error("过审谱面：", e)
            throw IllegalStateException.Send("过审谱面")
        }

        return ServiceCallStatistic.builds(event,
            beatmapsetIDs = search.beatmapsets.map { it.beatmapsetID },
        )
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(QualifiedMapService::class.java)
    }
}
