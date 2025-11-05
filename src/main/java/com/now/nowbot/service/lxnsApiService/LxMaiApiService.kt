package com.now.nowbot.service.lxnsApiService

import com.now.nowbot.model.maimai.LxMaiSong
import com.now.nowbot.model.maimai.MaiAlias
import com.now.nowbot.model.maimai.MaiSong

interface LxMaiApiService {
    fun getLxMaiSongs(): List<LxMaiSong>

    fun saveLxMaiSongs()

    fun getLxMaiSong(songID: Long): LxMaiSong?

    fun getMaimaiAlias(songID: Int): MaiAlias?

    fun insertMaimaiAlias(song: LxMaiSong?)

    fun insertMaimaiAlias(songs: List<LxMaiSong>?)

    fun getMaiSongs(): List<MaiSong>

    fun getPossibleMaiSongs(text: String): List<MaiSong>

    fun getMaiAliasSongs(text: String): List<MaiSong>

    fun getMaiAliasLibrary(): Map<Int, List<String>>
}