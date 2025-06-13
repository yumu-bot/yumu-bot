package com.now.nowbot.model.enums

import com.now.nowbot.util.command.REG_SEPERATOR_NO_SPACE

enum class MaiVersion(val full: String, val abbreviation: String, val code: String) {
    DEFAULT("", "", ""),
    MAIMAI("maimai", "初", "mai"),
    PLUS("maimai PLUS", "真", "mai"),
    GREEN("maimai GreeN", "超", "grn"),
    GREEN_PLUS("maimai GreeN PLUS", "檄", "grp"),
    ORANGE("maimai ORANGE", "橙", "org"),
    ORANGE_PLUS("maimai ORANGE PLUS", "暁", "orp"),
    PINK("maimai PiNK", "桃", "pnk"),
    PINK_PLUS("maimai PiNK PLUS", "櫻", "pkp"),
    MURASAKI("maimai MURASAKi", "紫", "msk"),
    MURASAKI_PLUS("maimai MURASAKi PLUS", "菫", "msp"),
    MILK("maimai MiLK", "白", "mlk"),
    MILK_PLUS("MiLK PLUS", "雪", "mkp"),
    FINALE("maimai FiNALE", "輝", "fnl"),
    ALL_FINALE("ALL FiNALE", "舞", "afn"),
    DX("maimai でらっくす", "熊", "dx"),
    DX_PLUS("maimai でらっくす PLUS", "華", "dxp"),
    SPLASH("maimai でらっくす Splash", "爽", "spl"),
    SPLASH_PLUS("maimai でらっくす Splash PLUS", "煌", "spp"),
    UNIVERSE("maimai でらっくす UNiVERSE", "宙", "uni"),
    UNIVERSE_PLUS("maimai でらっくす UNiVERSE PLUS", "星", "unp"),
    FESTIVAL("maimai でらっくす FESTiVAL", "祭", "fes"),
    FESTIVAL_PLUS("maimai でらっくす FESTiVAL PLUS", "祝", "fep"),
    BUDDIES("maimai でらっくす BUDDiES", "双", "bud"),
    BUDDIES_PLUS("maimai でらっくす BUDDiES PLUS", "宴", "bdp"),
    PRISM("maimai でらっくす PRiSM", "鏡", "pri"),
    PRISM_PLUS("maimai でらっくす PRiSM PLUS", "", "prp"),
    ;

