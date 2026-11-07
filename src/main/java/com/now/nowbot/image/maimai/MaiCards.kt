package com.now.nowbot.image.maimai

import com.now.nowbot.image.card.CardA1
import com.now.nowbot.image.card.CardA1Data
import com.now.nowbot.image.card.CardI3Data
import com.now.nowbot.image.draw.ImageAssets
import com.now.nowbot.image.svg.Svg
import com.now.nowbot.image.util.Numbers
import com.now.nowbot.model.maimai.ChuBestScore
import com.now.nowbot.model.maimai.MaiBestScore
import com.now.nowbot.model.maimai.MaiScore
import java.util.Locale

object MaiCardA1 {
    private val danNames = listOf("初", "二", "三", "四", "五", "六", "七", "八", "九", "十")

    fun render(user: MaiBestScore.User?, assets: ImageAssets): String {
        return CardA1.render(data(user, assets), assets)
    }

    fun render(user: MaiBestScore.User?, statistics: MaiScoreStatsView?, assets: ImageAssets): String {
        return CardA1.render(data(user, statistics, assets), assets)
    }

    fun render(
        user: MaiBestScore.User?,
        distRating: Int?,
        count: Int,
        size: Int,
        assets: ImageAssets,
    ): String = CardA1.render(data(user, distRating, count, size, assets), assets)

    fun data(user: MaiBestScore.User?, assets: ImageAssets): CardA1Data = data(user, null, assets)

    fun data(
        user: MaiBestScore.User?,
        distRating: Int?,
        count: Int,
        size: Int,
        assets: ImageAssets,
    ): CardA1Data {
        val mai = MaiAssets(assets)
        if (user == null || isEmptyUser(user)) return data(user, null, assets)

        val plate = mai.plate(user.plateID)
        val hasPlate = !plate.isNullOrBlank()
        val rating = user.rating ?: 0
        val percent = if (size > 0) Math.round(count.toDouble() / size * 100) else 0
        val target = distRating ?: 0
        return CardA1Data(
            background = mai.ratingBackground(if (target > 0) target else rating),
            avatar = mai.avatarGuest(),
            subBanner = plate,
            top1 = user.name.orEmpty(),
            top2 = if (hasPlate) "" else user.proberName.orEmpty(),
            left1 = if (hasPlate) user.proberName.orEmpty() else "",
            left2 = danText(user.dan),
            right1 = "Harder: $count ($percent%)",
            right2 = if (target > 0) "Rating: $rating (${target - rating})" else "Rating:",
            right3b = (if (target > 0) target else rating).toString(),
            right3m = "",
        )
    }

    fun data(user: MaiBestScore.User?, statistics: MaiScoreStatsView?, assets: ImageAssets): CardA1Data {
        val mai = MaiAssets(assets)
        if (user == null || isEmptyUser(user)) {
            return CardA1Data(
                background = assets.local("card-default.webp"),
                avatar = assets.local("sticker_qiqi_secretly_observing.png"),
                country = "CN",
                top1 = "YumuBot",
                left1 = "#0",
                left2 = "CN#0",
                right3b = "Bot",
            )
        }

        if (statistics != null) {
            val rating = user.rating ?: 0
            val percent = if (rating > 0) Math.round(statistics.pageRating.toDouble() / rating * 100) else 0
            return CardA1Data(
                background = mai.ratingBackground(statistics.pageRating),
                avatar = mai.avatarGuest(),
                top1 = user.name.orEmpty(),
                top2 = user.proberName.orEmpty(),
                left1 = "count: ${statistics.count}",
                left2 = "total: ${statistics.totalRating}",
                right2 = "Rating: $rating ($percent%)",
                right3b = statistics.pageRating.toString(),
                right3m = "",
            )
        }

        val plate = mai.plate(user.plateID)
        val hasPlate = !plate.isNullOrBlank()
        val dan = danText(user.dan)
        val base = user.base ?: 0

        return CardA1Data(
            background = mai.ratingBackground(user.rating ?: 0),
            avatar = mai.avatarGuest(),
            subBanner = plate,
            top1 = user.name.orEmpty(),
            top2 = if (hasPlate) "" else user.proberName.orEmpty(),
            left1 = if (hasPlate) user.proberName.orEmpty() else "",
            left2 = dan,
            right2 = if (base > 0) "Rating: $base + ${user.additional ?: 0}" else "Rating:",
            right3b = user.rating?.toString().orEmpty(),
            right3m = "",
        )
    }

    private fun isEmptyUser(user: MaiBestScore.User): Boolean {
        return user.name.isNullOrBlank() &&
            user.proberName.isNullOrBlank() &&
            (user.rating ?: 0) == 0 &&
            user.plateID == 0
    }

    private fun danText(dan: Int?): String {
        if (dan == null || dan == 0) return "初学者"
        if (dan in 1..10) return danNames[dan - 1] + "段"
        if (dan in 11..20) return "真" + danNames[dan - 11] + "段"
        return when (dan) {
            21 -> "真皆伝"
            22 -> "裏皆伝"
            else -> ""
        }
    }
}

