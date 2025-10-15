package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.dao.BindDao
import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.model.enums.CoverType
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
import com.now.nowbot.util.command.FLAG_PAGE
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.regex.Matcher

@Service("QUALIFIED_MAP") class QualifiedMapService(
    private val beatmapApiService: OsuBeatmapApiService,
    private val userApiService: OsuUserApiService,
    private val imageService: ImageService,
    private val bindDao: BindDao
) : MessageService<QualifiedMapService.QualifiedMapParam> {

    data class QualifiedMapParam(
        val mode: OsuMode,
        val status: String?,
        val genre: Byte?,
        val sort: String,
        val page: Int = 1,
    )

    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: DataValue<QualifiedMapParam>,
    ): Boolean {
        val matcher = Instruction.QUALIFIED_MAP.matcher(messageText)

        if (!matcher.find()) {
            return false
        }

        data.value = getParam(event, matcher)
        return true
    }

    override fun handleMessage(event: MessageEvent, param: QualifiedMapParam): ServiceCallStatistic? {

        val query = mapOf<String, Any>(
            "m" to param.mode.modeValue,
            "s" to (param.status?.takeIf { it != "any" } ?: "qualified"),
            "sort" to param.sort,
            "page" to 1,
        )

        val search: BeatmapsetSearch

        try {
            search = beatmapApiService.parallelSearchBeatmapset(query)

            val maxPerPage: Int = if (search.resultCount <= 24) {
                12
            } else {
                48
            }

            val split = DataUtil.splitPage(search.beatmapsets, param.page, maxPerPage)

            // 后处理
            AsyncMethodExecutor.awaitTripleCallableExecute(
                { beatmapApiService.applyBeatmapsetRankedTime(split.first) },
                { userApiService.applyUserForBeatmapset(split.first) },
                {
                    if (maxPerPage == 12) {
                        // 给完整面板整点头像
                        userApiService.asyncDownloadAvatarFromBeatmapsets(split.first)
                    } else {
                        beatmapApiService.asyncDownloadCoverFromSets(split.first, CoverType.LIST_2X)
                    }
                }
            )

            search.beatmapsets = split.first

            val img = imageService.getPanel(mapOf(
                "search" to search,
                "page" to split.second,
                "max_page" to split.third
            ), "A2")
            event.reply(img)
        } catch (e: Exception) {
            log.error("过审谱面：", e)
            throw IllegalStateException.Send("过审谱面")
        }

        return ServiceCallStatistic.builds(event,
            beatmapsetIDs = search.beatmapsets.map { it.beatmapsetID },
        )
    }

    private fun getParam(event: MessageEvent, matcher: Matcher): QualifiedMapParam {
        // 获取参数
        val statusStr: String? = matcher.group("status")
        val sortStr: String? = matcher.group("sort")
        val genreStr: String? = matcher.group("genre")
        val pageStr: String? = matcher.group(FLAG_PAGE)

        val page = pageStr?.toIntOrNull() ?: 1

        if (page !in 1..999) {
            throw IllegalArgumentException.WrongException.Henan()
        }

        val mode = OsuMode.getMode(OsuMode.getMode(matcher.group(FLAG_MODE)), bindDao.getGroupModeConfig(event))
        val status = DataUtil.getStatus(statusStr)
        val genre = DataUtil.getGenre(genreStr)
        val sort = DataUtil.getSort(sortStr ?: "ranked_asc")

        return QualifiedMapParam(
            mode = mode,
            status = status,
            genre = genre,
            sort = sort,
            page = page
        )
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(QualifiedMapService::class.java)
    }
}
