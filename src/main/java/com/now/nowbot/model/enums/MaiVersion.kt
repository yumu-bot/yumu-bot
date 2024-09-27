package com.now.nowbot.model.enums

import java.util.Locale
import org.springframework.util.CollectionUtils

enum class MaiVersion(val full: String, val abbreviation: String, val code: String) {
    DEFAULT("", "", ""),
    MAIMAI("maimai", "真", "mai"),
    PLUS("maimai PLUS", "真", "mai+"),
    GREEN("maimai GreeN", "超", "grn"),
    GREEN_PLUS("maimai GreeN PLUS", "檄", "grn+"),
    ORANGE("maimai ORANGE", "橙", "org"),
    ORANGE_PLUS("maimai ORANGE PLUS", "暁", "org+"),
    PINK("maimai PiNK", "桃", "pnk"),
    PINK_PLUS("maimai PiNK PLUS", "櫻", "pnk+"),
    MURASAKI("maimai MURASAKi", "紫", "msk"),
    MURASAKI_PLUS("maimai MURASAKi PLUS", "菫", "msk+"),
    MILK("maimai MiLK", "白", "mlk"),
    MILK_PLUS("MiLK PLUS", "雪", "mlk+"),
    FINALE("maimai FiNALE", "輝", "fnl"),
    ALL_FINALE("ALL FiNALE", "舞", "afn"),
    DX("maimai でらっくす", "熊", "dx"),
    DX_PLUS("maimai でらっくす PLUS", "華", "dx+"),
    SPLASH("maimai でらっくす Splash", "爽", "spl"),
    SPLASH_PLUS("maimai でらっくす Splash PLUS", "煌", "spl+"),
    UNIVERSE("maimai でらっくす UNiVERSE", "宙", "uni"),
    UNIVERSE_PLUS("maimai でらっくす UNiVERSE PLUS", "星", "uni+"),
    FESTIVAL("maimai でらっくす FESTiVAL", "祭", "fes"),
    FESTIVAL_PLUS("maimai でらっくす FESTiVAL PLUS", "祝", "fes+"),
    BUDDIES("maimai でらっくす BUDDiES", "", "bud"),
    BUDDIES_PLUS("maimai でらっくす BUDDiES PLUS", "", "bud+"),
    PRISM("maimai でらっくす PRiSM", "", "pri"),
    PRISM_PLUS("maimai でらっくす PRiSM PLUS", "", "pri+"),
    ;

