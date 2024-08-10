package com.now.nowbot.newbie.mapper

import com.now.nowbot.newbie.entity.UserPlayRecords
import com.now.nowbot.service.OsuApiService.OsuBeatmapApiService
import com.now.nowbot.service.OsuApiService.OsuUserApiService
import com.now.nowbot.util.AsyncMethodExecutor
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.DependsOn
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger


interface UserPlayRecordsMapper : JpaRepository<UserPlayRecords, Long> {
    @Query(
        """
        select distinct u.beatmapId as beatmapId from UserPlayRecords u
        where u.mode = 0 and u.userId = :uid and u.date between :start and :end
    """
    )
    fun queryMap(uid: Int, start: LocalDateTime, end: LocalDateTime): List<CountBeatmap>

    @Query(
        """
        select u.id as userId, u.beatmapId as beatmapId, u.rank as rank, u.count300 as n300, u.count100 as n100, u.count50 as n50, u.countMiss as misses
        from UserPlayRecords u
        where u.mode = 0 and u.userId = :uid and u.date between :start and :end and u.beatmapId in (:maps)
    """
    )
    fun queryData(uid: Int, start: LocalDateTime, end: LocalDateTime, maps: List<Int>): List<CountUser>

    @Query(
        """
        select b.osuId from Bindings b where b.qq in (:qqs)
    """
    )
    fun queryBind(qqs: List<Long>): List<Int>
}

@Service
@DependsOn("newbieEntityManagerFactory")
@ConditionalOnProperty(prefix = "spring.datasource.newbie", name = ["enable"], havingValue = "true")
class NewbieService(
    private val mapper: UserPlayRecordsMapper,
    private var osuMapService: OsuBeatmapApiService,
    private val osuUserService: OsuUserApiService,
) {
    private val log = LoggerFactory.getLogger(NewbieService::class.java)

    fun checkRank(bid: Int, cache: MutableMap<Int, Boolean>) = cache.getOrPut(bid) {
        osuMapService.testNewbieCountMap(bid.toLong())
    }

    fun getUserPlayRecords(
        uid: Int, start: LocalDateTime, end: LocalDateTime, pp: Float, cache: MutableMap<Int, Boolean>
    ): UserCount {

        val omap = if (start.isBefore(end)) {
            mapper.queryMap(uid, start, end).map { it.beatmapId }
        } else {
            mapper.queryMap(uid, end, start).map { it.beatmapId }
        }
        val maps = omap.filter { checkRank(it, cache) }.toList()
        val playData = mapper.queryData(uid, start, end, maps)
        // 处理tth
        val countTth = playData.sumOf {
            with(it) {
                n300 + n100 + n50
            }
        }
        // 处理time
        val (pass, unpass) = playData.partition { it.rank != "F" }

        val passTime = pass.sumOf {
            with(it) {
                val b = osuMapService.getBeatMapInfoFromDataBase(beatmapId.toLong())
                b.hitLength
            }
        }

        val unpassTime = unpass.sumOf {
            with(it) {
                val hits = n300 + n100 + n50 + misses
                osuMapService.getFailTime(beatmapId.toLong(), hits)
            }
        }

        return UserCount(uid, pp, playData.size, passTime + unpassTime, countTth)
    }

    fun countToday(qqIds: List<Long>): List<UserCount> {
        val users = mapper.queryBind(qqIds)
        val under3K = mutableSetOf<Int>()
        val userPP = mutableMapOf<Int, Float>()

        // 分批次 筛选出pp小于3000的用户
        users.chunked(50) { i ->
            Thread.sleep(3000)
            osuUserService.getUsers(i)
                .filter { it.rulesets != null && it.rulesets.osu != null && it.rulesets.osu.pp < 3000 }
                .map {
                    userPP[it.id.toInt()] = it.rulesets.osu.pp.toFloat()
                    it.id.toInt()
                }.forEach { under3K.add(it) }
        }
        val todayTime = LocalDate.now().atTime(0, 0, 0)
        val yesterdayTime = todayTime.minusDays(1)
        val timeRange = TimeRange(yesterdayTime, todayTime)
        log.info("ready to count ${under3K.size} users")
        return getAll(under3K.toList(), timeRange, userPP)
    }


    fun getAll(
        uid: List<Int>,
        timeRange: TimeRange,
        ppMap: Map<Int, Float>,
        cache: MutableMap<Int, Boolean> = ConcurrentHashMap()
    ): List<UserCount> {
        val result = mutableListOf<UserCount>()
        // 拆分成并发执行, 每 100 个任务一组
        val taskList = mutableListOf<AsyncMethodExecutor.Supplier<List<UserCount>>>()
        val countAll = AtomicInteger(0)
        uid.chunked(10).forEachIndexed { n, it ->
            val task = AsyncMethodExecutor.Supplier<List<UserCount>> {
                val resultList = mutableListOf<UserCount>()
                for (id in it) {
                    val r = AsyncMethodExecutor.doRetry(5) {
                        getUserPlayRecords(id, timeRange.start, timeRange.end, ppMap[id] ?: 0f, cache)
                    }
                    val c = countAll.incrementAndGet()
                    log.info("$c(work $n) - $id done")
                    resultList.add(r)
                }
                return@Supplier resultList
            }
            taskList.add(task)
        }
        AsyncMethodExecutor.AsyncSupplier(taskList).forEach {
            if (!it.isNullOrEmpty()) {
                val playedUser = it.filter { u -> u.playCount > 0 }
                result.addAll(playedUser)
            }
        }
        log.info("task over")
        return result
    }

    data class TimeRange(
        val start: LocalDateTime,
        val end: LocalDateTime,
    )

    data class UserCount(
        var id: Int,
        var pp: Float,
        val playCount: Int,
        val playTime: Int,
        val tth: Int,
    )
}


interface CountBeatmap {
    val beatmapId: Int
}

interface CountUser {
    val userId: Int
    val beatmapId: Int
    val rank: String
    val n300: Int
    val n100: Int
    val n50: Int
    val misses: Int
}