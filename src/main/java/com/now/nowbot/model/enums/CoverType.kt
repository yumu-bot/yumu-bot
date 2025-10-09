package com.now.nowbot.model.enums

import com.now.nowbot.model.osu.Covers

enum class CoverType {
    RAW, CARD, CARD_2X, COVER, COVER_2X, LIST, LIST_2X, SLIM_COVER, SLIM_COVER_2X;

    companion object {
        fun getCovetType(string: String?): CoverType {
            return when (string) {
                "raw", "r", "full", "f", "background", "b" -> RAW
                "list", "l", "l1" -> LIST
                "list2", "list2x", "list@2x", "l2" -> LIST_2X
                "c", "card", "c1" -> CARD
                "card2", "card2x", "card@2x", "c2" -> CARD_2X
                "slim", "slimcover", "s", "s1" -> SLIM_COVER
                "slim2", "slim2x", "slim@2x", "slimcover2", "slimcover2x", "slimcover@2x", "s2" -> SLIM_COVER_2X
                "2", "cover2", "cover2x", "cover@2x", "o2" -> COVER_2X
                null -> COVER
                else -> COVER
            }
        }

        fun Covers.getString(type: CoverType): String {
            return when (type) {
                LIST -> list
                LIST_2X -> list2x
                CARD -> card
                CARD_2X -> card2x
                SLIM_COVER -> slimcover
                SLIM_COVER_2X -> slimcover2x
                COVER_2X -> cover2x
                COVER -> cover
                RAW -> list.replace("@2x", "").replace("list", "raw")
            }
        }
    }
}