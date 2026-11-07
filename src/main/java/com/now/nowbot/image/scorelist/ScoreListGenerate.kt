package com.now.nowbot.image.scorelist

import com.now.nowbot.image.card.CardA2Data
import com.now.nowbot.image.card.CardCData
import com.now.nowbot.image.card.CardCSideColor
import com.now.nowbot.image.card.CardI4Data
import com.now.nowbot.image.draw.ImageAssets
import com.now.nowbot.image.draw.Mods
import com.now.nowbot.image.font.FontFace
import com.now.nowbot.image.font.Fonts
import com.now.nowbot.image.svg.Svg
import com.now.nowbot.image.util.ColorArray
import com.now.nowbot.image.util.Colors
import com.now.nowbot.image.util.GameModes
import com.now.nowbot.image.util.Numbers
import com.now.nowbot.image.util.Rankings
import com.now.nowbot.image.util.Texts
import com.now.nowbot.image.util.TimeFormat
import com.now.nowbot.model.match.MatchRating
import com.now.nowbot.model.match.SeriesRating
import com.now.nowbot.model.osu.Beatmap
import com.now.nowbot.model.osu.LazerScore
import com.now.nowbot.model.osu.LazerScoreWithFcPP
import kotlin.math.max

object ScoreListGenerate {
    private val DARK_RANKS = setOf("SSH", "SS", "XH", "X")

    fun score2CardC(
        score: LazerScore,
        identifier: Int = 1,
        assets: ImageAssets,
        listImage: String? = null,
        backgroundImage: String? = null,
    ): CardCData {
        val list = listImage ?: ScoreListImages.scoreBackground(score, "list@2x", assets)
        val background = backgroundImage ?: ScoreListImages.scoreBackground(score, "cover", assets)
        val beatmap = score.beatmap
        val beatmapset = score.beatmapset
        val rank = score.rank
        val star = beatmap.starRating
        val acc = Numbers.floor(score.accuracy * 100, 2) + "%"
        val combo = "${score.maxCombo}x"
        val timeDiff = TimeFormat.difference(score.endedTime, 0)
        val modsWidth = Mods.lazerModsWidth(score.mods, 60.0, 160.0, 6.0, true, false)
        val difficultyName = if (beatmap.difficultyName.isNotEmpty()) {
            Fonts.torus.cutStringTail(
                GameModes.getKeyDifficulty(beatmap), 24.0,
                500.0 - 10.0 - modsWidth - Fonts.torus.width("[] -   ()$acc$combo$timeDiff", 24.0), true,
            )
        } else {
            ""
        }
        val colorIndex = if (rank in DARK_RANKS) "#2A2226" else "#fff"
        val artist2 = Fonts.torus.cutStringTail(
            beatmapset.artist, 24.0,
            500.0 - 10.0 - modsWidth - Fonts.torus.width(" // ${beatmapset.creator}", 24.0), true,
        )
        val title2 = beatmapset.aliases?.firstOrNull()?.toString()
            ?: if (beatmapset.title == beatmapset.titleUnicode) "" else beatmapset.titleUnicode
        val indexB = if (score.pp <= 10000) Numbers.jsRound(score.pp).toLong().toString()
        else Numbers.floor(score.pp, 1, -1)

        val notPassed = !score.passed || rank == "F"
        val progress = if (score.totalHit > 0) score.scoreHit.toDouble() / score.totalHit else 1.0
        val percent = Numbers.jsRound(progress * 100).toLong()
        val indexL = if (notPassed) "$percent%" else ""
        val starColor = Colors.getStarRatingColor(star)
        val star2Color = Colors.getStarRatingColors(star)
        val colorRight = if (notPassed && score.approximateRank != null) {
            Colors.getRankColors(score.approximateRank, true)
        } else {
            Colors.getRankColors(rank)
        }
        val colorLabel12 = if (star < 4.0) "#1c1719" else "#fff"
        val notDeleted = star > 0.0
        val title1 = if (notDeleted) beatmapset.title else "Deleted Beatmap"
        val left1 = if (notDeleted) "$artist2 // ${beatmapset.creator}" else ""
        val left2 = if (notDeleted) {
            "[$difficultyName] - $acc $combo ($timeDiff)"
        } else {
            "$acc $combo ($timeDiff)"
        }

        return CardCData(
            background = background,
            cover = list,
            type = ScoreListImages.scoreType(score, assets),
            title = title1,
            title2 = title2,
            left1 = left1,
            left2 = left2,
            indexB = indexB,
            indexM = "PP",
            indexL = indexL,
            indexBSize = 48.0,
            indexMSize = 36.0,
            indexLSize = 24.0,
            label1 = Numbers.floor(star, 1),
            label2 = beatmap.beatmapID.toString(),
            label5 = "#$identifier",
            mods = score.mods,
            colorTitle2 = "#bbb",
            colorRight = CardCSideColor.Gradient(colorRight),
            colorLeft = CardCSideColor.Gradient(star2Color),
            colorIndex = colorIndex,
            colorLabel1 = starColor,
            colorLabel2 = starColor,
            colorLabel3 = "",
            colorLabel4 = "",
            colorLabel5 = starColor,
            colorLabel12 = colorLabel12,
            colorLeft12 = "#bbb",
            fontTitle2 = "PuHuiTi",
            fontLabel4 = "torus",
            progress = if (notPassed) progress else 1.0,
        )
    }

