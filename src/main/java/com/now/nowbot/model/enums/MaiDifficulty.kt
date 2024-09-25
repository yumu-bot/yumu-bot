package com.now.nowbot.model.enums

enum class MaiDifficulty(val full : String) {
    DEFAULT(""),
    BASIC("Basic"),
    ADVANCED("Advanced"),
    EXPERT("Expert"),
    MASTER("Master"),
    RE_MASTER("Re: Master");

    // default 默认与所有相等
    fun equalDefault(other: Any?): Boolean {
        return when (other) {
            !is MaiDifficulty -> false
            DEFAULT -> true
            else -> this == other
        }
    }

    companion object {
        @JvmStatic
        fun getIndex(int: Int?): MaiDifficulty {
            return when (int) {
                0 -> BASIC
                1 -> ADVANCED
                2 -> EXPERT
                3 -> MASTER
                4 -> RE_MASTER
                else -> DEFAULT
            }
        }

        @JvmStatic
        fun getDifficulty(str: String?): MaiDifficulty {
            if (str.isNullOrEmpty()) return DEFAULT

            return when (str.trim().lowercase()) {
                "basic",
                "bsc",
                "ba",
                "b",
                "0" -> BASIC
                "advanced",
                "adv",
                "ad",
                "a",
                "1" -> ADVANCED
                "expert",
                "exp",
                "ex",
                "e",
                "2" -> EXPERT
                "master",
                "mas",
                "mst",
                "ma",
                "m",
                "3" -> MASTER
                "remaster",
                "re master",
                "rem",
                "rms",
                "re",
                "r",
                "4" -> RE_MASTER
                else -> DEFAULT
            }
        }
    }
}
