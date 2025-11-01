package com.now.nowbot.service.messageServiceImpl

import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
import com.now.nowbot.dao.BindDao
import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.filter.BeatmapsetFilter
import com.now.nowbot.model.filter.MostPlayedBeatmapFilter
import com.now.nowbot.model.filter.SearchBeatmapsetFilter
import com.now.nowbot.model.osu.Beatmap
import com.now.nowbot.model.osu.Beatmapset
import com.now.nowbot.model.osu.BeatmapsetSearch
import com.now.nowbot.model.osu.OsuUser
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.qq.tencent.TencentMessageService
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.service.osuApiService.OsuUserApiService
import com.now.nowbot.throwable.botRuntimeException.IllegalStateException
import com.now.nowbot.throwable.botRuntimeException.NoSuchElementException
import com.now.nowbot.util.InstructionObject
import com.now.nowbot.util.InstructionUtil
import com.now.nowbot.util.DataUtil
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.OfficialInstruction
import com.now.nowbot.util.command.FLAG_ANY
import com.now.nowbot.util.command.FLAG_PAGE
import com.now.nowbot.util.command.FLAG_TYPE
import com.now.nowbot.util.command.REG_RANGE
import org.springframework.stereotype.Service
import java.util.regex.Matcher

@Service("EXPLORE")
class ExploreService(
    private val beatmapApiService: OsuBeatmapApiService,
    private val userApiService: OsuUserApiService,
    private val imageService: ImageService,
    private val bindDao: BindDao,
): MessageService<ExploreService.ExploreParam>, TencentMessageService<ExploreService.ExploreParam> {

    abstract class ExploreParam

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
    data class SearchParam(
        val search: BeatmapsetSearch,
        val page: Int = 1,
        val maxPage: Int = 1,
    ): ExploreParam() {
        fun toMap(): Map<String, Any> {
            return mapOf(
                "search" to search,
                "page" to page,
                "max_page" to maxPage,
            )
        }
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
    data class MostPlayedParam(
        val user: OsuUser,
        val mostPlayed: List<Beatmap>,
        val page: Int = 1,
        val maxPage: Int = 1,
    ): ExploreParam() {
        fun toMap(): Map<String, Any> {
            return mapOf(
                "user" to user,
                "most_played" to mostPlayed,
                "page" to page,
                "max_page" to maxPage,
            )
        }
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
    data class MyBeatmapsetParam(
        val user: OsuUser,
        val beatmapsets: List<Beatmapset>,
        val page: Int = 1,
        val maxPage: Int = 1,
    ): ExploreParam() {
        fun toMap(): Map<String, Any> {
            return mapOf(
                "user" to user,
                "beatmapsets" to beatmapsets,
                "page" to page,
                "max_page" to maxPage,
            )
        }
    }

    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: MessageService.DataValue<ExploreParam>
    ): Boolean {
        val matcher1 = Instruction.EXPLORE.matcher(messageText)
        val matcher2 = Instruction.EXPLORE_MOST_PLAYED.matcher(messageText)

        if (matcher1.find()) {
            data.value = getParam(event, matcher1)
            return true
        } else if (matcher2.find()) {
            data.value = getParam(event, matcher2, preselect = true)
            return true
        }

        return false
    }

    override fun handleMessage(
        event: MessageEvent,
        param: ExploreParam
    ): ServiceCallStatistic? {
        val image = param.getImage()

        event.reply(image)

        return ServiceCallStatistic.building(event) {
            setParam(
                mapOf(
                    "count" to when(param) {
                        is MostPlayedParam -> param.mostPlayed.size
                        is MyBeatmapsetParam -> param.beatmapsets.size
                        is SearchParam -> param.search.resultCount
                        else -> -1
                    }
                )
            )
        }
    }

    override fun accept(
        event: MessageEvent,
        messageText: String
    ): ExploreParam? {
        val matcher1 = OfficialInstruction.EXPLORE.matcher(messageText)
        val matcher2 = OfficialInstruction.EXPLORE_MOST_PLAYED.matcher(messageText)

        if (matcher1.find()) {
            return getParam(event, matcher1)
        } else if (matcher2.find()) {
            return getParam(event, matcher2, preselect = true)
        }

        return null
    }

    override fun reply(
        event: MessageEvent,
        param: ExploreParam
    ): MessageChain? {
        val image = param.getImage()

        return MessageChain(image)
    }

    private fun getParam(event: MessageEvent, matcher: Matcher, preselect: Boolean = false): ExploreParam {
        val type = if (preselect) {
            BeatmapType.MOST_PLAYED
        } else {
            BeatmapType.getType(matcher.group(FLAG_TYPE))
        }
        val page = matcher.group(FLAG_PAGE)?.toIntOrNull() ?: 1
        val any = matcher.group(FLAG_ANY)?.trim() ?: ""

        val user = InstructionUtil.getUserWithoutRange(event, matcher, InstructionObject(bindDao.getGroupModeConfig(event)))

        when(type) {
            BeatmapType.SEARCH -> {
                val query = getQuery(event, matcher)

                val bindUser = bindDao.getBindUserFromOsuIDOrNull(user.userID)

                val search = if (query.containsKey("played")
                    || query.containsKey("r")
                    || (query.containsKey("c") && query.map { (_, v) ->
                        v.toString().contains("recommended") || v.toString().contains("follows")
                    }.contains(true))
                    ) {
                    val bind = userApiService.refreshUserTokenInstant(bindUser, true)
                    beatmapApiService.searchBeatmapsetParallel(query, user = bind)
                } else {
                    beatmapApiService.searchBeatmapsetParallel(query)
                }

                if (search.beatmapsets.isEmpty()) {
                    throw NoSuchElementException.Result()
                }

                val (split, _, maxPage) = DataUtil.splitPage(search.beatmapsets, page, 15)

                search.beatmapsets = split

                search.sortBeatmapDiff()

                return SearchParam(search, page, maxPage)
            }

            BeatmapType.MOST_PLAYED -> {
                val conditions = DataUtil.paramMatcher(any, MostPlayedBeatmapFilter.entries.map { it.regex }, REG_RANGE.toRegex())

                // 如果不加井号，则有时候范围会被匹配到这里来
                val rangeInConditions = conditions.lastOrNull()?.firstOrNull()
                val hasRangeInConditions = (rangeInConditions.isNullOrEmpty().not())
                val hasCondition = conditions.dropLast(1).sumOf { it.size } > 0

                val before: Int
                val after: Int

                // 此时这个字段用于筛选模式
                val mode = OsuMode.getMode(matcher.group(FLAG_TYPE))

                val most = if (hasRangeInConditions.not() && hasCondition.not() && mode == OsuMode.DEFAULT) {
                    // 这个数量级肯定很大，所以减小查询
                    if (page <= 1) {
                        before = 0
                        after = 99
                        beatmapApiService.getUserMostPlayedBeatmaps(user.userID, 0, 20)
                    } else if (page <= 50) {
                        before = 0
                        after = 50
                        beatmapApiService.getUserMostPlayedBeatmaps(user.userID, 0, 1000)
                    } else {
                        before = (page - 1) / 50
                        after = 0
                        beatmapApiService.getUserMostPlayedBeatmaps(user.userID, before * 1000, before * 1000 + 1000)
                    }
                } else {
                    before = 0
                    after = 0
                    beatmapApiService.getUserMostPlayedBeatmaps(user.userID, 0, 2000)
                }

                val filter = MostPlayedBeatmapFilter.filterMostPlayBeatmaps(most, conditions)
                    .filter { it.mode.isEqualOrDefault(mode) }

                if (filter.isEmpty()) {
                    throw NoSuchElementException.MostPlayed()
                }

                val page2 = rangeInConditions?.toIntOrNull() ?: page

                val (split, _, maxPage) = DataUtil.splitPage(filter, page2, 20, before * 20, after * 20)

                return MostPlayedParam(user, split, page, maxPage)
            }

            else -> {
                val conditions = DataUtil.paramMatcher(any, BeatmapsetFilter.entries.map { it.regex }, REG_RANGE.toRegex())

                // 如果不加井号，则有时候范围会被匹配到这里来
                val rangeInConditions = conditions.lastOrNull()?.firstOrNull()
                val hasRangeInConditions = (rangeInConditions.isNullOrEmpty().not())
                val hasCondition = conditions.dropLast(1).sumOf { it.size } > 0

                val before: Int
                val after: Int

                val mine = if (hasRangeInConditions.not() && hasCondition.not()) {
                    if (page <= 50) {
                        before = 0
                        after = 50
                        beatmapApiService.getUserBeatmapset(user.userID, type.query, 0, 750)
                    } else {
                        before = (page - 1) / 50
                        after = 0
                        beatmapApiService.getUserBeatmapset(user.userID, type.query, before * 750, before * 750 + 750)
                    }
                } else {
                    before = 0
                    after = 0
                    beatmapApiService.getUserBeatmapset(user.userID, type.query, 0, 1500)
                }

                val filter = BeatmapsetFilter.filterBeatmapsets(mine, conditions)

                if (filter.isEmpty()) {
                    throw NoSuchElementException.UserBeatmapset(type.chinese)
                }

                val page2 = rangeInConditions?.toIntOrNull() ?: page

                val (split, _, maxPage) = DataUtil.splitPage(filter, page2, 15, before * 15, after * 15)

                return MyBeatmapsetParam(user, split.sortBeatmapDiff(), page, maxPage)
            }
        }
    }

    private fun ExploreParam.getImage(): ByteArray {
        return when(this) {
            is MostPlayedParam -> imageService.getPanel(this.toMap(), "A12")
            is MyBeatmapsetParam -> imageService.getPanel(this.toMap(), "A13")
            is SearchParam -> imageService.getPanel(this.toMap(), "A14")
            else -> throw IllegalStateException.ClassCast("探索谱面")
        }
    }

    private fun getQuery(event: MessageEvent, matcher: Matcher): Map<String, Any> {
        val any = (matcher.group(FLAG_ANY) ?: "").trim()

        val query: Map<String, Any>

        val conditions = DataUtil.paramMatcher(any, SearchBeatmapsetFilter.entries.map { it.regex }, REG_RANGE.toRegex(),
            keepWhiteSpace = true)

        // 如果不加井号，则有时候范围会被匹配到这里来
        val rangeInConditions = conditions.lastOrNull()?.firstOrNull()
        val hasRangeInConditions = (rangeInConditions.isNullOrEmpty().not())
        val hasCondition = conditions.dropLast(1).sumOf { it.size } > 0

        if (hasRangeInConditions.not() && hasCondition.not()) {
            // 此时使用最基础的获取
            query = mapOf(
                "q" to any
            )
        } else {
            val groupMode = bindDao.getGroupModeConfig(event)

            val overwritten = if (groupMode != OsuMode.DEFAULT) {
                mapOf(
                    "m" to groupMode.modeValue.toString()
                )
            } else {
                mapOf()
            }

            query = SearchBeatmapsetFilter.buildQuery(conditions, overwritten = overwritten)
        }

        return query
    }

    companion object {
        private fun List<Beatmapset>.sortBeatmapDiff(): List<Beatmapset> {
            if (this.isEmpty()) return this

            for (s in this) {
                if (s.beatmaps.isNullOrEmpty()) continue

                s.beatmaps = s.beatmaps!!.sortedBy { it.starRating }.sortedBy { it.modeInt ?: 0 }
            }

            return this
        }

        internal enum class BeatmapType(val chinese: String, val query: String) {
            SEARCH("搜索",""),

            MOST_PLAYED("最常玩","most_played"),

            FAVOURITE("收藏","favourite"),
            GRAVEYARD("坟场","graveyard"),
            GUEST("客串","guest"),
            LOVED("心选","loved"),
            NOMINATED("提名","nominated"),
            PENDING("待定","pending"),
            RANKED("上架","ranked"),
            ;

            companion object {
                fun getType(string: String?): BeatmapType {
                    return when(string?.dropWhile { it.isWhitespace() }) {
                        "f", "fa", "fav", "favor", "favour", "favourite", "favorite", "收", "藏", "收藏" -> FAVOURITE
                        "g", "gr", "gra", "grave", "graveyard", "坟", "坟场" -> GRAVEYARD
                        "u", "gu", "gue", "guest", "map", "客", "客串" -> GUEST
                        "l", "lv", "lvd", "love", "loved", "心", "心选", "社区喜爱" -> LOVED
                        "m", "mp", "pm", "mop", "most", "play", "played", "mostplay", "mostplayed" -> MOST_PLAYED
                        "n", "nm", "nom", "nominate", "nominated", "点", "提名" -> NOMINATED
                        "p", "pd", "pen", "pend", "pending", "待", "待定" -> PENDING
                        "r", "h", "rk", "ran", "rank", "ranking", "host", "飞", "上架" -> RANKED
                        else -> SEARCH
                    }
                }
            }
        }
    }
}