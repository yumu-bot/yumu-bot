package com.now.nowbot.model.enums

enum class MaiDifficulty(val full : String) {
    DEFAULT(""),
    BASIC("Basic"),
    ADVANCED("Advanced"),
    EXPERT("Expert"),
    MASTER("Master"),
    RE_MASTER("Re: Master"),
    UTAGE("U·TA·GE")
    ;
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
                5 -> UTAGE
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
                "0",
                "初",
                "初级" -> {
                    BASIC
                }
                "advanced",
                "adv",
                "ad",
                "a",
                "1",
                "高",
                "高级" -> {
                    ADVANCED
                }
                "expert",
                "exp",
                "ex",
                "e",
                "2",
                "专",
                "专家" -> {
                    EXPERT
                }
                "master",
                "mas",
                "mst",
                "ma",
                "m",
                "3",
                "大",
                "大师" -> {
                    MASTER
                }
                "remaster",
                "re:master",
                "re：master",
                "re master",
                "rem",
                "rms",
                "re",
                "rm",
                "r",
                "4",
                "宗",
                "宗师" -> {
                    RE_MASTER
                }
                "utage",
                "uta",
                "ut",
                "u",
                "5",
                "宴",
                "宴会",
                "宴会场",
                "宴会場" -> {
                    UTAGE
                }
                else -> {
                    DEFAULT
                }
            }
        }
    }
}
