package com.now.nowbot.newbie.mapper

import com.now.nowbot.entity.NewbiePlayCount
import com.now.nowbot.mapper.NewbiePlayCountRepository
import com.now.nowbot.newbie.entity.Bindings
import com.now.nowbot.newbie.entity.UserPlayRecords
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.service.osuApiService.OsuUserApiService
import com.now.nowbot.util.AsyncMethodExecutor
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.DependsOn
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Service
import java.io.BufferedWriter
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap

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
        select u.id as userId, u.beatmapId as beatmapId, u.rank as rank,
        u.count300 as n300, u.count100 as n100, u.count50 as n50, u.countMiss as misses, u.date as date
        from UserPlayRecords u
        where u.mode = 0 and u.userId = :uid and u.date between :start and :end
    """
    )
    fun queryDataAll(uid: Int, start: LocalDateTime, end: LocalDateTime): List<CountUserDate>

    @Query(
        """
        select b.osuId from Bindings b where b.qq in (:qqs)
    """
    )
    fun queryBind(qqs: List<Long>): List<Int>

    @Query(
        """
        select b from Bindings b where b.qq in (:qqs)
    """
    )
    fun queryBindData(qqs: List<Long>): List<Bindings>

    @Query(
        """
        select "UserId" as id, "UserInfo_Name" as name, "UserInfo_Performance" as pp
        FROM public."UserSnapshots" 
        where "UserId" = :uid and "Date" > :start and "Mode" = 0  order by "Id" asc limit 1;
    """, nativeQuery = true
    )
    fun getUserPP(uid: Long, start: LocalDateTime): CountUserInfo?
}

@Service
@DependsOn("newbieEntityManagerFactory")
@ConditionalOnProperty(prefix = "spring.datasource.newbie", name = ["enable"], havingValue = "true")
class NewbieDao(
    private val mapper: UserPlayRecordsMapper,
    private val newbiePlayCountRepository: NewbiePlayCountRepository,
    private var osuMapService: OsuBeatmapApiService,
    private val osuUserService: OsuUserApiService,
) {
    private val log = LoggerFactory.getLogger(NewbieDao::class.java)

    fun checkRank(bid: Int, cache: MutableMap<Int, Boolean>) = cache.getOrPut(bid) {
        osuMapService.isNotOverRating(bid.toLong())
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
                val b = osuMapService.getBeatmapFromDatabase(beatmapId.toLong())
                b.hitLength!!
            }
        }

        val unpassTime = unpass.sumOf {
            with(it) {
                val hits = n300 + n100 + n50 + misses
                osuMapService.getFailTime(beatmapId.toLong(), hits)
            }
        }

        log.info("user $uid pass: $passTime, unpass: $unpassTime")

        return UserCount(uid, pp, playData.size, passTime + unpassTime, countTth)
    }

    fun countData(
        uid: List<Int>,
        start: LocalDateTime,
        end: LocalDateTime,
        callback: ((UserCount) -> Unit) = { }
    ): List<UserCount> {
        val under3K = mutableSetOf<Long>()
        val userPP = mutableMapOf<Long, Float>()

        // 分批次 筛选出pp小于3600的用户
        uid.chunked(50) { i ->
            Thread.sleep(3000)
            osuUserService.getUsers(i)
                .filter { it.rulesets != null && it.rulesets!!.osu != null && it.rulesets!!.osu!!.pp!! < 3600 }
                .map {
                    userPP[it.userID] = it.rulesets!!.osu!!.pp!!.toFloat()
                    it.userID
                }.forEach { under3K.add(it) }
        }
        val timeRange = TimeRange(start, end)
        log.info("ready to count ${under3K.size} users")
        return getAll(under3K.toList(), timeRange, userPP, callback = callback)
    }

    fun countDataByQQ(
        qqIds: List<Long>,
        start: LocalDateTime,
        end: LocalDateTime,
        callback: ((UserCount) -> Unit) = { }
    ): List<UserCount> {
        val users = mapper.queryBind(qqIds)
        return countData(users, start, end, callback)
    }

    fun countToday(qqIds: List<Long>): List<UserCount> {
        val todayTime = LocalDate.now().atTime(0, 0, 0)
        val yesterdayTime = todayTime.minusDays(1)
        return countDataByQQ(qqIds, todayTime, yesterdayTime)
    }


    fun getAll(
        uid: List<Long>,
        timeRange: TimeRange,
        ppMap: Map<Long, Float>,
        cache: MutableMap<Int, Boolean> = ConcurrentHashMap(),
        callback: ((UserCount) -> Unit) = { }
    ): List<UserCount> {
        val result = mutableListOf<UserCount>()
        uid.forEachIndexed { n, id ->
            val data = AsyncMethodExecutor.doRetry(5) {
                Thread.sleep(3000)
                log.info("start to count $id")
                this.getUserPlayRecords(id.toInt(), timeRange.start, timeRange.end, ppMap[id] ?: 0f, cache)
            }
            if (data.playCount < 1) return@forEachIndexed
            callback(data)
            result.add(data)
        }
        return result
    }

    fun updateUserPP(date: LocalDateTime, end: LocalDateTime, selectDate: LocalDate, out: BufferedWriter) {
        out.write("name, uid, pp, pc, tth, pt")
        out.newLine()
        val all = newbiePlayCountRepository.findAllByDate(selectDate)
        val newbie = all.map { it.uid!! }
        countData(newbie, date, end) {
            val user = NewbiePlayCount(it)
            val uid = it.id
            val nowPP = it.pp
            val newUser = mapper.getUserPP(uid.toLong(), date)!!
            user.pp = newUser.pp
            newbiePlayCountRepository.saveAndFlush(user)
            val upPP = nowPP - newUser.pp
            out.write("${newUser.name}, ${newUser.id}, ${upPP}, ${it.playCount}, ${it.playCount}, ${it.playTime}")
            out.newLine()
        }
    }

    data class TimeRange(
        var start: LocalDateTime,
        var end: LocalDateTime,
    ) {
        init {
            if (start.isAfter(end)) {
                start = end.also { end = start }
            }
        }

    }

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

interface CountUserInfo {
    val id: Long
    val name: String
    val pp: Float
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

interface CountUserDate {
    val userId: Int
    val beatmapId: Int
    val rank: String
    val n300: Int
    val n100: Int
    val n50: Int
    val misses: Int
    val date: LocalDateTime
}