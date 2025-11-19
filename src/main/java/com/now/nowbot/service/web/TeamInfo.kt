package com.now.nowbot.service.web

import com.fasterxml.jackson.annotation.JsonProperty
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.enums.OsuMode.Companion.getMode
import com.now.nowbot.model.osu.OsuUser
import com.now.nowbot.util.DataUtil
import com.now.nowbot.util.JacksonUtil
import java.util.ArrayList
import java.util.regex.Pattern

data class TeamInfo(
    val id: Int,
    val name: String,
    val abbr: String,
    val formed: String,

    val banner: String,
    val flag: String,

    val users: List<OsuUser>,
    val ruleset: OsuMode,
    val application: String,
    val available: Int,

    val rank: Int,
    val pp: Int,

    @field:JsonProperty("ranked_score")
    val rankedScore: Long,

    @field:JsonProperty("play_count")
    val playCount: Long,

    val members: Int,

    val description: String
)


private val teamFormedPattern: Pattern = Pattern.compile(
    "<time\\s*class=\"js-tooltip-time\"\\s*data-tooltip-position=\"bottom center\"\\s+title=\"(\\S+)\""
)

private val teamUserPattern: Pattern = Pattern.compile("data-user=\"(?<json>.+)\"")

private val teamModePattern: Pattern = Pattern.compile(
    "(?s)<div class=\"team-info-entry__title\">Default ruleset</div>\\s+<div class=\"team-info-entry__value\">\\s+<span class=\"fal fa-extra-mode-(\\w+)\">"
)

// 有点刻晴了
private val teamNamePattern: Pattern = Pattern.compile(
    "<h1 class=\"profile-info__name\">\\s*<span class=\"u-ellipsis-overflow\">\\s*([\\S\\s]+)\\s*</span>\\s*</h1>"
)
private val teamAbbrPattern: Pattern = Pattern.compile(
    "(?s)<p class=\"profile-info__flag\">\\s*\\[(.+)]\\s*</p>"
)
private val teamApplicationPattern: Pattern = Pattern.compile(
    "(?s)application</div>\\s*<div class=\"team-info-entry__value\">\\s*(\\S+)\\s*\\((\\d+).+\\s*</div>"
)

private val rankPattern: Pattern = Pattern.compile(
    "(?s)<div class=\"team-info-entry__value team-info-entry__value--large\">\\s+#([\\d,]+)\\s+</div>"
)

private val ppPattern: Pattern = Pattern.compile(
    "(?s)<div class=\"team-info-entry__title\">\\s+Performance\\s+</div>\\s+<div class=\"team-info-entry__value\">\\s+([\\d,]+)\\s+</div>"
)
private val rankedScorePattern: Pattern = Pattern.compile(
    "(?s)<div class=\"team-info-entry__title\">\\s+Ranked Score\\s+</div>\\s+<div class=\"team-info-entry__value\">\\s+([\\d,]+)\\s+</div>"
)
private val playCountPattern: Pattern = Pattern.compile(
    "(?s)<div class=\"team-info-entry__title\">\\s+Play Count\\s+</div>\\s+<div class=\"team-info-entry__value\">\\s+([\\d,]+)\\s+</div>"
)
private val membersPattern: Pattern = Pattern.compile(
    "(?s)<div class=\"team-info-entry__title\">\\s+Members\\s+</div>\\s+<div class=\"team-info-entry__value\">\\s+([\\d,]+)\\s+</div>"
)

private val teamDescriptionPattern: Pattern = Pattern.compile("<div class='bbcode'>(.+)</div>")

private val teamBannerPattern: Pattern = Pattern.compile("url\\('(https://assets.ppy.sh/teams/header/.+)'\\)")

private val teamFlagPattern: Pattern = Pattern.compile("url\\('(https://assets.ppy.sh/teams/flag/.+)'\\)")

fun parseTeamInfo(id: Int, html: String): TeamInfo {

    val bannerMatcher = teamBannerPattern.matcher(html)
    val banner: String = if (bannerMatcher.find()) {
        bannerMatcher.group(1)
    } else {
        ""
    }

    val nameMatcher = teamNamePattern.matcher(html)
    val name: String = if (nameMatcher.find()) {
        DataUtil.unescapeHTML(nameMatcher.group(1).trim())
    } else {
        ""
    }

    val abbrMatcher = teamAbbrPattern.matcher(html)
    val abbr: String = if (abbrMatcher.find()) {
        DataUtil.unescapeHTML(abbrMatcher.group(1).trim())
    } else {
        ""
    }

    val flagMatcher = teamFlagPattern.matcher(html)
    val flag: String = if (flagMatcher.find()) {
        flagMatcher.group(1)
    } else {
        ""
    }

    val formedMatcher = teamFormedPattern.matcher(html)
    val formed: String = if (formedMatcher.find()) {
        formedMatcher.group(1)
    } else {
        ""
    }

    val modeMatcher = teamModePattern.matcher(html)
    val mode: OsuMode = if (modeMatcher.find()) {
        getMode(modeMatcher.group(1))
    } else {
        OsuMode.DEFAULT
    }

    val application: String
    val available: Int

    val applicationMatcher = teamApplicationPattern.matcher(html)
    if (applicationMatcher.find()) {
        application = applicationMatcher.group(1)
        available = applicationMatcher.group(2)?.toIntOrNull() ?: 0
    } else {
        application = ""
        available = 0
    }

    val rankMatcher = rankPattern.matcher(html)
    val rank: Int = if (rankMatcher.find()) {
        rankMatcher.group(1).replace(",", "").toIntOrNull() ?: 0
    } else {
        0
    }

    val ppMatcher = ppPattern.matcher(html)
    val pp: Int = if (ppMatcher.find()) {
        ppMatcher.group(1).replace(",", "").toIntOrNull() ?: 0
    } else {
        0
    }

    val rankedScoreMatcher = rankedScorePattern.matcher(html)
    val rankedScore: Long = if (rankedScoreMatcher.find()) {
        rankedScoreMatcher.group(1).replace(",", "").toLongOrNull() ?: 0L
    } else {
        0L
    }

    val playCountMatcher = playCountPattern.matcher(html)
    val playCount: Long = if (playCountMatcher.find()) {
        playCountMatcher.group(1).replace(",", "").toLongOrNull() ?: 0L
    } else {
        0L
    }

    val membersMatcher = membersPattern.matcher(html)
    val members: Int = if (membersMatcher.find()) {
        membersMatcher.group(1).replace(",", "").toIntOrNull() ?: 0
    } else {
        0
    }

    val description: String
    val descriptionMatcher = teamDescriptionPattern.matcher(html)
    description = if (descriptionMatcher.find()) {
        descriptionMatcher.group(1)
    } else {
        ""
    }

    val userMatcher = teamUserPattern.matcher(html)
    val users = ArrayList<OsuUser>()

    while (userMatcher.find()) {
        val json = DataUtil.unescapeHTML(userMatcher.group("json"))
        users.add(JacksonUtil.parseObject(json, OsuUser::class.java))
    }

    return TeamInfo(
        id, name, abbr, formed, banner, flag, users, mode, application, available,

        rank, pp, rankedScore, playCount, members,

        description
    )
}