    fun score2CardI4(
        score: LazerScore,
        identifier: Int = 1,
        assets: ImageAssets,
        listImage: String? = null,
    ): CardI4Data {
        val cover = listImage ?: ScoreListImages.scoreBackground(score, "list@2x", assets)
        val rankColor = Colors.getRankColors(score.rank)
        val type = ScoreListImages.scoreType(score, assets)
        val acc = Numbers.floorParts(score.accuracy * 100, 2)
        val combo = "${score.maxCombo}x"
        val rank = ScoreListImages.rankImage(score.rank, assets)
        val title2Original = if (score.beatmapset.title == score.beatmapset.titleUnicode) {
            ""
        } else {
            score.beatmapset.titleUnicode
        }
        val title2Font = titleFont(title2Original)
        val title2Size = if (Texts.isAscii(title2Original)) 16.0 else 14.0
        val title2 = title2Font.cutStringTail(title2Original, title2Size, 65.0)
        val title2Width = title2Font.width(title2, title2Size)
        val artistMaxWidth = 185.0 - Fonts.torus.width(" // ${score.beatmapset.creator}", 16.0) - title2Width
        val artist = Fonts.torus.cutStringTail(score.beatmapset.artist, 16.0, artistMaxWidth, true)
        val left1 = "$artist // ${score.beatmapset.creator}"
        val indexB = if (score.pp <= 10000) Numbers.jsRound(score.pp).toLong().toString()
        else Numbers.floor(score.pp, 1, -1)
        val star = score.beatmap.starRating
        val starColor = Colors.getStarRatingColor(star)
        val colorLabel12 = if (star < 4.0) "#1c1719" else "#fff"
        val level = Rankings.getOsuDXRatingStar(
            score.statistics,
            score.maximumStatistics,
            GameModes.getGameMode(score.mode, 1),
        )

        return CardI4Data(
            cover = cover,
            rank = rank,
            type = type,
            level = level,
            title = score.beatmapset.title,
            title2 = title2,
            left1 = left1,
            left2 = score.beatmap.difficultyName,
            left3 = acc.integer,
            left4 = acc.decimal + "% " + combo,
            indexB = indexB,
            indexM = "PP",
            indexBSize = 32.0,
            indexMSize = 20.0,
            label1 = Numbers.floor(star, 1),
            label2 = "#$identifier",
            colorText = "#fff",
            colorLabel1 = colorLabel12,
            colorLabel2 = colorLabel12,
            colorLeft = starColor,
            colorRrect1 = starColor,
            colorRrect2 = starColor,
            colorBackgrounds = rankColor,
            mods = score.mods,
        )
    }

    fun fixedBestScore2CardC(
        score: LazerScoreWithFcPP,
        rank: Int,
        rankAfter: Int,
        assets: ImageAssets,
    ): CardCData {
        val card = score2CardC(score.score, rank, assets)
        val modsWidth = Mods.lazerModsWidth(score.score.mods, 60.0, 160.0, 6.0, true, false)
        val timeDiff = TimeFormat.difference(score.score.endedTime, 0)
        val rankAfterText = " -> $rankAfter"
        val difficultyName = if (score.score.beatmap.difficultyName.isNotEmpty()) {
            Fonts.torus.cutStringTail(
                GameModes.getKeyDifficulty(score.score.beatmap), 24.0,
                500.0 - 10.0 - modsWidth -
                    Fonts.torus.width("[] - BP ()$rank$rankAfterText$timeDiff", 24.0),
                true,
            )
        } else {
            ""
        }
        return card.copy(left2 = "[$difficultyName] - BP$rank$rankAfterText ($timeDiff)")
    }

