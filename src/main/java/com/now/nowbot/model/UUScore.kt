package com.now.nowbot.model

import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.json.LazerScore
import com.now.nowbot.model.json.LazerStatistics
import com.now.nowbot.service.osuApiService.OsuCalculateApiService
import kotlin.math.floor
import kotlin.math.round

class UUScore(score: LazerScore, calculateApiService: OsuCalculateApiService) {
    var name: String
    var mode: OsuMode
    var country: String
    var totalLength: Int
    var titleUnicode: String? = ""
    var difficultyName: String
    var artist: String? = ""
    var starRating: Double
    var starStr: String
    var rank: String
    var mods: Array<String?>
    var score: Int
    var acc: Double
    var pp: Double
    var maxCombo: Int
    var combo: Int
    var bid: Long
    
    var statistics: LazerStatistics = score.statistics
    var passed: Boolean
    var url: String? = ""
    var key: Int
    var playTime: String

    init {
        val user = score.user
        bid = score.beatMap.beatMapID

        calculateApiService.applyPPToScore(score)
        calculateApiService.applyBeatMapChanges(score)
        calculateApiService.applyStarToScore(score)

        val modsList = score.mods
        
        name = user.userName
        mode = score.mode
        country = user.countryCode
        mods = arrayOfNulls(modsList.size)
        for (i in mods.indices) {
            mods[i] = modsList[i].acronym
        }

        titleUnicode = score.beatMapSet.titleUnicode
        artist = score.beatMapSet.artistUnicode
        url = score.beatMapSet.covers.card

        maxCombo = score.beatMap.maxCombo!!
        difficultyName = score.beatMap.difficultyName
        
        starRating = score.beatMap.starRating
        totalLength = score.beatMap.totalLength
        
        val starInteger = floor(starRating).toInt()
        val srStr = StringBuilder()

        var i = 0
        while (i < starInteger && i < 10) {
            srStr.append('★')
            i++
        }

        if (0.5 < (starRating - starInteger) && starInteger < 10) {
            srStr.append('☆')
        }

        starStr = srStr.toString()
        
        rank = score.rank
        this.score = score.score.toInt()
        acc = ((round(score.accuracy * 10000.0)) / 100.0)
        
        pp = score.PP ?: 0.0
        
        combo = score.maxCombo
        passed = score.passed
        key = score.beatMap.CS!!.toInt()
        playTime = score.endedTimeString

        statistics = score.statistics
        
        if (!passed) rank = "F"
    }

    val scoreLegacyOutput: String
        get() {
            val sb = StringBuilder()

            //  "username" ("country_code"): "mode" ("key"K)-if needed
            sb.append(name).append(' ').append('(').append(country).append(')').append(':').append(' ').append(mode)

            if (mode == OsuMode.MANIA) {
                difficultyName = difficultyName.replace("^\\[\\d{1,2}K]\\s*".toRegex(), "")
                sb.append(' ').append('(').append(key).append("K").append(')').append('\n')
            } else {
                sb.append('\n')
            }

            //  "artist_unicode" - "title_unicode" ["version"]
            sb.append(artist).append(" - ").append(titleUnicode).append(' ').append('[').append(difficultyName).append(']')
                .append('\n')

            //  ★★★★★ "difficulty_rating"* mm:ss
            sb.append(starStr).append(' ').append(format(starRating)).append('*')
                .append(' ')
                .append(totalLength / 60).append(':').append(String.format("%02d", totalLength % 60)).append('\n')

            //  ["rank"] +"mods" "score" "pp"(###)PP
            sb.append('[').append(rank).append(']').append(' ')

            if (mods.isNotEmpty()) {
                sb.append('+')
                for (mod in mods) {
                    sb.append(mod).append(' ')
                }
            }

            sb.append(score.toString().replace("(?<=\\d)(?=(?:\\d{4})+$)".toRegex(), "'")).append(' ').append('(')
                .append(format(pp)).append("PP").append(')').append('\n')

            //  "max_combo"/###x "accuracy"%
            sb.append(combo).append('x').append(' ').append('/').append(' ').append(maxCombo).append('x')
                .append(' ').append('/').append('/').append(' ').append(format(acc)).append('%')
                .append('\n')

            //  "count_300" /  "count_100" / "count_50" / "count_miss"
            when (mode) {
                OsuMode.TAIKO -> sb.append(statistics.great).append(" / ").append(statistics.ok).append(" / ").append(statistics.miss).append('\n')
                    .append('\n')

                OsuMode.MANIA -> {
                    sb.append(statistics.great).append('+').append(statistics.perfect).append('(')
                    if (statistics.great >= statistics.perfect && statistics.great != 0) {
                        sb.append(String.format("%.2f", (1.0 * statistics.perfect / statistics.great)))
                    } else if (statistics.great < statistics.perfect && statistics.great != 0) {
                        sb.append(String.format("%.1f", (1.0 * statistics.perfect / statistics.great)))
                    } else {
                        sb.append('-')
                    }
                    sb.append(')').append(" / ")
                        .append(statistics.good).append(" / ")
                        .append(statistics.ok).append(" / ")
                        .append(statistics.meh).append(" / ")
                        .append(statistics.miss)
                        .append('\n').append('\n')
                }

                OsuMode.CATCH -> sb.append(statistics.great).append(" / ")
                    .append(statistics.largeTickHit).append(" / ")
                    .append(statistics.smallTickHit).append(" / ")
                    .append(statistics.miss).append('(').append('-').append(statistics.smallTickMiss).append(')')
                    .append('\n').append('\n')

                else -> sb.append(statistics.great).append(" / ")
                    .append(statistics.ok).append(" / ")
                    .append(statistics.meh).append(" / ")
                    .append(statistics.miss).append('\n').append('\n')
            }

            //DateTimeFormatter.ISO_ZONED_DATE_TIME.parse(play_time) 格式化 ISO-8601 日期格式
            sb.append("bid: ").append(bid)
            return sb.toString()
        }

    companion object {
        fun format(d: Double): String {
            return String.format("%.2f", d)
        }
    }}
