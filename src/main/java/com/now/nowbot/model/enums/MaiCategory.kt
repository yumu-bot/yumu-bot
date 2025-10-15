package com.now.nowbot.model.enums

import com.now.nowbot.throwable.botRuntimeException.NoSuchElementException
import org.intellij.lang.annotations.Language

enum class MaiCategory(val english: String) {
    POPS_ANIME("POPS & ANIME"),

    NICONICO_VOCALOID("niconico & VOCALOID"),

    TOUHOU_PROJECT("Touhou Project"),

    MAIMAI("maimai"),

    ONGEKI_CHUNITHM("Ongeki & CHUNITHM"),

    GAME_VARIETY("GAME & VARIETY"),

    U_TA_GE("U * TA * GE"),

    ;

    companion object {

        internal enum class CategoryFilter(@param:Language("RegExp") val regex: Regex) {
            POPS_ANIME("流行&?动漫|pops\\s*&?\\s*anime|pops\\s*アニメ|アニメ|pops?|anime|流行|动漫|ap".toRegex()),

            NICONICO_VOCALOID("(niconico)?\\s*&?\\s*(ボーカロイド|vocaloid)|niconico|vocal|vocaloid|ボーカロイド|博歌乐|nv".toRegex()),

            TOUHOU_PROJECT("[东東]方\\s*project|[东東]方?|车万|touhou|th|[t东東]".toRegex()),

            MAIMAI("mai(mai)?|original|舞萌?|原创?".toRegex()),

            ONGEKI_CHUNITHM("ongeki\\s*&?\\s*chunithm|オンゲキ\\s*chunithm|音击&中二节奏|音击|中二(节奏)?|oc".toRegex()),

            GAME_VARIETY("game\\s*&?\\svariety|ゲーム\\s*バラエティ|(其他)?游戏|其他|game|variety".toRegex()),

            U_TA_GE("宴会[場场]|宴会?|utage".toRegex()),
        }


        fun getCategory(genre: String?): MaiCategory {
            if (genre.isNullOrEmpty()) throw NoSuchElementException("舞萌歌曲类型错误。")

            val g = genre.lowercase()

            CategoryFilter.entries.mapIndexed { i: Int, filter: CategoryFilter ->
                if (g.matches(filter.regex)) {
                    return MaiCategory.entries.getOrNull(i) ?: throw NoSuchElementException("舞萌歌曲类型错误。")
                }
            }

            /*
            val conditions = DataUtil.paramMatcher(genre, CategoryFilter.entries.map { it.regex })

            conditions.map { it.size }.forEachIndexed { size, i ->
                if (size > 0) {
                    return entries.getOrNull(i) ?: throw NoSuchElementException("舞萌歌曲类型错误。")
                }
            }

             */

            return when(g.first()) {
                'a', 'p', 'ア', '流', '动', '漫' -> POPS_ANIME
                'n', 'v', 'に', 'ボ' -> NICONICO_VOCALOID
                't', 'h', '东', '東', '车' -> TOUHOU_PROJECT
                'm', 'i', 'l', '原', '舞' -> MAIMAI
                'o', 'c', 'オ', '音', '中' -> ONGEKI_CHUNITHM
                'g', 'y', 'ゲ', 'バ', '游', '其' -> GAME_VARIETY
                'u', 'e', '宴' -> U_TA_GE
                else -> throw NoSuchElementException("舞萌歌曲类型错误：找不到 $g。")
            }
        }
    }

}