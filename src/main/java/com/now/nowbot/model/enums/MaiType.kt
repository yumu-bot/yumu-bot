package com.now.nowbot.model.enums

import com.now.nowbot.throwable.botRuntimeException.NoSuchElementException

enum class MaiType(val english: String) {
    POPS_ANIME("POPS & ANIME"),

    NICONICO_VOCALOID("niconico & VOCALOID"),

    TOUHOU_PROJECT("Touhou Project"),

    MAIMAI("maimai"),

    ONGEKI_CHUNITHM("Ongeki & CHUNITHM"),

    GAME_VARIETY("GAME & VARIETY"),

    U_TA_GE("U * TA * GE"),

    ;

    companion object {
        fun getType(genre: String?): MaiType {
            return when (genre?.trim()) {
                "东方Project", "東方Project", "东方", "东", "touhou", "t" -> TOUHOU_PROJECT

                "maimai", "舞萌", "舞", "m" -> MAIMAI

                "niconico & VOCALOID", "niconicoボーカロイド", "v家", "博歌乐", "nv", "n", "v" -> NICONICO_VOCALOID

                "流行&动漫", "POPSアニメ", "pop", "pops", "anime", "流行", "动漫", "流", "漫", "a", "p" -> POPS_ANIME

                "其他游戏", "ゲームバラエティ", "其他", "游戏", "游", "game", "g" -> GAME_VARIETY

                "音击&中二节奏", "オンゲキCHUNITHM", "ongeki", "chunithm", "音击", "中二", "中二节奏", "音", "中", "o", "c" -> ONGEKI_CHUNITHM

                "宴会場", "宴会场", "宴", "utage", "u" -> U_TA_GE

                else -> throw NoSuchElementException("类型错误。")

            }
        }
    }

}