package com.now.nowbot.service.osuApiService

import com.now.nowbot.model.json.Match
import com.now.nowbot.model.multiplayer.MatchQuery
import com.now.nowbot.model.multiplayer.MonitoredMatch
import org.springframework.web.reactive.function.client.WebClientResponseException

interface OsuMatchApiService {

    /**
     *  `id_asc` 旧的在前, `id_desc` 新的在前
     * @param sort `id_asc` or `id_desc`
     */
    @Throws(WebClientResponseException::class)
    fun queryMatch(limit: Int = 50, sort: String = "id_desc", cursor: String? = null): MatchQuery

    /**
     * 查找的是正在进行的比赛
     * @param name 比赛名包含 `name` 忽略大小写
     * @param limit 查找最近第 `limit` 范围内的比赛, 以创建房间的时间开始排, 如果过于久远查不到
     */
    fun queryMatch(name: String, limit: Int = 300): List<MonitoredMatch.MatchStat> {
        var query: MatchQuery
        var cursor: String? = null
        val result = mutableListOf<MonitoredMatch.MatchStat>()
        var count = 0
        do {
            query = queryMatch(50, "id_desc", cursor)
            cursor = query.cursor
            count += query.matches.size
            val data = query.matches.filter { it.endTime == null && it.name.contains(name, true) }
            result.addAll(data)
        } while (cursor != null && count < limit)
        return result
    }

    fun getMonitoredMatchInfo(mid: Long, before: Long? = null, after: Long? = null, limit: Int = 100) : MonitoredMatch

    @Throws(WebClientResponseException::class)
    fun getMatchInfo(mid: Long, limit: Int): Match

    @Throws(WebClientResponseException::class)
    fun getMatchInfoFirst(mid: Long): Match

    @Throws(WebClientResponseException::class)
    fun getMatchInfoBefore(mid: Long, id: Long): Match

    @Throws(WebClientResponseException::class)
    fun getMatchInfoAfter(mid: Long, id: Long): Match
}