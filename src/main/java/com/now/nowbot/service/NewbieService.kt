package com.now.nowbot.service

import com.now.nowbot.dao.OsuUserInfoDao
import com.now.nowbot.entity.LazerScoreLite
import com.now.nowbot.entity.NewbiePlayCount
import com.now.nowbot.mapper.LazerScoreRepository
import com.now.nowbot.mapper.LazerScoreStatisticRepository
import com.now.nowbot.mapper.NewbiePlayCountRepository
import com.now.nowbot.model.LazerMod
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.json.LazerStatistics
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.service.osuApiService.OsuCalculateApiService
import com.now.nowbot.service.osuApiService.OsuUserApiService
import com.now.nowbot.util.JacksonUtil
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.ZonedDateTime

@Service("NEWBIE_SERVICE")
class NewbieService(
    private val osuUserInfoDao: OsuUserInfoDao,
    private val scoreRepository: LazerScoreRepository,
    private val scoreStatisticRepository: LazerScoreStatisticRepository,
    private val beatmapApiService: OsuBeatmapApiService,
    private val calculateApiService: OsuCalculateApiService,
    private val osuUserApiService: OsuUserApiService,
    private val newbiePlayCountRepository: NewbiePlayCountRepository,
) {

    val allowedMods = setOf(
        LazerMod.Easy::class,
        LazerMod.NoFail::class,
        LazerMod.HalfTime::class,
        LazerMod.HardRock::class,
        LazerMod.SuddenDeath::class,
        LazerMod.Perfect::class,
        LazerMod.DoubleTime::class,
        LazerMod.Nightcore::class,
        LazerMod.Hidden::class,
        LazerMod.Flashlight::class,
        LazerMod.SpunOut::class,
        LazerMod.TouchDevice::class,
    )

    fun getStarRating(bid: Long, modsString: String): Float {
        if (modsString == "[]") {
            return beatmapApiService.getBeatMapFromDataBase(bid).starRating.toFloat()
        }
        val mods = JacksonUtil.parseObjectList(modsString, LazerMod::class.java)
        if (mods.any { !allowedMods.contains(it::class) || it.settings != null }) {
            return -1f
        }

        val start = calculateApiService.getStar(bid, OsuMode.OSU, mods).toFloat()
        return start
    }

    fun getDailyStatistic(userId: Long, date: LocalDate): ScoreDailyStatistic {
        val start = ZonedDateTime
            .of(date, LocalTime.of(0, 0), ZoneOffset.systemDefault())
            .toOffsetDateTime()
        val end = start.plusDays(1)

        val scores = scoreRepository.getUserRankedScore(userId, OsuMode.OSU.modeValue, start, end)
        scores.filter {
            val star = getStarRating(it.beatmapId, it.mods ?: "[]")
            star in 0f..5.7f
        }

        val statisticsMap = scoreStatisticRepository
            .getByScoreId(scores.map { it.id })
            .associate { it.id to JacksonUtil.parseObject(it.data, LazerStatistics::class.java) }

        val (passScore, failedScore) = scores.partition { it.passed }

        // this.sliderTailHit is tth
        val passStatistics = passScore.toStatistics(statisticsMap)

        val failedStatistics = failedScore.toStatistics(statisticsMap)

        val tth = passStatistics
            .sumOf { it.second.sliderTailHit } + failedStatistics.sumOf { it.second.sliderTailHit }

        val pc = passStatistics.size + failedStatistics.size
        /*
                val fileTimeAll =
                    beatmapApiService.getAllFailTime(failedStatistics.map { it.first.beatmapId to it.second.sliderTailHit })
                val passTimeMap = beatmapApiService
                    .getAllBeatmapHitLength(passStatistics.map { it.first.beatmapId }.toSet())
                    .toMap()

                val pt = passScore.sumOf { passTimeMap[it.beatmapId] ?: 0 } + fileTimeAll.sum()
         */
        return ScoreDailyStatistic(userId, date, pc, tth)
    }

    fun getPPStatistic(userId: Long): Pair<String, Float> {
        val yesterday = LocalDate.now().minusDays(1)
        val yesterdayUserInfoOpt = osuUserInfoDao.getLastFrom(userId, OsuMode.OSU, yesterday)
        if (yesterdayUserInfoOpt.isEmpty) {
            return "" to 0f
        }
        val yesterdayUserInfo = yesterdayUserInfoOpt.get()
        val nowUserInfo = osuUserApiService.getPlayerInfo(userId, OsuMode.OSU)
        return nowUserInfo.username to (nowUserInfo.pp - yesterdayUserInfo.pp).toFloat()
    }

    fun getToday(userId: Long): ScoreDailyStatistic {
        return getDailyStatistic(userId, LocalDate.now()).also {
            val (name, pp) = getPPStatistic(userId)
            it.name = name
            it.pp = pp
        }
    }

    fun dailyTask(users: List<Long>) {
        users.chunked(50) { usersId ->
            for (uid in usersId) {
                val dailyStatistic = getDailyStatistic(uid, LocalDate.now().minusDays(1))
                val pp = osuUserInfoDao.getLast(uid, OsuMode.OSU).map { it.pp }.orElse(0.0).toFloat()
                val saveData = NewbiePlayCount(
                    uid = uid.toInt(),
                    pp = pp,
                    date = dailyStatistic.date,
                    playTime = 0,
                    playCount = dailyStatistic.playCount,
                    playHits = dailyStatistic.totalHit,
                )
                newbiePlayCountRepository.save(saveData)
            }
        }
    }


    fun List<LazerScoreLite>.toStatistics(statisticsMap: Map<Long, LazerStatistics>): List<Pair<LazerScoreLite, LazerStatistics>> {
        return mapNotNull {
            val stat = statisticsMap[it.id]?.apply { this.sliderTailHit = getTotalHits(OsuMode.OSU) }
            if (stat != null) {
                it to stat
            } else {
                null
            }
        }.filter { it.second.sliderTailHit > 30 }
    }

    data class ScoreDailyStatistic(
        val userId: Long,
        val date: LocalDate,
        val playCount: Int,
        val totalHit: Int,
        var name: String? = null,
        var pp: Float? = null,
    )
}