    companion object {
        @JvmStatic
        fun getNameList(versions: MutableList<MaiVersion>): MutableList<String> {
            if (CollectionUtils.isEmpty(versions)) {
                return mutableListOf()
            }

            return versions.stream().map(MaiVersion::full).toList()
        }

        @JvmStatic
        fun getAbbreviationList(versions: MutableList<MaiVersion>): MutableList<String> {
            if (CollectionUtils.isEmpty(versions)) {
                return mutableListOf()
            }

            return versions.stream().map(MaiVersion::abbreviation).toList()
        }

        @JvmStatic
        fun getCodeList(versions: MutableList<MaiVersion>): MutableList<String> {
            if (CollectionUtils.isEmpty(versions)) {
                return mutableListOf()
            }

            return versions.stream().map(MaiVersion::code).toList()
        }

        @JvmStatic
        fun getVersionList(str: String?): MutableList<MaiVersion> {
            if (str == null) return mutableListOf(DEFAULT)

            val out = mutableSetOf<MaiVersion>()
            val strList = str.split(Regex("[,，|:：]"))

            if (strList.isEmpty()) return mutableListOf(DEFAULT)

            for (s in strList) {
                val v = MaiVersion.getVersion(s)

                if (v != DEFAULT) out.add(v)
            }

            if (out.isEmpty()) return mutableListOf(DEFAULT)

            return out.stream().toList()
        }

        @JvmStatic
        fun List<MaiVersion>.listToString(): String {
            return this.stream().filter{it != DEFAULT}.map(MaiVersion::full).toList().joinToString(separator = ", ", prefix = "[", postfix = "]")
        }

        @JvmStatic
        fun getCategory(category: String): String {
            return when(category) {
                "東方Project" -> "Touhou Project"
                "舞萌" -> "maimai"
                "niconico & VOCALOID" -> "niconico & VOCALOID"
                "流行&动漫" -> "POPS & ANIME"
                "其他游戏" -> "GAME & VARIETY"
                "音击&中二节奏" -> "Ongeki & CHUNITHM"
                else -> ""
            }
        }

        @JvmStatic
        fun getCategoryAbbreviation(category: String): String {
            return when(category) {
                "东方Project", "東方Project" -> "THP"
                "舞萌", "maimai" -> "MAI"
                "niconico & VOCALOID" -> "NI&VO"
                "POPSアニメ", "流行&动漫" -> "PO&AN"
                "其他游戏" -> "GM&VA"
                "音击&中二节奏", "オンゲキCHUNITHM" -> "OG&CH"
                "宴会場", "宴会场" -> "UTAGE"
                else -> ""
            }
        }

        @JvmStatic
        fun getVersion(str: String?): MaiVersion {
            if (str == null) return DEFAULT

            return when (str.trim { it <= ' ' }
                .replace(Regex("[-—_]"), " ")
                .replace(Regex("\\s*[＋+]"), "+")
                .lowercase(Locale.getDefault())) {
                "prismplus",
                "prism plus",
                "prp",
                "prip",
                "prsp",
                "prism+",
                "pr+",
                "pri+",
                "prs+",
                "1.51" -> PRISM_PLUS
                "prism",
                "pr",
                "pri",
                "prs",
                "1.5",
                "1.50" -> PRISM
                "buddiesplus",
                "buddies plus",
                "bdp",
                "budp",
                "buddies+",
                "bd+",
                "bud+",
                "1.41" -> BUDDIES_PLUS
                "buddies",
                "bd",
                "bud",
                "1.4",
                "1.40" -> BUDDIES
                "festivalplus",
                "festival plus",
                "fep",
                "fsp",
                "fesp",
                "festival+",
                "fs+",
                "fes+",
                "fst+",
                "1.31" -> FESTIVAL_PLUS
                "festival",
                "fs",
                "fes",
                "fst",
                "1.3",
                "1.30" -> FESTIVAL
                "universeplus",
                "universe plus",
                "unp",
                "uvp",
                "unvp",
                "universe+",
                "un+",
                "uv+",
                "uni+",
                "1.21" -> UNIVERSE_PLUS
                "universe",
                "un",
                "uv",
                "uni",
                "1.2",
                "1.20" -> UNIVERSE
                "splashplus",
                "splash plus",
                "spp",
                "splp",
                "splash+",
                "sp+",
                "spl+",
                "1.11" -> SPLASH_PLUS
                "splash",
                "sp",
                "spl",
                "1.1",
                "1.10" -> SPLASH
                "deluxeplus",
                "deluxe plus",
                "dxp",
                "dlxp",
                "deluxe+",
                "dx+",
                "dlx+",
                "1.01" -> DX_PLUS
                "deluxe",
                "dx",
                "dlx",
                "1.0",
                "1.00" -> DX
                "allfinale",
                "all finale",
                "finale plus",
                "before deluxe",
                "beforedeluxe",
                "afn",
                "finale+",
                "final+",
                "fn+",
                "fnl+",
                "0.71" -> ALL_FINALE
                "finale",
                "final",
                "fn",
                "fnl",
                "0.7",
                "0.70" -> FINALE
                "milkplus",
                "milk plus",
                "mkp",
                "milk+",
                "white+",
                "mk+",
                "mlk+",
                "0.61" -> MILK_PLUS
                "milk",
                "white",
                "mk",
                "mlk",
                "0.6",
                "0.60" -> MILK
                "murasakiplus",
                "murasaki plus",
                "msp",
                "murasaki+",
                "ms+",
                "msk+",
                "0.51" -> MURASAKI_PLUS
                "murasaki",
                "ms",
                "msk",
                "0.5",
                "0.50" -> MURASAKI
                "pinkplus",
                "pink plus",
                "pkp",
                "pink+",
                "pk+",
                "pnk+",
                "0.41" -> PINK_PLUS
                "pink",
                "pk",
                "pnk",
                "0.4",
                "0.40" -> PINK
                "orangeplus",
                "orange plus",
                "orgp",
                "orp",
                "or+",
                "org+",
                "0.31" -> ORANGE_PLUS
                "orange",
                "or",
                "org",
                "0.3",
                "0.30" -> ORANGE
                "greenplus",
                "green plus",
                "grep",
                "grp",
                "gre+",
                "grn+",
                "gr+",
                "0.21" -> GREEN_PLUS
                "green",
                "gr",
                "gre",
                "grn",
                "0.2",
                "0.20" -> GREEN
                "plus",
                "maimaiplus",
                "maimai plus",
                "maimai+",
                "pl",
                "pls",
                "mai+",
                "0.11" -> PLUS
                "maimai",
                "mi",
                "mai",
                "0.1",
                "0.10" -> MAIMAI
                else -> {
                    for (v in entries) {
                        if (str == v.full || str == v.abbreviation) {
                            return v
                        }
                    }
                    return DEFAULT
                }
            }
        }
    }
}
