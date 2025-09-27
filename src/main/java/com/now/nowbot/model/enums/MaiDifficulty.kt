package com.now.nowbot.model.enums

import com.now.nowbot.util.command.REG_SEPERATOR_NO_SPACE

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
        fun getIndex(difficulty: MaiDifficulty): Int {
            return when (difficulty) {
                BASIC -> 0
                ADVANCED -> 1
                EXPERT -> 2
                MASTER -> 3
                RE_MASTER -> 4
                UTAGE -> 5
                else -> -1
            }
        }

        fun getDifficulty(int: Int?): MaiDifficulty {
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

        /**
         * 通过字符串来获取多个难度。
         * 如果为空，则视作允许任意难度。
         */
        fun getDifficulties(str: String?): List<MaiDifficulty> {
            if (str.isNullOrEmpty()) return listOf()

            val strs = str.split(REG_SEPERATOR_NO_SPACE.toRegex())

            val diffs = strs.map { getDifficulty(it) }
                .distinct()
                .filter { it != DEFAULT }

            return diffs
        }

        fun getDifficulty(str: String?): MaiDifficulty {
            if (str.isNullOrEmpty()) return DEFAULT

            return when (str.trim().lowercase()) {
                "basic",
                "bsc",
                "ba",
                "b",
                "1",
                "绿",
                "初",
                "初级" -> {
                    BASIC
                }
                "advanced",
                "adv",
                "ad",
                "a",
                "2",
                "黄",
                "高",
                "高级" -> {
                    ADVANCED
                }
                "expert",
                "exp",
                "ex",
                "e",
                "3",
                "红",
                "专",
                "专家" -> {
                    EXPERT
                }
                "master",
                "mas",
                "mst",
                "ma",
                "m",
                "4",
                "紫",
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
                "5",
                "白",
                "宗",
                "宗师" -> {
                    RE_MASTER
                }
                "utage",
                "uta",
                "ut",
                "u",
                "6",
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