object MaiCards {
    fun scoreToCardI3(score: MaiScore, assets: ImageAssets): CardI3Data {
        val mai = MaiAssets(assets)
        val achievementText = String.format(Locale.ROOT, "%.4f", score.achievements)
        val rating = score.rating
        val ratingMax = MaiAssets.maximumRating(score.star)
        val ratingMaxText = if (rating >= ratingMax) " [MAX]" else " [$ratingMax]"
        val isUtage = score.difficulty.startsWith("U") || score.level.contains("?")
        val tooBright = score.index == 1 || score.index == 4
        val difficultyColor = MaiAssets.difficultyColor(score.index, isUtage)
        val difficulty = if (score.star <= 0.0) {
            score.level.ifBlank { "?" }
        } else {
            Svg.number(score.star)
        }

        val combo = mai.comboIcon(score.combo)
            ?.let { Svg.image(0.0, 0.0, 27.0, 30.0, it) }
            .orEmpty()
        val sync = mai.syncIcon(score.sync)
            ?.let { Svg.image(0.0, 0.0, 27.0, 30.0, it) }
            .orEmpty()

        return CardI3Data(
            background = mai.rankBackground(score.rank),
            cover = mai.cover(score.songID),
            rank = mai.rankImage(score.rank),
            type = mai.typeImage(score.type),
            level = MaiAssets.starLevel(score.score, score.max),
            title = score.title,
            title2 = score.alias.orEmpty(),
            left1 = score.artist,
            left2 = score.charter,
            left3 = rating.toString(),
            left4 = ratingMaxText,
            indexB = achievementText.dropLast(3),
            indexM = achievementText.takeLast(3),
            indexR = "%",
            indexBSize = 32.0,
            indexMSize = 20.0,
            indexRSize = 18.0,
            label1 = difficulty,
            label2 = if (score.position >= 1) "#${score.position}" else "",
            label3 = score.songID.toString(),
            colorText = "#fff",
            colorLabel1 = if (tooBright) "#1c1719" else "#fff",
            colorLabel2 = if (tooBright) "#1c1719" else "#fff",
            colorLabel3 = if (tooBright) "#1c1719" else "#fff",
            colorLeft = difficultyColor,
            colorRrect1 = difficultyColor,
            colorRrect2 = difficultyColor,
            colorRrect3 = difficultyColor,
            component1 = combo,
            component2 = sync,
            component3 = "",
            left3IsRight = false,
        )
    }
}

fun chuScoreToCardI3(score: com.now.nowbot.model.maimai.ChuScore, assets: ImageAssets): CardI3Data {
    val mai = MaiAssets(assets)
    val scoreParts = Numbers.roundParts(score.score, -4, 0)
    val difficultyColor = MaiAssets.chunithmDifficultyColor(score.index)
    val rating = score.rating
    val ratingMax = score.star + 2.15
    val ratingMaxText = if (score.score >= 1009000) {
        " [MAX]"
    } else {
        " [" + Numbers.round(ratingMax, 2) + "]"
    }
    val difficulty = if (score.star <= 0.0) score.level.ifBlank { "?" } else Svg.number(score.star)
    val tooBright = score.index == 1
    val chain = mai.chunithmChainIcon(score.combo)
        ?.let { Svg.image(0.0, 0.0, 105.0, 15.0, it) }
        .orEmpty()

    return CardI3Data(
        background = mai.chunithmRankBackground(score.score),
        cover = mai.chunithmCover(score.songID),
        rank = mai.chunithmRankImage(score.score),
        type = null,
        level = 0,
        title = score.title,
        title2 = score.alias.orEmpty(),
        left1 = score.artist,
        left2 = score.charter,
        left3 = Numbers.round(rating, 2),
        left4 = ratingMaxText,
        indexB = scoreParts.integer,
        indexM = scoreParts.decimal,
        indexR = "",
        indexBSize = 32.0,
        indexMSize = 20.0,
        indexRSize = 18.0,
        label1 = difficulty,
        label2 = if (score.position >= 1) "#${score.position}" else "",
        label3 = score.songID.toString(),
        colorText = "#fff",
        colorLabel1 = if (tooBright) "#1c1719" else "#fff",
        colorLabel2 = if (tooBright) "#1c1719" else "#fff",
        colorLabel3 = if (tooBright) "#1c1719" else "#fff",
        colorLeft = difficultyColor,
        colorRrect1 = difficultyColor,
        colorRrect2 = difficultyColor,
        colorRrect3 = difficultyColor,
        component1 = "",
        component2 = "",
        component3 = chain,
        left3IsRight = true,
    )
}


object MaiChuCardA1 {
    fun render(user: ChuBestScore.User?, assets: ImageAssets): String {
        return CardA1.render(data(user, assets), assets)
    }

    fun data(user: ChuBestScore.User?, assets: ImageAssets): CardA1Data {
        val mai = MaiAssets(assets)
        val rating = Numbers.roundParts(user?.rating ?: 0.0, 2)
        val base = user?.base ?: 0.0
        return CardA1Data(
            background = mai.chunithmRatingBackground(user?.rating ?: 0.0),
            avatar = assets.local("Chunithm/avatar-guest.png"),
            top1 = user?.name.orEmpty(),
            top2 = user?.probername.orEmpty(),
            left1 = "",
            left2 = if ((user?.average ?: 0.0) > 0) "B30 Avg.: ${Numbers.floor(user?.average, 2)}" else "",
            right2 = if (base > 0) {
                "Rating: ${Numbers.floor(base, 2)} + ${Numbers.floor(user?.additional ?: 0.0, 2)}"
            } else {
                "Rating:"
            },
            right3b = rating.integer,
            right3m = rating.decimal,
        )
    }
}
