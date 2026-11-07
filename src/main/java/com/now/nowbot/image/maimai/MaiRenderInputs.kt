package com.now.nowbot.image.maimai

import com.now.nowbot.model.enums.MaiCabinet
import com.now.nowbot.model.maimai.ChuBestScore
import com.now.nowbot.model.maimai.ChuScore
import com.now.nowbot.model.maimai.ChuSong
import com.now.nowbot.model.maimai.MaiBestScore
import com.now.nowbot.model.maimai.MaiFit
import com.now.nowbot.model.maimai.MaiScore
import com.now.nowbot.model.maimai.MaiSong
import com.now.nowbot.service.messageServiceImpl.ChuBestScoreService
import com.now.nowbot.service.messageServiceImpl.MaiBestScoreService
import com.now.nowbot.service.messageServiceImpl.MaiDistributionService
import com.now.nowbot.service.messageServiceImpl.MaiFilterService
import com.now.nowbot.service.messageServiceImpl.MaiFindService
import com.now.nowbot.service.messageServiceImpl.MaiScoreService
import com.now.nowbot.service.messageServiceImpl.MaiVersionScoreService
import com.now.nowbot.util.JacksonUtil
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

internal fun MaiFilterService.MaiFilterParam.toRenderParam(): MaiFilterRenderParam {
    val maxPage = ceil(score.size / 50.0).toInt()
    val page = max(min(this.page, maxPage), 1)
    return MaiFilterRenderParam(
        user = this.user,
        scores = score.drop((page - 1) * 50).take(50),
        page = page,
        maxPage = maxPage,
        type = this.type,
    )
}

internal fun readMaiFindParam(body: Map<String, Any?>): MaiFindService.MaiFindParam {
    return MaiFindService.MaiFindParam(
        songs = convertList(body["songs"], MaiSong::class.java),
        page = intValue(body["page"], 1),
        maxPage = intValue(body["max_page"], 1),
        count = intValue(body["count"], 0),
    )
}

internal fun readMaiFilterRenderParam(body: Map<String, Any?>): MaiFilterRenderParam {
    return MaiFilterRenderParam(
        user = userValue(body["user"]) ?: MaiBestScore.User(),
        scores = convertList(body["scores"], MaiScore::class.java),
        page = intValue(body["page"], 1),
        maxPage = intValue(body["max_page"], 1),
        type = stringValue(body["type"]),
    )
}

internal fun readMaiVersusRenderParam(body: Map<String, Any?>): MaiVersusRenderParam {
    return MaiVersusRenderParam(
        me = userValue(body["me"]),
        other = userValue(body["other"]),
        myDist = intValue(body["my_dist"], 0),
        otherDist = intValue(body["other_dist"], 0),
        my = intList(body["my"]),
        others = intList(body["others"]),
        status = stringValue(body["status"]).ifBlank { "UNKNOWN" },
    )
}

private fun <T> convert(value: Any?, type: Class<T>): T? {
    if (value == null) return null
    if (type.isInstance(value)) return type.cast(value)
    return JacksonUtil.mapper.convertValue<T>(value, type)
}

private fun <T> convertList(value: Any?, type: Class<T>): List<T> {
    val list = value as? List<*> ?: return emptyList()
    return list.mapNotNull { item ->
        convert(item, type)
    }
}

private fun userValue(value: Any?): MaiBestScore.User? = when (value) {
    null -> null
    is MaiBestScore.User -> value
    is Map<*, *> -> MaiBestScore.User(
        name = value["name"]?.toString(),
        proberName = value["probername"]?.toString() ?: value["prober_name"]?.toString(),
        dan = intOrNull(value["dan"]),
        plate = value["plate"]?.toString(),
        rating = intOrNull(value["rating"]),
        base = intOrNull(value["base"]),
        additional = intOrNull(value["additional"]),
        plateID = intOrNull(value["plate_id"]) ?: 0,
    )
    else -> null
}

private fun intValue(value: Any?, default: Int): Int = intOrNull(value) ?: default

private fun intOrNull(value: Any?): Int? = when (value) {
    is Number -> value.toInt()
    is String -> value.toIntOrNull()
    else -> null
}

private fun stringValue(value: Any?): String = value?.toString().orEmpty()

private fun intList(value: Any?): List<Int> {
    val list = value as? List<*> ?: return emptyList()
    return list.mapNotNull { intOrNull(it) }
}

internal fun MaiBestScoreService.PanelMAParam.toRenderParam(): MaiMultipleRenderParam =
    MaiMultipleRenderParam(
        user = user,
        scores = scores,
        scoresLatest = scoresLatest,
        panel = "MB",
    )

internal fun MaiScoreService.MaiScoreParam.toMultipleRenderParam(): MaiMultipleRenderParam =
    MaiMultipleRenderParam(
        user = user,
        scores = scores,
        scoresLatest = emptyList(),
        panel = "MS",
        page = page,
        maxPage = maxPage,
        statistics = statistics?.toView(),
    )

internal fun ChuBestScoreService.PanelMA2Param.toRenderParam(): ChuMultipleRenderParam =
    ChuMultipleRenderParam(
        user = user,
        scores = scores,
        scoresLatest = scoresLatest,
        scoresSelection = scoresSelection,
        panel = "CB",
    )

internal fun MaiScoreService.MaiScoreStatistics.toView(): MaiScoreStatsView =
    MaiScoreStatsView(
        count = count,
        pageRating = pageRating,
        totalRating = totalRating,
        averageRating = averageRating,
        averageAchievement = averageAchievement,
        averageStar = averageStar,
    )

