package com.now.nowbot.service.osuApiService

import com.fasterxml.jackson.databind.JsonNode
import com.now.nowbot.model.osu.LazerMod
import com.now.nowbot.model.osu.LazerMod.Companion.getModsValue
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.osu.*
import java.io.IOException
import java.util.*

interface OsuBeatmapApiService {
    fun getBeatmapFileString(bid: Long): String?

    fun getBeatmapFileByte(bid: Long): ByteArray?

    fun hasBeatmapFileFromDirectory(bid: Long): Boolean

    fun refreshBeatmapFileFromDirectory(bid: Long): Boolean

    // 查一下文件是否跟 checksum 是否对得上
    @Throws(IOException::class) fun checkBeatmap(beatmap: Beatmap?): Boolean

    @Throws(IOException::class) fun checkBeatmap(bid: Long, checkStr: String?): Boolean

    @Throws(IOException::class) fun checkBeatmap(beatmap: Beatmap, fileStr: String): Boolean

    fun getBeatmap(bid: Long): Beatmap

    fun getBeatmaps(ids: Iterable<Long>): List<Beatmap>

    /**
     * @param type favourite, graveyard, guest, loved, nominated, pending, ranked
     * 注意，是英式英文的 favourite，有个 u
     */
    fun getUserBeatmapset(id: Long, type: String = "favourite", offset: Int = 0, limit: Int = 100): List<Beatmapset>

    fun getUserMostPlayedBeatmaps(id: Long, offset: Int = 0, limit: Int = 100): Map<Int, Beatmap>

    fun getBeatmapsets(sid: Long): Beatmapset

    /**
     * 这个可能会撞限制，所以请用 extendBeatmapset
     */
    fun getBeatmapsets(sids: Iterable<Long>): List<Beatmapset>

    /**
     * 扩展 search result 的 set 内的 beatmap，确保它含有 owners 等信息
     */
    fun extendBeatmapInSet(sets: Iterable<Beatmapset>): List<Beatmapset>

    /**
     * 扩展 score 内的 beatmap，确保它含有 owners 等信息
     */
    fun extendBeatmapInScore(scores: Iterable<LazerScore>): List<LazerScore>

    fun getBeatmapFromDatabase(bid: Int): Beatmap {
        return getBeatmapFromDatabase(bid.toLong())
    }

    // TODO database 存的谱面缺太多东西，比如甚至谱面状态都没有。。。
    fun getBeatmapFromDatabase(bid: Long): Beatmap

    fun isNotOverRating(bid: Long): Boolean

    @Throws(Exception::class) fun getBeatmapObjectGrouping26(beatmap: Beatmap): IntArray

    fun getFailTime(bid: Long, passObj: Int): Int

    fun getAllFailTime(all:List<Pair<Long, Int>>): List<Int>

    fun getAllBeatmapHitLength(bid: Collection<Long>): List<Pair<Long, Int>>

    fun getPlayPercentage(score: LazerScore): Double

    fun getAttributes(id: Long, mode: OsuMode?): BeatmapDifficultyAttributes

    fun getAttributes(id: Long, mode: OsuMode?, modsInt: Int): BeatmapDifficultyAttributes

    fun getAttributes(id: Long): BeatmapDifficultyAttributes {
        return getAttributes(id, OsuMode.DEFAULT)
    }

    fun getAttributes(id: Long, value: Int): BeatmapDifficultyAttributes {
        return getAttributes(id, OsuMode.DEFAULT, value)
    }

    fun getAttributes(id: Long, mods: List<LazerMod>?): BeatmapDifficultyAttributes {
        if (mods.isNullOrEmpty()) return getAttributes(id, OsuMode.DEFAULT)

        return getAttributes(id, getModsValue(mods))
    }

    fun lookupBeatmap(checksum: String?, filename: String?, id: Long?): JsonNode?

    /**
     * 单次获取搜索结果
     */
    fun searchBeatmapset(query: Map<String, Any?>): BeatmapsetSearch

    /**
     * 多次获取搜索结果
     */
    fun searchBeatmapset(query: Map<String, Any?>, tries: Int): BeatmapsetSearch

    // 给同一张图的成绩添加完整的谱面
    fun applyBeatmapExtendForSameScore(scores: List<LazerScore>, beatmap: Beatmap)

    // 给成绩添加完整的谱面
    fun applyBeatmapExtend(score: LazerScore)

    fun applyBeatmapExtend(scores: List<LazerScore>)

    fun applyBeatmapExtend(score: LazerScore, extended: Beatmap)

    // 给成绩添加完整的谱面
    fun applyBeatmapExtendFromDatabase(score: LazerScore)

    fun applyBeatmapExtendFromDatabase(scores: List<LazerScore>)

    // 获取Q区谱面大致的上架时间
    fun getBeatmapsetRankedTime(beatmap: Beatmap): String

    // 获取谱面大致的上架时间
    fun getBeatmapsetRankedTimeMap(): Map<Long, String>

    fun applyBeatmapsetRankedTime(beatmapsets: List<Beatmapset>)

    fun updateBeatmapTagLibraryDatabase()

    fun extendBeatmapTag(beatmap: Beatmap)
}
