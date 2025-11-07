package com.now.nowbot.service.lxnsApiService

import com.now.nowbot.model.maimai.LxMaiSong
import com.now.nowbot.model.maimai.MaiAlias
import com.now.nowbot.model.maimai.MaiSong

interface LxMaiApiService {
    fun getAudio(songID: Int): ByteArray

    fun getLxMaiSongs(): List<LxMaiSong>

    fun saveLxMaiSongs()

    fun getLxMaiSong(songID: Int): LxMaiSong?

    fun getMaimaiAlias(songID: Int): MaiAlias?

    fun insertMaimaiAlias(song: LxMaiSong?)

    fun insertMaimaiAlias(songs: List<LxMaiSong>?)

    fun getMaiSongs(): List<MaiSong>

    fun getPossibleMaiSongs(text: String): List<MaiSong>

    fun getMaiAliasSongs(text: String): List<MaiSong>

    fun getMaiAliasLibrary(): Map<Int, List<String>>

    companion object {

        /**
         * 落雪只记录准确的 songID，此时不需要 10000 以上的单位
         */
        fun convertToLxMaiSongID(songID: Int): Int {
            return if (songID in 10000 ..< 100000) {
                songID % 10000
            } else {
                songID
            }
        }
    }
}