    fun topScore2CardC(score: LazerScore, identifier: Int, assets: ImageAssets): CardCData {
        val card = score2CardC(score, identifier, assets)
        val modsWidth = Mods.lazerModsWidth(score.mods, 60.0, 160.0, 6.0, true, false)
        val username = score.user.username
        val artist = Fonts.torus.cutStringTail(
            score.beatmapset.artist, 24.0,
            500.0 - 10.0 - modsWidth - Fonts.torus.width(" // $username", 24.0), true,
        )
        return card.copy(
            left1 = "$artist // $username",
            cover = ScoreListImages.avatar(score.user, assets),
        )
    }

    fun mostPlayed2CardC(beatmap: Beatmap, identifier: Int, assets: ImageAssets): CardCData {
        val beatmapset = beatmap.beatmapset
        val list = ScoreListImages.mapBackground(beatmap, "list@2x", assets)
        val background = ScoreListImages.mapBackground(beatmap, "cover", assets)
        val difficultyName = if (beatmap.difficultyName.isNotEmpty()) {
            Fonts.torus.cutStringTail(GameModes.getKeyDifficulty(beatmap), 24.0, 490.0, true)
        } else {
            ""
        }
        val artist = Fonts.torus.cutStringTail(beatmapset?.artist.orEmpty(), 24.0, 490.0, true)
        val aliases = beatmapset?.aliases.orEmpty()
        val title2 = aliases.firstOrNull()?.toString()
            ?: if (beatmapset?.title == beatmapset?.titleUnicode) "" else beatmapset?.titleUnicode.orEmpty()
        val playCount = beatmap.currentPlayCount ?: 0
        val colorIndex = if (playCount > 1000) "#2A2226" else "#fff"
        val rank2Color = mostPlayedRankColor(playCount)
        val mode = " (${beatmap.mode.shortName})"
        val star = beatmap.starRating
        val starColor = Colors.getStarRatingColor(star)
        val star2Color = Colors.getStarRatingColors(star)
        val colorLabel12 = if (star < 4.0) "#1c1719" else "#fff"

        return CardCData(
            background = background,
            cover = list,
            type = "",
            title = beatmapset?.title.orEmpty(),
            title2 = title2,
            left1 = "$artist // ${beatmapset?.creator.orEmpty()}",
            left2 = "[$difficultyName]$mode",
            indexB = playCount.toString(),
            indexM = "",
            indexL = "PC",
            indexBSize = 48.0,
            indexMSize = 36.0,
            indexLSize = 24.0,
            label1 = Numbers.floor(star, 1),
            label2 = beatmap.beatmapID.toString(),
            label5 = "#$identifier",
            mods = emptyList(),
            colorTitle2 = "#bbb",
            colorRight = CardCSideColor.Gradient(rank2Color),
            colorLeft = CardCSideColor.Gradient(star2Color),
            colorIndex = colorIndex,
            colorLabel1 = starColor,
            colorLabel2 = starColor,
            colorLabel3 = "",
            colorLabel4 = "",
            colorLabel5 = starColor,
            colorLabel12 = colorLabel12,
            colorLeft12 = "#bbb",
            fontTitle2 = "PuHuiTi",
            fontLabel4 = "torus",
            progress = 1.0,
        )
    }

