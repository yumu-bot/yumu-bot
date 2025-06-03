package com.now.nowbot.service.osuApiService

import com.fasterxml.jackson.databind.JsonNode
import com.now.nowbot.model.osu.LazerMod
import com.now.nowbot.model.osu.LazerMod.Companion.getModsValue
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.osu.*
import java.io.IOException
import java.util.*

interface OsuBeatmapApiService {
    fun getBeatMapFileString(bid: Long): String?

    fun getBeatMapFileByte(bid: Long): ByteArray?

    fun hasBeatMapFileFromDirectory(bid: Long): Boolean

    fun refreshBeatMapFileFromDirectory(bid: Long): Boolean

    // 查一下文件是否跟 checksum 是否对得上
    @Throws(IOException::class) fun checkBeatMap(beatmap: Beatmap?): Boolean

    @Throws(IOException::class) fun checkBeatMap(bid: Long, checkStr: String?): Boolean

    @Throws(IOException::class) fun checkBeatMap(beatmap: Beatmap, fileStr: String): Boolean

    // 尽量用 FromDataBase，这样可以节省 API 开支
    fun getBeatMap(bid: Long): Beatmap

    fun getBeatMap(bid: Int): Beatmap {
        return getBeatMap(bid.toLong())
    }

    fun getBeatMapSet(sid: Long): Beatmapset

    fun getBeatMapSet(sid: Int): Beatmapset {
        return getBeatMapSet(sid.toLong())
    }

    fun getBeatMapFromDataBase(bid: Int): Beatmap {
        return getBeatMapFromDataBase(bid.toLong())
    }

    // TODO database 存的谱面缺太多东西，比如甚至谱面状态都没有。。。
    fun getBeatMapFromDataBase(bid: Long): Beatmap

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
    fun searchBeatMapSet(query: Map<String, Any?>): BeatMapSetSearch

    /**
     * 多次获取搜索结果
     */
    fun searchBeatMapSet(query: Map<String, Any?>, tries: Int): BeatMapSetSearch

    // 给同一张图的成绩添加完整的谱面
    fun applyBeatMapExtendForSameScore(scores: List<LazerScore>, beatmap: Beatmap)

    // 给成绩添加完整的谱面
    fun applyBeatMapExtend(score: LazerScore)

    // 给成绩添加完整的谱面
    fun applyBeatMapExtend(scores: List<LazerScore>)

    fun applyBeatMapExtend(score: LazerScore, extended: Beatmap)

    // 给成绩添加完整的谱面
    fun applyBeatMapExtendFromDataBase(score: LazerScore)

    // 获取Q区谱面大致的上架时间
    fun getBeatMapSetRankedTime(beatmap: Beatmap): String

    // 获取谱面大致的上架时间
    fun getBeatMapSetRankedTimeMap(): Map<Long, String>

    fun applyBeatMapSetRankedTime(beatmapsets: List<Beatmapset>)

    fun updateBeatMapTagLibraryDatabase()

    fun extendBeatMapTag(beatmap: Beatmap)
}
