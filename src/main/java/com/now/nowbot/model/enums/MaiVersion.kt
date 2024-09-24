package com.now.nowbot.model.enums

import java.util.Locale
import org.springframework.util.CollectionUtils

enum class MaiVersion(versionName: String, versionAbbreviation: String) {
    NULL("", ""),
    DEFAULT("maimai", ""),
    PLUS("maimai PLUS", "真"),
    GREEN("maimai GreeN", "超"),
    GREEN_PLUS("maimai GreeN PLUS", "檄"),
    ORANGE("maimai ORANGE", "橙"),
    ORANGE_PLUS("maimai ORANGE PLUS", "暁"),
    PINK("maimai PiNK", "桃"),
    PINK_PLUS("maimai PiNK PLUS", "櫻"),
    MURASAKI("maimai MURASAKi", "紫"),
    MURASAKI_PLUS("maimai MURASAKi PLUS", "菫"),
    MILK("maimai MiLK", "白"),
    MILK_PLUS("MiLK PLUS", "雪"),
    FINALE("maimai FiNALE", "輝"),
    ALL_FINALE("ALL FiNALE", "舞"),
    DX("maimai でらっくす", "熊"),
    DX_PLUS("maimai でらっくす PLUS", "華"),
    SPLASH("maimai でらっくす Splash", "爽"),
    SPLASH_PLUS("maimai でらっくす Splash PLUS", "煌"),
    UNIVERSE("maimai でらっくす UNiVERSE", "宙"),
    UNIVERSE_PLUS("maimai でらっくす UNiVERSE PLUS", "星"),
    FESTIVAL("maimai でらっくす FESTiVAL", "祭"),
    FESTIVAL_PLUS("maimai でらっくす FESTiVAL PLUS", "祝"),
    BUDDIES("maimai でらっくす BUDDiES", ""),
    BUDDIES_PLUS("maimai でらっくす BUDDiES PLUS", ""),
    PRISM("maimai でらっくす PRiSM", ""),
    PRISM_PLUS("maimai でらっくす PRiSM PLUS", ""),
    ;

    val full: String = versionName
    val abbr: String = versionAbbreviation

    companion object {
        @JvmStatic
        fun getNameList(versions: MutableList<MaiVersion>): MutableList<String> {
            if (CollectionUtils.isEmpty(versions)) {
                return mutableListOf<String>()
            }

            return versions.stream().map(MaiVersion::full).toList()
        }

        @JvmStatic
        fun getAbbreviationList(versions: MutableList<MaiVersion>): MutableList<String> {
            if (CollectionUtils.isEmpty(versions)) {
                return mutableListOf<String>()
            }

            return versions.stream().map(MaiVersion::abbr).toList()
        }

        fun getMutableVersion(str: String?): MutableList<MaiVersion> {
            if (str == null) return mutableListOf(NULL)

            val out = mutableSetOf<MaiVersion>()
            val strList = str.split(Regex("[,，|:：]"))

            if (strList.isEmpty()) return mutableListOf(NULL)

            for (s in strList) {
                val v = MaiVersion.getVersion(s)

                if (v != NULL) out.add(v)
            }

            if (out.isEmpty()) return mutableListOf(NULL)

            return out.stream().toList()
        }

        fun List<MaiVersion>.listToString(): String {
            return this.stream().filter{it != NULL}.map(MaiVersion::full).toList().joinToString(separator = ", ", prefix = "[", postfix = "]")
        }

        fun getVersion(str: String?): MaiVersion {
            if (str == null) return NULL

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
                "universeplus",
                "universe plus",
                "unp",
                "uvp",
                "unvp",
                "universe+",
                "un+",
                "uv+",
                "uni+",
                "1.31" -> UNIVERSE_PLUS
                "universe",
                "un",
                "uv",
                "uni",
                "1.3",
                "1.30" -> UNIVERSE
                "festivalplus",
                "festival plus",
                "fep",
                "fsp",
                "fesp",
                "festival+",
                "fs+",
                "fes+",
                "fst+",
                "1.21" -> FESTIVAL_PLUS
                "festival",
                "fs",
                "fes",
                "fst",
                "1.2",
                "1.20" -> FESTIVAL
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
                "0.10" -> DEFAULT
                else -> {
                    for (v in entries) {
                        if (str == v.full || str == v.abbr) {
                            return v
                        }
                    }
                    return NULL
                }
            }
        }
    }
}