    fun matchPlayer2CardC(
        player: MatchRating.PlayerData,
        isTeamVs: Boolean,
        assets: ImageAssets,
        useTeamColor: Boolean = true,
        showAssociatedRound: Boolean = false,
    ): CardCData {
        val user = player.player
        val playerClass = player.playerClass
        val category = playerClass?.category.orEmpty()
        val team = teamConfig(player.team)
        val playerBackground = if (useTeamColor) {
            assets.local(team.background)
        } else {
            playerClassBackground(category, assets)
        }
        val rws = Numbers.jsRound(player.rws * 10000) / 100.0
        val totalCount = player.win + player.lose
        val winRate = Numbers.jsRound(player.win.toDouble() / max(totalCount, 1) * 100).toLong()
        val totalScore = Numbers.floor(player.total, 2)
        val left1 = if (isTeamVs) {
            "$totalScore // ${player.win}W-${player.lose}L ($winRate%)"
        } else {
            "$totalScore // ${player.win}W-${totalCount}P"
        }
        val associated = if (showAssociatedRound) {
            val associatedRate = Numbers.jsRound(
                totalCount.toDouble() / max(player.associatedRoundCount, 1) * 100,
            ).toLong()
            " // ${totalCount}P-${player.associatedRoundCount}T ($associatedRate%)"
        } else {
            ""
        }
        val left2 = "#${player.ranking} (${Svg.number(rws)})$associated"
        val colorIndex = if (category == "BC") "#2A2226" else "#FFF"
        val colors = playerClassColors(category)
        val avatar = ScoreListImages.avatar(user, assets)
        val mra = Numbers.floorParts(player.mra, 2)

        return CardCData(
            background = playerBackground,
            cover = avatar,
            type = "",
            title = user.username,
            title2 = user.countryCode,
            left1 = left1,
            left2 = left2,
            indexB = mra.integer,
            indexM = mra.decimal,
            indexBSize = 48.0,
            indexMSize = 36.0,
            label1 = "",
            label2 = "",
            label3 = playerClass?.english.orEmpty(),
            label4 = playerClass?.chinese.orEmpty(),
            mods = emptyList(),
            colorTitle2 = "#aaa",
            colorRight = CardCSideColor.Gradient(colors),
            colorLeft = CardCSideColor.Gradient(team.colors.asReversed()),
            colorIndex = colorIndex,
            colorLabel1 = "",
            colorLabel2 = "",
            colorLabel3 = "#382E32",
            colorLabel4 = "#382E32",
            colorLeft12 = "#bbb",
            fontTitle2 = "torus",
            fontLabel4 = "PuHuiTi",
        )
    }

    fun matchRating2CardA2(
        rating: MatchRating,
        assets: ImageAssets,
        background: String? = null,
        isMatchStart: Boolean = false,
    ): CardA2Data {
        val stat = rating.match.statistics
        val star = Numbers.floor(rating.averageStarRating, 2)
        val backgroundImage = background
            ?: rating.firstMapSID.takeIf { it > 0 }?.let {
                assets.remote("https://assets.ppy.sh/beatmaps/$it/covers/list@2x.jpg")
            }
            ?: assets.local("card-default.webp")
        val split = splitMatchName(stat.name)
        val title2 = if (split.team1.isNotEmpty()) "${split.team1} vs ${split.team2}" else ""
        val left1 = "Round: ${rating.roundCount}"
        val left2 = "Player: ${rating.playerCount}"
        val left3 = "Score: ${rating.scoreCount}"
        val right1 = "Average Star $star*"
        val right2 = "MID ${stat.matchID}"
        val right3b: String
        val right3m: String
        if (rating.skipping) {
            right3b = ""
            right3m = "Warm Up"
        } else if (rating.isTeamVs) {
            val red = rating.teamPointMap["red"] ?: 0
            val blue = rating.teamPointMap["blue"] ?: 0
            right3b = if (red + blue <= 0) "TeamVs" else "$red : $blue"
            right3m = ""
        } else {
            right3b = (rating.roundCount + if (isMatchStart) 1 else 0).toString()
            right3m = "x"
        }

        return CardA2Data(
            background = backgroundImage,
            mapStatus = "",
            title1 = split.name,
            title2 = title2,
            left1 = left1,
            left2 = left2,
            left3 = left3,
            right1 = right1,
            right2 = right2,
            right3b = right3b,
            right3m = right3m,
        )
    }

