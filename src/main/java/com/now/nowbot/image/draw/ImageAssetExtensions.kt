package com.now.nowbot.image.draw

import com.now.nowbot.model.osu.Beatmap
import com.now.nowbot.model.osu.Beatmapset
import com.now.nowbot.model.osu.Covers
import com.now.nowbot.model.osu.LazerScore
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.concurrent.ThreadLocalRandom

fun ImageAssets.localSvgText(name: String): String? {
    return readLocal(name)?.toString(Charsets.UTF_8)
}

fun ImageAssets.randomBanner(
    type: String = "default",
    index: Int? = null,
    today: LocalDate = LocalDate.now(ZoneOffset.ofHours(8)),
    random: () -> Double = { ThreadLocalRandom.current().nextDouble() },
): String? {
    if (today == BANNER_SPECIAL_DATE) {
        return local("Banner/a4.webp") ?: local("card-default.webp")
    }

    val maimai = type == "maimai"
    val max = if (maimai) MAIMAI_BANNER_TOTAL else DEFAULT_BANNER_TOTAL
    val resolved = index?.takeIf { it != 0 } ?: weightedRandom(max, random)
    val prefix = if (maimai) "d" else "b"
    return local("Banner/$prefix$resolved.webp") ?: local("card-default.webp")
}

fun ImageAssets.mapBackground(beatmap: Beatmap?, coverType: String = "cover"): String? {
    val set = beatmap?.beatmapset
    return resolveMapBackground(set, beatmap?.beatmapsetID ?: 0L, coverType)
}

fun ImageAssets.mapBackground(beatmapset: Beatmapset?, coverType: String = "cover"): String? {
    return resolveMapBackground(beatmapset, beatmapset?.beatmapsetID ?: 0L, coverType)
}

fun ImageAssets.mapBackground(score: LazerScore, coverType: String = "cover"): String? {
    val beatmapset = score.beatmapset
    val fallbackId = beatmapset.beatmapsetID.takeIf { it > 0 }
        ?: score.beatmap.beatmapsetID.takeIf { it > 0 }
        ?: 0L
    return resolveMapBackground(beatmapset, fallbackId, coverType)
}

private fun ImageAssets.resolveMapBackground(
    set: Beatmapset?,
    fallbackId: Long,
    coverType: String,
): String? {
    val type = coverType.trim().lowercase().let { if (it in VALID_COVER_TYPES) it else "cover" }
    val covers = set?.covers
    var url = when (type) {
        "cover", "silmcover" -> covers?.cover.orEmpty()
        "cover@2x" -> covers?.cover2x.orEmpty()
        "silmcover@2x" -> covers?.slimcover2x.orEmpty()
        "list" -> covers?.list.orEmpty()
        "list@2x" -> covers?.list2x.orEmpty()
        "card" -> covers?.card.orEmpty()
        "card@2x" -> covers?.card2x.orEmpty()
        "raw", "fullsize" -> covers?.let { fullsize(it) }.orEmpty()
        else -> ""
    }

    if (url.isBlank()) {
        val id = set?.beatmapsetID?.takeIf { it > 0 } ?: fallbackId
        if (id > 0) url = "https://assets.ppy.sh/beatmaps/$id/covers/$type.jpg"
    }
    return remote(url) ?: local("card-default.webp")
}

private fun fullsize(covers: Covers): String {
    if (covers.fullsize.isNotBlank()) return covers.fullsize
    if (covers.list.isNotBlank()) {
        return covers.list.replace("@2x", "").replace("list", "fullsize")
    }
    return ""
}

private fun weightedRandom(max: Int, random: () -> Double): Int {
    if (max <= 1) return 1
    val firstWeight = 1.0
    val lastWeight = 3.0
    val totalWeight = (firstWeight + lastWeight) * max / 2
    var value = random() * totalWeight
    for (index in 1..max) {
        val weight = firstWeight + (index - 1) * (lastWeight - firstWeight) / (max - 1)
        value -= weight
        if (value <= 0) return index
    }
    return max
}

private const val DEFAULT_BANNER_TOTAL = 200
private const val MAIMAI_BANNER_TOTAL = 84
private val BANNER_SPECIAL_DATE: LocalDate = LocalDate.of(2026, 4, 26)
private val VALID_COVER_TYPES = setOf(
    "cover", "cover@2x", "silmcover", "silmcover@2x",
    "list", "list@2x", "card", "card@2x", "raw", "fullsize",
)
