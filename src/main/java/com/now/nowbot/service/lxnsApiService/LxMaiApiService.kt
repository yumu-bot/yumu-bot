package com.now.nowbot.service.lxnsApiService

import com.now.nowbot.model.maimai.LxMaiCollection
import com.now.nowbot.model.maimai.LxMaiSong
import com.now.nowbot.model.maimai.MaiAlias
import com.now.nowbot.model.maimai.MaiSong

interface LxMaiApiService {
    fun getAudio(songID: Int): ByteArray

    /**
     * 注意，这个结果没有 10000 段的信息
     */
    fun getLxMaiSongs(): List<LxMaiSong>

    fun saveLxMaiSongs()

    fun saveLxMaiCollections()

    fun getLxMaiSong(songID: Int): LxMaiSong?

    fun getMaimaiAlias(songID: Int): MaiAlias?

    fun insertMaimaiAlias(song: LxMaiSong?)

    fun insertMaimaiAlias(songs: List<LxMaiSong>?)

    fun insertMaimaiAliasForMaiSong(songs: List<MaiSong>?)

    fun getMaiSong(songID: Int): MaiSong?

    fun getMaiSongs(): List<MaiSong>

    fun getPossibleMaiSongs(text: String): List<MaiSong>

    fun getMaiAliasSongs(text: String): List<MaiSong>

    fun getMaiAliasLibrary(): Map<Int, List<String>>

    fun getLxMaiCollection(type: String = "plate", types: String = "plates"): List<LxMaiCollection>

    companion object {

        /**
         * 落雪只记录准确的 songID，此时不需要 10000 以上的单位
         */
        fun convertToLxMaiSongID(songID: Any): Int {
            val id = songID.toString().toInt()

            return if (id in 10000 ..< 100000) {
                id % 10000
            } else {
                id
            }
        }
    }
}