    fun seriesRating2CardA2(
        series: SeriesRating,
        assets: ImageAssets,
        background: String? = null,
    ): CardA2Data {
        val star = Numbers.floor(series.averageStarRating, 2)
        val backgroundImage = background
            ?: series.firstMapSID.takeIf { it > 0 }?.let {
                assets.remote("https://assets.ppy.sh/beatmaps/$it/covers/list@2x.jpg")
            }
            ?: assets.local("card-default.webp")
        val split = splitMatchName(series.statistics.name)
        val title2 = if (split.team1.isNotEmpty()) "${split.team1} vs ${split.team2}" else ""
        val left2 = "Match: ${series.matchCount} // Round: ${series.roundCount}"
        val left3 = "Player: ${series.playerCount} // Score: ${series.scoreCount}"
        val right1 = "Average Star $star*"
        val right2 = "Scores per Player"
        val spNumber = if (series.playerCount > 0) {
            Numbers.floorParts(series.scoreCount.toDouble() / series.playerCount, 2)
        } else {
            Numbers.floorParts(0, 2)
        }
        val right3b = if (series.playerCount > 0) spNumber.integer else "0"
        val right3m = if (series.playerCount > 0) spNumber.decimal else ""

        return CardA2Data(
            background = backgroundImage,
            mapStatus = "",
            title1 = split.name,
            title2 = title2,
            left1 = "",
            left2 = left2,
            left3 = left3,
            right1 = right1,
            right2 = right2,
            right3b = right3b,
            right3m = right3m,
        )
    }

    fun splitMatchName(text: String?): MatchName {
        if (text.isNullOrEmpty()) return MatchName("", "", "")
        fun sanitize(value: String): String = value.replace(Regex("[()（）\\[\\]【】{}]"), "").trim()

        val colon = text.indexOfFirst { it == ':' || it == '：' }
        if (colon < 0) return MatchName(sanitize(text), "", "")
        val rawName = text.substring(0, colon)
        val rawTeams = text.substring(colon + 1)
        val parts = rawTeams.split(Regex("[Vv][Ss]"))
        if (parts.size < 2) return MatchName(sanitize(text), "", "")
        val name = sanitize(rawName)
        val team1 = sanitize(parts[0])
        val team2 = sanitize(parts[1])
        if (team1.isEmpty() || team2.isEmpty()) return MatchName(sanitize(text), "", "")
        return MatchName(name, team1, team2)
    }

    private fun titleFont(text: String): FontFace = if (Texts.isAscii(text)) Fonts.torus else Fonts.puHuiTi

    private fun mostPlayedRankColor(playCount: Int): List<String> = when {
        playCount > 1000 -> ColorArray.rainbow
        playCount > 500 -> ColorArray.lightYellow
        playCount > 300 -> ColorArray.amber
        playCount > 200 -> ColorArray.lightGreen
        playCount > 100 -> ColorArray.green
        playCount > 75 -> ColorArray.blue
        playCount > 50 -> ColorArray.deepBlue
        playCount > 40 -> ColorArray.purple
        playCount > 30 -> ColorArray.magenta
        playCount > 20 -> ColorArray.pink
        playCount > 10 -> ColorArray.red
        else -> ColorArray.deepGray
    }.asReversed()

    private data class TeamConfig(val colors: List<String>, val background: String)

    private fun teamConfig(team: String?): TeamConfig = when (team) {
        "red" -> TeamConfig(ColorArray.red, "card-red.webp")
        "blue" -> TeamConfig(ColorArray.blue, "card-blue.webp")
        else -> TeamConfig(ColorArray.gray, "card-gray.webp")
    }

    private fun playerClassColors(category: String): List<String> {
        return (PLAYER_CLASS_COLORS[category] ?: ColorArray.deepGray).asReversed()
    }

    private fun playerClassBackground(category: String, assets: ImageAssets): String? {
        val grade = PLAYER_CLASS_GRADES[category] ?: "F"
        return assets.local("object-score-backimage-$grade.webp")
    }

    private val PLAYER_CLASS_COLORS = mapOf(
        "BC" to ColorArray.lightYellow,
        "CA" to ColorArray.amber,
        "MF" to ColorArray.green,
        "SP" to ColorArray.lightGreen,
        "WF" to ColorArray.deepBlue,
        "GE" to ColorArray.gray,
        "GU" to ColorArray.blue,
        "SU" to ColorArray.purple,
        "SG" to ColorArray.magenta,
        "NO" to ColorArray.pink,
        "FU" to ColorArray.red,
    )

    private val PLAYER_CLASS_GRADES = mapOf(
        "BC" to "X",
        "CA" to "S",
        "MF" to "A",
        "SP" to "A",
        "WF" to "B",
        "GE" to "XH",
        "GU" to "B",
        "SU" to "C",
        "SG" to "C",
        "NO" to "D",
        "FU" to "D",
    )

    data class MatchName(val name: String, val team1: String, val team2: String)
}