    companion object {
        @JvmStatic
        fun getNameList(versions: List<MaiVersion>): List<String> {
            if (versions.isEmpty()) {
                return listOf()
            }

            return versions.map(MaiVersion::full)
        }

        @JvmStatic
        fun getCodeList(versions: List<MaiVersion>): List<String> {
            if (versions.isEmpty()) {
                return listOf()
            }

            return versions.map(MaiVersion::code)
        }

        fun getVersionFromAbbr(abbreviation: String): MaiVersion {
            for(v in MaiVersion.entries) {
                if (v.abbreviation == abbreviation) return v
            }

            return DEFAULT
        }

        @JvmStatic
        fun getVersionList(str: String?): List<MaiVersion> {
            if (str == null) return listOf(DEFAULT)

            val out = mutableSetOf<MaiVersion>()
            val strList = str.split(REG_SEPERATOR_NO_SPACE.toRegex())

            if (strList.isEmpty()) return listOf(DEFAULT)

            for (s in strList) {
                val v = MaiVersion.getVersion(s)

                if (v != DEFAULT) out.add(v)
            }

            if (out.isEmpty()) return listOf(DEFAULT)

            return out.toList()
        }

        @JvmStatic
        fun List<MaiVersion>.listToString(): String {
            return this
                .filter{it != DEFAULT}
                .map(MaiVersion::full)
                .joinToString(separator = ", ", prefix = "[", postfix = "]")
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
                .lowercase()) {
                "prismplus",
                "prism plus",
                "prp",
                "prip",
                "prsp",
                "prism+",
                "pr+",
                "pri+",
                "prs+",
                "1.55" -> PRISM_PLUS
                "prism",
                "pr",
                "pri",
                "prs",
                "镜",
                "鏡",
                "舞萌 2025",
                "舞萌2025",
                "2025",
                "25",
                "1.5",
                "1.50" -> PRISM
                "buddiesplus",
                "buddies plus",
                "bdp",
                "budp",
                "buddies+",
                "bd+",
                "bud+",
                "宴",
                "1.45" -> BUDDIES_PLUS
                "buddies",
                "bd",
                "bud",
                "1.4",
                "双",
                "舞萌 2024",
                "舞萌2024",
                "2024",
                "24",
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
                "祝",
                "1.35" -> FESTIVAL_PLUS
                "festival",
                "fs",
                "fes",
                "fst",
                "1.3",
                "祭",
                "舞萌 2023",
                "舞萌2023",
                "2023",
                "23",
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
                "星",
                "1.25" -> UNIVERSE_PLUS
                "universe",
                "un",
                "uv",
                "uni",
                "1.2",
                "宙",
                "舞萌 2022",
                "舞萌2022",
                "2022",
                "22",
                "1.20" -> UNIVERSE
                "splashplus",
                "splash plus",
                "spp",
                "splp",
                "splash+",
                "sp+",
                "spl+",
                "煌",
                "1.15" -> SPLASH_PLUS
                "splash",
                "sp",
                "spl",
                "1.1",
                "爽",
                "舞萌 2021",
                "舞萌2021",
                "2021",
                "21",
                "1.10" -> SPLASH
                "deluxeplus",
                "deluxe plus",
                "dxp",
                "dlxp",
                "deluxe+",
                "dx+",
                "dlx+",
                "華",
                "华",
                "1.05" -> DX_PLUS
                "deluxe",
                "dx",
                "dlx",
                "1.0",
                "熊",
                "舞萌 DX",
                "舞萌DX",
                "舞萌",
                "20",
                "1.00" -> DX
                "allfinale",
                "all finale",
                "finale plus",
                "before deluxe",
                "beforedeluxe",
                "beforedx",
                "afn",
                "finale+",
                "final+",
                "fn+",
                "fnl+",
                "舞",
                "0.75" -> ALL_FINALE
                "finale",
                "final",
                "fn",
                "fnl",
                "0.7",
                "輝",
                "辉",
                "0.70" -> FINALE
                "milkplus",
                "milk plus",
                "mkp",
                "milk+",
                "white+",
                "mk+",
                "mlk+",
                "雪",
                "0.65" -> MILK_PLUS
                "milk",
                "white",
                "mk",
                "mlk",
                "0.6",
                "白",
                "0.60" -> MILK
                "murasakiplus",
                "murasaki plus",
                "msp",
                "murasaki+",
                "ms+",
                "msk+",
                "菫",
                "0.55" -> MURASAKI_PLUS
                "murasaki",
                "ms",
                "msk",
                "0.5",
                "紫",
                "0.50" -> MURASAKI
                "pinkplus",
                "pink plus",
                "pkp",
                "pink+",
                "pk+",
                "pnk+",
                "櫻",
                "樱",
                "0.45" -> PINK_PLUS
                "pink",
                "pk",
                "pnk",
                "0.4",
                "桃",
                "0.40" -> PINK
                "orangeplus",
                "orange plus",
                "orgp",
                "orp",
                "or+",
                "org+",
                "暁",
                "晓",
                "0.35" -> ORANGE_PLUS
                "orange",
                "or",
                "org",
                "0.3",
                "橙",
                "0.30" -> ORANGE
                "greenplus",
                "green plus",
                "grep",
                "grp",
                "gre+",
                "grn+",
                "gr+",
                "檄",
                "0.25" -> GREEN_PLUS
                "green",
                "gr",
                "gre",
                "grn",
                "0.2",
                "超",
                "0.20" -> GREEN
                "plus",
                "maimaiplus",
                "maimai plus",
                "maimai+",
                "pl",
                "pls",
                "mai+",
                "真",
                "0.15" -> PLUS
                "maimai",
                "mi",
                "mai",
                "初",
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
