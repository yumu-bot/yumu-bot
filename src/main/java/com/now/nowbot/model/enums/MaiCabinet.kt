package com.now.nowbot.model.enums

import com.now.nowbot.model.maimai.MaiSong

enum class MaiCabinet {
    DX,
    SD,
    ANY,

    ;

    companion object {
        fun getCabinet(string: String?): MaiCabinet {
            if (string.isNullOrBlank()) return ANY

            return when(string.lowercase()) {
                "sd", "标准", "standard", "标" -> SD
                "dx", "豪华", "deluxe" -> DX
                else -> ANY
            }
        }

        fun getCabinet(song: MaiSong?): MaiCabinet {
            if (song == null) return ANY
            return if (song.isDeluxe) {
                DX
            } else {
                SD
            }
        }
    }
}