internal fun readMaiMultiple(body: Map<String, Any?>): MaiMultipleRenderParam {
    return MaiMultipleRenderParam(
        user = userValue(body["user"]),
        scores = convertList(body["scores"], MaiScore::class.java),
        scoresLatest = convertList(body["scores_latest"], MaiScore::class.java),
        panel = stringValue(body["panel"]).ifBlank { "MA" },
        page = intValue(body["page"], 1),
        maxPage = intValue(body["max_page"], 1),
        statistics = statsValue(body["statistics"]),
    )
}

internal fun readChuMultiple(body: Map<String, Any?>): ChuMultipleRenderParam {
    return ChuMultipleRenderParam(
        user = chuUser(body["user"]),
        scores = convertList(body["scores"], ChuScore::class.java),
        scoresLatest = convertList(body["scores_latest"], ChuScore::class.java),
        scoresSelection = convertList(body["scores_selection"], ChuScore::class.java),
        panel = stringValue(body["panel"]).ifBlank { "CB" },
    )
}

internal fun readMaiScoreParam(body: Map<String, Any?>): MaiScoreService.MaiScoreParam {
    return MaiScoreService.MaiScoreParam(
        user = userValue(body["user"]),
        songs = convertList(body["songs"], MaiSong::class.java),
        scores = convertList(body["scores"], MaiScore::class.java),
        cabinet = MaiCabinet.getCabinet(stringValue(body["version"])),
        statistics = statsValue(body["statistics"])?.toStatistics(),
        page = intValue(body["page"], 1),
        maxPage = intValue(body["max_page"], 1),
    )
}

internal fun readMaiDistribution(body: Map<String, Any?>): MaiDistributionService.PanelMDParam {
    return MaiDistributionService.PanelMDParam(
        user = userValue(body["user"]) ?: MaiBestScore.User(),
        deluxe = convertList(body["scores_latest"], MaiDistributionService.MaiDistScore::class.java),
        standard = convertList(body["scores"], MaiDistributionService.MaiDistScore::class.java),
        rating = intValue(body["rating"], 0),
        count = intValue(body["count"], 0),
        size = intValue(body["size"], 0),
    )
}

internal fun readMaiVersion(body: Map<String, Any?>): MaiVersionScoreService.MaiVersionResponse {
    return MaiVersionScoreService.MaiVersionResponse(
        user = userValue(body["user"]) ?: MaiBestScore.User(),
        plateID = intValue(body["plate"], 0),
        plateList = convertList(body["plate_list"], MaiVersionScoreService.MaiPlateList::class.java),
        count15 = intValue(body["count_15"], 0),
        finished15 = intValue(body["finished_15"], 0),
        count12 = intValue(body["count_12"], 0),
        finished12 = intValue(body["finished_12"], 0),
    )
}

internal fun readMaiSingleMai(body: Map<String, Any?>): MaiBestScoreService.PanelMEParam {
    return MaiBestScoreService.PanelMEParam(
        user = userValue(body["user"]) ?: MaiBestScore.User(),
        score = convert(body["score"], MaiScore::class.java) ?: MaiScore(),
        song = convert(body["song"], MaiSong::class.java) ?: MaiSong(),
        chart = convert(body["chart"], MaiFit.ChartData::class.java) ?: MaiFit.ChartData(),
        diff = convert(body["diff"], MaiFit.DiffData::class.java) ?: MaiFit.DiffData(),
    )
}

internal fun readMaiSingleChu(body: Map<String, Any?>): ChuBestScoreService.PanelME2Param {
    return ChuBestScoreService.PanelME2Param(
        user = chuUser(body["user"]) ?: ChuBestScore.User(null, null, null, null, null, null),
        score = convert(body["score"], ChuScore::class.java) ?: ChuScore(),
        song = convert(body["song"], ChuSong::class.java) ?: ChuSong(),
    )
}

private fun statsValue(value: Any?): MaiScoreStatsView? {
    val map = value as? Map<*, *> ?: return null
    return MaiScoreStatsView(
        count = intValue(map["count"], 0),
        pageRating = intValue(map["page_rating"], 0),
        totalRating = intValue(map["total_rating"], 0),
        averageRating = doubleValue(map["average_rating"]),
        averageAchievement = doubleValue(map["average_achievement"]),
        averageStar = doubleValue(map["average_star"]),
    )
}

internal fun MaiScoreStatsView.toStatistics(): MaiScoreService.MaiScoreStatistics =
    MaiScoreService.MaiScoreStatistics(
        count = count,
        pageRating = pageRating,
        totalRating = totalRating,
        averageRating = averageRating,
        averageAchievement = averageAchievement,
        averageStar = averageStar,
    )

private fun chuUser(value: Any?): ChuBestScore.User? = when (value) {
    null -> null
    is ChuBestScore.User -> value
    is Map<*, *> -> ChuBestScore.User(
        name = value["name"]?.toString(),
        probername = (value["probername"] ?: value["prober_name"])?.toString(),
        rating = doubleOrNull(value["rating"]),
        base = doubleOrNull(value["base"]),
        additional = doubleOrNull(value["additional"]),
        average = doubleOrNull(value["average"]),
    )
    else -> null
}

private fun doubleValue(value: Any?): Double = doubleOrNull(value) ?: 0.0

private fun doubleOrNull(value: Any?): Double? = when (value) {
    is Number -> value.toDouble()
    is String -> value.toDoubleOrNull()
    else -> null
}
