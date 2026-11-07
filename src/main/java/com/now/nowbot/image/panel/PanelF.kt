package com.now.nowbot.image.panel

import com.now.nowbot.image.card.CardA2
import com.now.nowbot.image.card.CardF
import com.now.nowbot.image.card.CardFData
import com.now.nowbot.image.card.MatchPlayerScore
import com.now.nowbot.image.draw.ImageAssets
import com.now.nowbot.image.draw.Templates
import com.now.nowbot.image.draw.mapBackground
import com.now.nowbot.image.scorelist.ScoreListGenerate
import com.now.nowbot.image.svg.Svg
import com.now.nowbot.image.util.TimeFormat
import com.now.nowbot.model.match.Match.MatchRound
import com.now.nowbot.model.match.MatchRating
import com.now.nowbot.model.osu.LazerScore

object PanelF {
    private const val MARK_INDEX = "<g id=\"Index\">"
    private const val MARK_BANNER = "<g style=\"clip-path: url(#clippath-PF-1);\">"
    private const val MARK_CARD_C = "<g id=\"CardC\">"
    private const val MARK_CARD_A2 = "<g id=\"CardA2\">"
    private const val MARK_MAIN = "<g id=\"MainCard\">"

    fun render(rating: MatchRating, quickplay: Boolean, assets: ImageAssets): String {
        var svg = Templates.read("Panel_F.svg")
        val stat = rating.match.statistics
        val requestTime = "match time: ${matchDuration(stat.startTime, stat.endTime)} // request time: ${TimeFormat.nowTimestamp()}"
        val name = if (quickplay) {
            PanelBatchSupport.panelName("(Quick) Ranked Play (!ymrp)", "RP", requestTime)
        } else {
            PanelBatchSupport.panelName("Match Now (!ymmn)", "MN", requestTime)
        }
        svg = Svg.setText(svg, name, MARK_INDEX)
        svg = Svg.setText(svg, PanelBatchSupport.randomBanner(assets)?.let {
            Svg.image(0.0, 0.0, 1920.0, 320.0, it, 0.7)
        }, MARK_BANNER)
        val skip = rating.skipIgnoreMap["skip"]?.toInt() ?: 0
        val ignore = rating.skipIgnoreMap["ignore"]?.toInt() ?: 0
        val allRounds = rating.rounds.filter { it.scores.isNotEmpty() }
        val rounds = allRounds.drop(skip).dropLast(ignore)
        val cardsF = rounds.mapNotNull { round -> cardFor(rating, round, assets) }
        val cardsA2 = rounds.mapNotNull { round -> round.beatmap?.let { CardA2.render(CardA2.fromMatchBeatmap(it, assets), assets) } }
        val bodyF = cardsF.mapIndexed { index, card -> Svg.group(510.0, 330.0 + index * 250.0, card) }.joinToString("\n")
        val bodyA2 = cardsA2.mapIndexed { index, card -> Svg.group(40.0, 330.0 + index * 250.0, card) }.joinToString("\n")
        svg = Svg.setText(svg, bodyF, MARK_CARD_C)
        svg = Svg.setText(svg, bodyA2, MARK_CARD_A2)
        val firstBeatmap = rounds.firstOrNull()?.beatmap
        val info = ScoreListGenerate.matchRating2CardA2(rating, assets, isMatchStart = false)
        svg = Svg.setText(svg, Svg.group(40.0, 40.0, CardA2.render(info, assets)), MARK_MAIN)
        val panelHeight = PanelBatchSupport.panelHeight(rounds.size, 210.0, 1, 290.0, 40.0, 40.0)
        return PanelBatchSupport.applyHeights(svg, panelHeight, panelHeight - 290.0)
    }

    private fun cardFor(rating: MatchRating, round: MatchRound, assets: ImageAssets): String? {
        val red = mutableListOf<MatchPlayerScore>()
        val blue = mutableListOf<MatchPlayerScore>()
        val none = mutableListOf<MatchPlayerScore>()
        round.scores.forEach { score ->
            val player = player(score, assets)
            when (score.playerStat?.team) {
                "red" -> red += player
                "blue" -> blue += player
                else -> none += player
            }
        }
        val index = rating.rounds.indexOf(round)
        val redWinsBefore = rating.rounds.take(index + 1).count { it.winningTeam == "red" }
        val blueWinsBefore = rating.rounds.take(index + 1).count { it.winningTeam == "blue" }
        val data = CardFData(
            isTeamVs = round.isTeamVS,
            redWin = round.winningTeam == "red",
            blueWin = round.winningTeam == "blue",
            redScore = round.redTeamScore,
            blueScore = round.blueTeamScore,
            totalScore = round.totalTeamScore,
            redWinsBefore = redWinsBefore,
            blueWinsBefore = blueWinsBefore,
            red = red,
            blue = blue,
            none = none,
        )
        return CardF.render(data, assets)
    }

    private fun player(score: LazerScore, assets: ImageAssets): MatchPlayerScore {
        val user = score.user
        return MatchPlayerScore(
            name = user.username,
            avatar = assets.remote(user.avatarUrl) ?: assets.remote("https://a.ppy.sh/${user.userID}"),
            score = score.legacyScore?.takeIf { it > 0 } ?: score.score,
            mods = score.mods.map { it.acronym },
            rank = score.ranking ?: 0,
        )
    }

    private fun matchDuration(start: java.time.OffsetDateTime, end: java.time.OffsetDateTime?): String {
        val s = start.atZoneSameInstant(java.time.ZoneOffset.ofHours(8))
        if (end == null) return s.format(java.time.format.DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm")) + " - in progress"
        val e = end.atZoneSameInstant(java.time.ZoneOffset.ofHours(8))
        return s.format(java.time.format.DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm")) + " - " +
            e.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")) + " (" + TimeFormat.difference(end, 0) + ")"
    }
}
