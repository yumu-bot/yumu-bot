package com.now.nowbot.dao

import com.now.nowbot.entity.OsuUserInfoArchiveLite
import com.now.nowbot.entity.OsuUserInfoArchiveLite.InfoArchive
import com.now.nowbot.mapper.OsuUserInfoMapper
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.json.InfoLogStatistics
import com.now.nowbot.model.json.MicroUser
import com.now.nowbot.model.json.OsuUser
import com.now.nowbot.model.json.Statistics
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.*

@Component
class OsuUserInfoDao(private val osuUserInfoMapper: OsuUserInfoMapper) {

    fun saveUser(user: OsuUser, mode: OsuMode) {
        val u = fromModel(user, mode)
        osuUserInfoMapper.save(u)
    }

    fun saveUsers(users: List<MicroUser>) {
        val all = users.flatMap {
            if (it.rulesets == null) return@flatMap emptyList<OsuUserInfoArchiveLite>()

            val osu = fromStatistics(it.rulesets!!.osu, OsuMode.OSU)
            if (osu != null) osu.osuID = it.userID

            val taiko = fromStatistics(it.rulesets!!.taiko, OsuMode.TAIKO)
            if (taiko != null) taiko.osuID = it.userID

            val fruits = fromStatistics(it.rulesets!!.fruits, OsuMode.CATCH)
            if (fruits != null) fruits.osuID = it.userID

            val mania = fromStatistics(it.rulesets!!.mania, OsuMode.MANIA)
            if (mania != null) mania.osuID = it.userID

            return@flatMap listOf(osu, taiko, fruits, mania)
            }
            .filterNotNull()
            .toList()

        osuUserInfoMapper.saveAllAndFlush(all)
    }

    /**
     * 取那一天最后的数据
     *
     * @param date 当天
     * @return 那一天最后的数据
     */
    fun getLast(uid: Long?, mode: OsuMode?, date: LocalDate?): Optional<OsuUserInfoArchiveLite> {
        return osuUserInfoMapper.selectDayLast(uid, mode, date)
    }

    /**
     * 取 到那一天为止 最后的数据 (默认向前取一年)
     *
     * @param date 那一天
     */
    fun getLastFrom(uid: Long?, mode: OsuMode?, date: LocalDate): Optional<OsuUserInfoArchiveLite> {
        val time = LocalDateTime.of(date, LocalTime.MAX)
        return osuUserInfoMapper.selectDayLast(uid, mode, time.minusYears(1), time)
    }

    fun getLast(uid: Long?, mode: OsuMode?): Optional<OsuUserInfoArchiveLite> {
        return osuUserInfoMapper.selectLast(uid, mode)
    }

    fun getYesterdayInfo(uid: List<Long?>?): List<InfoArchive> {
        return osuUserInfoMapper.getArchiveByUidYesterday(uid)
    }

    companion object {
        fun fromArchive(archive: OsuUserInfoArchiveLite): OsuUser {
            val user = OsuUser()
            user.mode = archive.mode.shortName
            user.id = archive.osuID

            val statistics = InfoLogStatistics()
            statistics.countA = archive.grade_counts_a
            statistics.countS = archive.grade_counts_s
            statistics.countSS = archive.grade_counts_ss
            statistics.countSH = archive.grade_counts_sh
            statistics.countSSH = archive.grade_counts_ssh

            statistics.globalRank = archive.global_rank
            statistics.countryRank = archive.country_rank
            statistics.totalScore = archive.total_score
            statistics.totalHits = archive.total_hits
            statistics.rankedScore = archive.ranked_score
            statistics.accuracy = archive.hit_accuracy
            statistics.playCount = archive.play_count
            statistics.playTime = archive.play_time
            statistics.levelCurrent = archive.level_current
            statistics.levelProgress = archive.level_progress
            statistics.maxCombo = archive.maximum_combo
            statistics.pp = archive.pp

            statistics.logTime = archive.time

            user.statistics = statistics
            return user
        }

        fun fromModel(data: OsuUser, mode: OsuMode): OsuUserInfoArchiveLite {
            val archive = OsuUserInfoArchiveLite()

            archive.osuID = data.userID
            setOut(archive, data.statistics!!)

            archive.play_count = data.playCount
            archive.play_time = data.playTime

            if (mode.isDefault()) {
                archive.mode = data.currentOsuMode
            } else {
                archive.mode = mode
            }
            archive.time = LocalDateTime.now()
            return archive
        }

        private fun setOut(out: OsuUserInfoArchiveLite, statistics: Statistics) {
            out.global_rank = statistics.globalRank
            out.country_rank = statistics.countryRank
            out.total_score = statistics.totalScore
            out.ranked_score = statistics.rankedScore
            out.grade_counts_a = statistics.countA
            out.grade_counts_s = statistics.countS
            out.grade_counts_sh = statistics.countSH
            out.grade_counts_ss = statistics.countSS
            out.grade_counts_ssh = statistics.countSSH

            out.hit_accuracy = statistics.accuracy
            out.pp = statistics.pp
            out.level_current = statistics.levelCurrent
            out.level_progress = statistics.levelProgress
            out.is_ranked = statistics.ranked
            out.maximum_combo = statistics.maxCombo
            out.total_hits = statistics.totalHits
        }

        private fun fromStatistics(s: Statistics?, mode: OsuMode): OsuUserInfoArchiveLite? {
            if (s == null) return null
            val out = OsuUserInfoArchiveLite()
            out.play_count = s.playCount
            out.play_time = s.playTime
            out.mode = mode
            out.time = LocalDateTime.now()
            setOut(out, s)

            return out
        }
    }
}
