package com.now.nowbot.model

import com.now.nowbot.model.json.BeatMap
import com.now.nowbot.model.json.LazerScore
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.throwable.GeneralTipsException
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.text.NumberFormat
import java.util.*
import kotlin.math.floor

class UUScore (score: LazerScore, osuBeatmapApiService: OsuBeatmapApiService) {
    var name: String
    var mode: String
    var country: String
    var map_length: Int
    var map_name: String? = ""
    var difficulty_name: String
    var artist: String? = ""
    var star_rating: Float
    var star_str: String
    var rank: String
    var mods: Array<String?>
    var score: Int
    var acc: Double
    var pp: Double
    var max_combo: Int
    var combo: Int
    var bid: Int
    
    var n_300: Int
    var n_100: Int
    var n_50: Int
    var n_geki: Int
    var n_katu: Int
    var n_0: Int
    var passed: Boolean
    var url: String? = ""
    var key: Int
    var play_time: String

    init {
        val user = score.user
        bid = Math.toIntExact(score.beatMap.beatMapID)
        
        val b: BeatMap
        
        try {
            b = osuBeatmapApiService.getBeatMapFromDataBase(bid)
        }catch (e: HttpClientErrorException.Unauthorized) {
            throw GeneralTipsException(GeneralTipsException.Type.G_TokenExpired_Me)
        }catch (e: WebClientResponseException.Unauthorized) {
            throw GeneralTipsException(GeneralTipsException.Type.G_TokenExpired_Me)
        }
        
        val s = b.beatMapSet
        val modsList = score.mods
        
        name = user.userName
        mode = score.mode.getName()
        country = user.countryCode
        mods = arrayOfNulls(modsList.size)
        for (i in mods.indices) {
            mods[i] = modsList[i].type.acronym
        }
        if (s != null) {
            map_name = s.titleUnicode
            artist = s.artistUnicode
            url = s.covers.card
        }
        max_combo = b.maxCombo
        difficulty_name = b.difficultyName
        
        star_rating = b.starRating
        map_length = b.totalLength
        
        val sr_floor = floor(star_rating.toDouble()).toInt()
        val sr_str = StringBuilder()
        var i = 0
        while (i < sr_floor && i < 10) {
            sr_str.append('★')
            i++}
        if (0.5 < (star_rating - sr_floor) && sr_floor < 10) {
            sr_str.append('☆')
        }
        star_str = sr_str.toString()
        
        rank = score.rank
        this.score = score.score.toInt()
        acc = ((Math.round(score.accuracy * 10000)) / 100.0).toFloat().toDouble()
        
        pp = if (Objects.nonNull(score.PP)) score.PP!! else 0.0
        
        combo = score.maxCombo
        passed = score.passed
        key = score.beatMap.cs.toInt()
        play_time = score.endedTimeString
        
        val stat = score.statistics
        n_300 = stat.great ?: 0
        n_100 = stat.ok ?: 0
        n_50 = stat.meh ?: 0
        n_geki = stat.perfect ?: 0
        n_katu = stat.good ?: 0
        n_0 = stat.miss ?: 0
        
        if (!passed) rank = "F"
    }

    val scoreLegacyOutput: String
        get() {
            val sb = StringBuilder()

            //  "username" ("country_code"): "mode" ("key"K)-if needed
            sb.append(name).append(' ').append('(').append(country).append(')').append(':').append(' ').append(mode)

            if (mode == "mania") {
                difficulty_name = difficulty_name.replace("^\\[\\d{1,2}K]\\s*".toRegex(), "")
                sb.append(' ').append('(').append(key).append("K").append(')').append('\n')
            } else {
                sb.append('\n')
            }

            //  "artist_unicode" - "title_unicode" ["version"]
            sb.append(artist).append(" - ").append(map_name).append(' ').append('[').append(difficulty_name).append(']')
                .append('\n')

            //  ★★★★★ "difficulty_rating"* mm:ss
            sb.append(star_str).append(' ').append(format(star_rating.toDouble())).append('*')
                .append(' ')
                .append(map_length / 60).append(':').append(String.format("%02d", map_length % 60)).append('\n')

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
            sb.append(combo).append('x').append(' ').append('/').append(' ').append(max_combo).append('x')
                .append(' ').append('/').append('/').append(' ').append(format(acc)).append('%')
                .append('\n')

            //  "count_300" /  "count_100" / "count_50" / "count_miss"
            when (mode) {
                "taiko" -> sb.append(n_300).append(" / ").append(n_100).append(" / ").append(n_0).append('\n')
                    .append('\n')

                "mania" -> {
                    sb.append(n_300).append('+').append(n_geki).append('(')
                    if (n_300 >= n_geki && n_geki != 0) {
                        sb.append(String.format("%.2f", (1f * n_geki / n_300)))
                    } else if (n_300 < n_geki && n_300 != 0) {
                        sb.append(String.format("%.1f", (1f * n_geki / n_300)))
                    } else {
                        sb.append('-')
                    }
                    sb.append(')').append(" / ").append(n_katu).append(" / ").append(n_100).append(" / ").append(n_50)
                        .append(" / ").append(n_0).append('\n').append('\n')
                }

                "catch", "fruits" -> sb.append(n_300).append(" / ").append(n_100).append(" / ").append(n_50)
                    .append(" / ").append(n_0).append('(').append('-').append(n_katu).append(')').append('\n')
                    .append('\n')

                else -> sb.append(n_300).append(" / ").append(n_100).append(" / ").append(n_50).append(" / ")
                    .append(n_0).append('\n').append('\n')
            }

            //DateTimeFormatter.ISO_ZONED_DATE_TIME.parse(play_time) 格式化 ISO-8601 日期格式
            sb.append("bid: ").append(bid)
            return sb.toString()
        }

    companion object {
        @Throws(GeneralTipsException::class) fun getInstance(score: LazerScore, beatmapApiService: OsuBeatmapApiService): UUScore {
            return UUScore(score, beatmapApiService)
        }
        
        fun format(d: Double): String {
            val x = Math.round(d * 100) / 100.0
            val nf = NumberFormat.getInstance()
            return nf.format(x)
        }
    }}
