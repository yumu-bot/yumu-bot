package com.now.nowbot.service.repairServiceImpl

import com.now.nowbot.aop.CheckPermission
import com.now.nowbot.config.Permission
import com.now.nowbot.dao.OsuUserInfoDao
import com.now.nowbot.entity.OsuUserInfoArchiveLite
import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.entity.UserGlobalRankLite
import com.now.nowbot.entity.UserInfoLite
import com.now.nowbot.entity.UserInfoLite.Companion.updateFrom
import com.now.nowbot.entity.UserRankPercentLite
import com.now.nowbot.entity.UserRankPercentLite.Companion.updateFrom
import com.now.nowbot.entity.UserStatisticsLite
import com.now.nowbot.entity.UserStatisticsLite.Companion.updateFrom
import com.now.nowbot.mapper.OsuUserInfoRepository
import com.now.nowbot.mapper.UserGlobalRankRepository
import com.now.nowbot.mapper.UserInfoRepository
import com.now.nowbot.mapper.UserRankPercentRepository
import com.now.nowbot.mapper.UserStatisticsRepository
import com.now.nowbot.model.calculate.InfoLogStatistics
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.osu.OsuUser
import com.now.nowbot.model.osu.Statistics
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.messageServiceImpl.TestService
import com.now.nowbot.util.JacksonUtil
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

