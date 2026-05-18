package com.now.nowbot.model.enums

import com.now.nowbot.model.maimai.MaiSong

enum class MaiCabinet {
    DX,
    SD,
    UTAGE,
    ANY,

    ;

    companion object {
        fun getCabinet(string: String?, isUtage: Boolean = false): MaiCabinet {
            if (string.isNullOrBlank()) return ANY
            if (isUtage) {
                return UTAGE
            }

            return when(string.lowercase()) {
                "sd", "标准", "standard", "标" -> SD
                "dx", "豪华", "deluxe" -> DX
                "宴会场", "宴", "宴会", "utage" -> UTAGE
                else -> ANY
            }
        }

        fun getCabinet(song: MaiSong?): MaiCabinet {
            if (song == null) return ANY
            return if (song.isUtage) {
                UTAGE
            } else if (song.isDeluxe) {
                DX
            } else {
                SD
            }
        }
    }
}