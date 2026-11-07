package com.now.nowbot.image.maimai

import com.now.nowbot.image.card.CardI3Data
import com.now.nowbot.image.draw.ImageAssets
import com.now.nowbot.image.svg.Svg
import com.now.nowbot.image.util.Numbers
import com.now.nowbot.model.maimai.MaiScore
import java.util.Locale

object MaiMD {
    fun scoreToCardI3(dist: com.now.nowbot.service.messageServiceImpl.MaiDistributionService.MaiDistScore, assets: ImageAssets): CardI3Data {
        val mai = MaiAssets(assets)
        val score: MaiScore = dist.score
        val achievementText = String.format(Locale.ROOT, "%.4f", score.achievements)
        val fitRating = Math.round(dist.rating.toDouble()).toString()
        val rating = Math.round(score.rating.toDouble()).toString()
        val isUtage = score.difficulty.startsWith("U") || score.level.contains("?")
        val tooBright = score.index == 1 || score.index == 4
        val difficultyColor = MaiAssets.difficultyColor(score.index, isUtage)
        val level = if (dist.chart.fit > 0) {
            Numbers.floor(dist.chart.fit, 2) + " [${Svg.number(score.star)}]"
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
            left3 = fitRating,
            left4 = " <- $rating",
            indexB = achievementText.dropLast(3),
            indexM = achievementText.takeLast(3),
            indexR = "%",
            indexBSize = 32.0,
            indexMSize = 20.0,
            indexRSize = 18.0,
            label1 = level,
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