@Service
@CheckPermission(isSuperAdmin = true)
class InfoUpgradeService(
    private val userInfoDao: OsuUserInfoDao,
    private val oldRepo: OsuUserInfoRepository,

    private val userRankPercentRepository: UserRankPercentRepository,
    private val userInfoRepository: UserInfoRepository,
    private val userStatisticsRepository: UserStatisticsRepository,
    private val userGlobalRankRepository: UserGlobalRankRepository,
) : MessageService<Boolean> {
    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: MessageService.DataValue<Boolean>
    ): Boolean {

        val fix = "!" + "iu"

        if (messageText.contains(fix) && Permission.isSuperAdmin(event.sender.contactID)) {
            data.value = true
            return true
        }

        return false
    }

    override fun handleMessage(
        event: MessageEvent,
        param: Boolean
    ): ServiceCallStatistic? {
        migrateGradually()

        return null
    }

    private fun migrateGradually() {
        // 根据数据库查询结果，物理 id 从 488971 开始，所以游标前移一位设为 488970L
        var lastId = 488970L // 0L
        var totalProcessed = 0

        log.info("🚀 渐进式同步启动！起始 ID 游标设为: $lastId")

        while (true) {
            // 批量拉取 1000 条（基于物理主键 id）
            val batchList = oldRepo.findTop1000ByIdGreaterThanOrderByIdAsc(lastId)

            if (batchList.isEmpty()) {
                log.info("🏁 旧表中已经没有更多比 ID $lastId 大的数据了，迁移正常结束。")
                break
            }

            // 在内存中一条一条慢慢清理
            for (archive in batchList) {
                val internalNegativeID = archive.id!! * -1

                val user = fromArchive(archive)

                val fakeToday = archive.time.atOffset(ZoneOffset.ofHours(8))
                    .withOffsetSameInstant(ZoneOffset.UTC)
                    .toLocalDate()

                user.beatmapPlaycount.takeIf { it > 0}?.runCatching {
                    val entity = UserInfoLite(id = internalNegativeID).updateFrom(user).apply {
                        this.createdAt = fakeToday
                        this.updatedAt = fakeToday
                    }

                    userInfoRepository.upsert(entity)
                }?.onFailure { e ->
                    log.error("❌ Info 转换失败跳过！旧表物理ID: ${archive.id}, Osu用户ID: ${archive.userID}. 原因: ${e.message}")
                }

                user.statistics?.countryRank?.runCatching {
                    val entity = UserRankPercentLite().updateFrom(user)?.apply {
                        this.date = fakeToday
                    } ?: return@runCatching

                    userRankPercentRepository.upsert(entity)
                }?.onFailure { e ->
                    log.error("❌ RankPercent 转换失败跳过！旧表物理ID: ${archive.id}, Osu用户ID: ${archive.userID}. 原因: ${e.message}")
                }

                runCatching {
                    // 新增
                    val entity = UserStatisticsLite(id = internalNegativeID).updateFrom(user)!!.apply {
                        this.createdAt = fakeToday
                        this.updatedAt = fakeToday
                    }

                    userStatisticsRepository.upsert(entity)
                }.onFailure { e ->
                    log.error("❌ Statistics 转换失败跳过！旧表物理ID: ${archive.id}, Osu用户ID: ${archive.userID}. 原因: ${e.message}")
                }

                user.rankHistory?.data?.runCatching {
                    val history = user.rankHistory!!.data

                    val delta = (history.filter { it > 0 }.size - 1)

                    if (delta < 0L) return@runCatching

                    val totalDays = history.size
                    val incomingMap: Map<LocalDate, Long> = history.mapIndexed { index, rank ->
                        val daysAgo = (totalDays - 1) - index
                        val date = fakeToday.minusDays(daysAgo.toLong())
                        date to rank
                    }.filter { it.second > 0L }.toMap()

                    if (incomingMap.isEmpty()) return@runCatching

                    // 2. 找出这批数据的实际日期边界
                    val target = incomingMap.keys
                    val startDate = target.minOrNull()!!
                    val endDate = target.maxOrNull()!!

                    // 3. 从数据库捞出这段时间内【已经存在】的日期集合
                    val exist = userGlobalRankRepository.getDateBetween(user.userID, user.currentOsuMode.modeValue, startDate, endDate).toSet()

                    val insert = target.minus(exist)

                    if (insert.isNotEmpty()) {
                        // 无需更新数据
                        val entities = insert.map { date ->
                            UserGlobalRankLite(
                                userID = user.userID,
                                mode = user.currentOsuMode.modeValue,
                                date = date,
                                rank = incomingMap[date]!!
                            )
                        }

                        userGlobalRankRepository.saveAll(entities)
                    }
                }?.onFailure { e ->
                    log.error("❌ GlobalRank 转换失败跳过！旧表物理ID: ${archive.id}, Osu用户ID: ${archive.userID}. 原因: ${e.message}")
                }


                lastId = archive.id!!
                totalProcessed++
            }

            // 放在这里打印，每处理完一个批次（1000条）触发一次日志，更优雅
            log.info("📊 [进度通知] 已处理数据量: $totalProcessed 条 | 当前物理主键 ID 推进至: $lastId")
        }

        log.info("🎉 恭喜！全表数据同步清洗完毕，共计处理 $totalProcessed 条数据。")
    }

    companion object {
        private val log = LoggerFactory.getLogger(TestService::class.java)

        fun fromArchive(archive: OsuUserInfoArchiveLite): OsuUser {
            val user = OsuUser()
            user.defaultOsuMode = archive.mode
            user.currentOsuMode = archive.mode
            user.id = archive.userID
            user.userAchievementsCount = archive.achievementsCount
            user.beatmapPlaycount = archive.beatmapPlaycount

            val statistics = InfoLogStatistics()
            statistics.countA = archive.countA
            statistics.countS = archive.countS
            statistics.countSS = archive.countSS
            statistics.countSH = archive.countSH
            statistics.countSSH = archive.countSSH

            statistics.globalRank = archive.globalRank
            statistics.globalRankPercent = archive.globalRankPercent
            statistics.countryRank = archive.countryRank
            statistics.totalScore = archive.totalScore
            statistics.totalHits = archive.totalHits
            statistics.rankedScore = archive.rankedScore
            statistics.accuracy = archive.accuracy
            statistics.playCount = archive.playCount
            statistics.playTime = archive.playTime
            statistics.levelCurrent = archive.levelCurrent.toShort()
            statistics.levelProgress = archive.levelProgress.toShort()
            statistics.maxCombo = archive.maximumCombo
            statistics.replaysWatchedByOthers = archive.replaysWatched
            statistics.pp = archive.pp

            statistics.logTime = archive.time

            user.statistics = statistics
            archive.rankHistory?.let {
                user.rankHistory = OsuUser.RankHistory(archive.mode.shortName,
                    JacksonUtil.parseObjectList(archive.rankHistory, Long::class.java)
                )
            } ?: run {
                user.rankHistory = OsuUser.RankHistory(archive.mode.shortName,
                    emptyList()
                )
            }

                return user
        }

        private fun OsuUserInfoArchiveLite.updateFrom(user: OsuUser, mode: OsuMode): OsuUserInfoArchiveLite {
            val archive = this

            archive.userID = user.userID
            archive.setLiteStatistics(user.statistics)

            archive.playCount = user.playCount
            archive.playTime = user.playTime
            user.rankHistory?.let { archive.rankHistory = it.data.toString() }
            archive.beatmapPlaycount = user.beatmapPlaycount
            archive.achievementsCount = user.userAchievementsCount

            // 过滤掉非法的游戏模式
            if (mode.isDefault()) {
                archive.mode = OsuMode.getMode(user.currentOsuMode.toSafeModeValue())
            } else {
                archive.mode = OsuMode.getMode(mode.toSafeModeValue())
            }

            archive.time = LocalDateTime.now()
            return archive
        }

        private fun OsuUserInfoArchiveLite.setLiteStatistics(statistics: Statistics?) {
            if (statistics == null) return

            this.globalRank = statistics.globalRank
            this.globalRankPercent = statistics.globalRankPercent
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
            this.levelCurrent = statistics.levelCurrent.toInt()
            this.levelProgress = statistics.levelProgress.toInt()
            this.isRanked = statistics.ranked ?: false
            this.maximumCombo = statistics.maxCombo
            this.replaysWatched = statistics.replaysWatchedByOthers
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