package com.now.nowbot.dao

import com.now.nowbot.entity.OsuUserInfoArchiveLite
import com.now.nowbot.entity.OsuUserInfoArchiveLite.InfoArchive
import com.now.nowbot.mapper.OsuUserInfoRepository
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.osu.InfoLogStatistics
import com.now.nowbot.model.osu.MicroUser
import com.now.nowbot.model.osu.OsuUser
import com.now.nowbot.model.osu.Statistics
import com.now.nowbot.util.JacksonUtil
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Component
class OsuUserInfoDao(private val infoRepository: OsuUserInfoRepository) {

    fun saveUserToday(user: OsuUser, mode: OsuMode) {
        val now = LocalDateTime.now()
        val today = LocalDate.now()

        val last = getLastToday(user.userID, mode, today)

        if (last != null && user.playCount > 0) {

            // 今天内已经有数据，但是最新的数据发生了变化
            if (user.playCount != last.playCount) {
                infoRepository.removeBetween(user.userID, mode, LocalDateTime.of(today, LocalTime.MIN), now)
            } else {
                return
            }
        }

        val lite = fromModel(user, mode)
        infoRepository.save(lite)
    }

    fun saveUsersToday(users: List<MicroUser>) {
        val now = LocalDateTime.now().toLocalDate()

        users.flatMap {
            if (it.rulesets == null) return@flatMap emptyList<OsuUserInfoArchiveLite>()

            val oc = infoRepository.getLastCountryRank(it.userID, OsuMode.OSU)
            val tc = infoRepository.getLastCountryRank(it.userID, OsuMode.TAIKO)
            val cc = infoRepository.getLastCountryRank(it.userID, OsuMode.CATCH)
            val mc = infoRepository.getLastCountryRank(it.userID, OsuMode.MANIA)

            val osu = fromStatistics(it.rulesets!!.osu, OsuMode.OSU, oc)
            if (osu != null) osu.userID = it.userID

            val taiko = fromStatistics(it.rulesets!!.taiko, OsuMode.TAIKO, tc)
            if (taiko != null) taiko.userID = it.userID

            val fruits = fromStatistics(it.rulesets!!.fruits, OsuMode.CATCH, cc)
            if (fruits != null) fruits.userID = it.userID

            val mania = fromStatistics(it.rulesets!!.mania, OsuMode.MANIA, mc)
            if (mania != null) mania.userID = it.userID

            return@flatMap listOf(osu, taiko, fruits, mania)
            }
            .filterNotNull()
            .forEach { user ->
                val today = getLastToday(user.userID, user.mode, now)

                if (today != null && user.playCount > 0 && today.playCount == user.playCount) {
                    return
                }

                infoRepository.save(user)
            }
    }

    /**
     * 取这一天最后的数据
     *
     * @param date 这一天，不输入就是今天
     * @return 这一天最后的数据
     */
    private fun getLastToday(userID: Long, mode: OsuMode, date: LocalDate = LocalDate.now()): OsuUserInfoArchiveLite? {
        return infoRepository.getLastBetween(userID, mode, LocalDateTime.of(date, LocalTime.MIN), LocalDateTime.of(date, LocalTime.MAX))
    }

    /**
     * 取 到那一天为止 最后的数据 (默认向前取一年)
     *
     * @param date 那一天
     */
    fun getLastFrom(userID: Long, mode: OsuMode, date: LocalDate): OsuUserInfoArchiveLite? {
        val time = LocalDateTime.of(date, LocalTime.MAX)
        return infoRepository.getLastBetween(userID, mode, time.minusYears(1), time)
    }

    fun getLast(userID: Long, mode: OsuMode): OsuUserInfoArchiveLite? {
        return infoRepository.getLast(userID, mode)
    }

    fun getFromYesterday(userIDs: List<Long>): List<InfoArchive> {
        return getFromUserIDsYesterday(userIDs)
    }

    private fun getFromUserIDsYesterday(userIDs: List<Long>): List<InfoArchive> {
        val time = LocalDate.now().minusDays(1)
        return infoRepository.getFromUserIDs(userIDs, LocalDateTime.of(time, LocalTime.MIN), LocalDateTime.of(time, LocalTime.MAX))
    }

    companion object {
        fun fromArchive(archive: OsuUserInfoArchiveLite): OsuUser {
            val user = OsuUser()
            user.mode = archive.mode.shortName
            user.id = archive.userID

            val statistics = InfoLogStatistics()
            statistics.countA = archive.countA
            statistics.countS = archive.countS
            statistics.countSS = archive.countSS
            statistics.countSH = archive.countSH
            statistics.countSSH = archive.countSSH

            statistics.globalRank = archive.globalRank
            statistics.countryRank = archive.countryRank
            statistics.totalScore = archive.totalScore
            statistics.totalHits = archive.totalHits
            statistics.rankedScore = archive.rankedScore
            statistics.accuracy = archive.accuracy
            statistics.playCount = archive.playCount
            statistics.playTime = archive.playTime
            statistics.levelCurrent = archive.levelCurrent
            statistics.levelProgress = archive.levelProgress
            statistics.maxCombo = archive.maximumCombo
            statistics.pp = archive.pp

            statistics.logTime = archive.time

            user.statistics = statistics
            archive.rankHistory?.let {
                user.rankHistory = OsuUser.RankHistory(archive.mode.shortName,
                    JacksonUtil.parseObjectList(archive.rankHistory, Int::class.java) ?: listOf())
            }

            return user
        }

        fun fromModel(data: OsuUser, mode: OsuMode): OsuUserInfoArchiveLite {
            val archive = OsuUserInfoArchiveLite()

            archive.userID = data.userID
            archive.setLiteStatistics(data.statistics)

            archive.playCount = data.playCount
            archive.playTime = data.playTime
            data.rankHistory?.let { archive.rankHistory = it.data.toString() }
            archive.beatmapPlaycount = data.beatmapPlaycount

            // 过滤掉非法的游戏模式
            if (mode.isDefault()) {
                archive.mode = OsuMode.getMode(data.currentOsuMode.modeValue % 4)
            } else {
                archive.mode = OsuMode.getMode(mode.modeValue % 4)
            }

            archive.time = LocalDateTime.now()
            return archive
        }

        private fun OsuUserInfoArchiveLite.setLiteStatistics(statistics: Statistics?) {
            if (statistics == null) return

            this.globalRank = statistics.globalRank
            this.countryRank = statistics.countryRank
            this.totalScore = statistics.totalScore ?: 0
            this.rankedScore = statistics.rankedScore ?: 0
            this.countA = statistics.countA
            this.countS = statistics.countS
            this.countSH = statistics.countSH
            this.countSS = statistics.countSS
            this.countSSH = statistics.countSSH

            this.accuracy = statistics.accuracy ?: 0.0
            this.pp = statistics.pp ?: 0.0
            this.levelCurrent = statistics.levelCurrent
            this.levelProgress = statistics.levelProgress
            this.isRanked = statistics.ranked ?: false
            this.maximumCombo = statistics.maxCombo
            this.totalHits = statistics.totalHits ?: 0
            this.playCount = statistics.playCount ?: 0
            this.playTime = statistics.playTime ?: 0
        }

        private fun fromStatistics(statistics: Statistics?, mode: OsuMode, countryRank: Long? = null): OsuUserInfoArchiveLite? {
            if (statistics == null) return null

            return OsuUserInfoArchiveLite().apply {
                this.mode = mode
                this.time = LocalDateTime.now()
                this.setLiteStatistics(statistics)

                countryRank?.let { this.countryRank = countryRank }
            }
        }
    }
}
