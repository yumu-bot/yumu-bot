package com.now.nowbot.model.enums

import com.now.nowbot.util.command.REGEX_PLUS
import com.now.nowbot.util.command.REGEX_SEPARATOR
import com.now.nowbot.util.command.REGEX_SEPARATOR_NO_SPACE

enum class MaiVersion(
    val full: String,
    val abbreviation: String,
    val code: String,
    val value: Int,
    val color: String,
    val aliases: Array<String>
) {
    DEFAULT("", "", "", 0, "#aaa", emptyArray()),
    ALL_FINALE("ALL FiNALE", "舞", "afn", -1, "#000",
        arrayOf("allfinale", "finaleplus", "beforedeluxe", "beforedx", "finale+", "final+", "fn+", "fnl+", "0.75")),

    MAIMAI("maimai", "初", "mai", 10000, "#00A29D",
        arrayOf("maimai", "mai", "mi", "m", "初", "0.1", "0.10")),
    PLUS("maimai PLUS", "真", "mai", 11000, "#00A29D",
        arrayOf("plus", "maimaiplus", "maimai+", "m+", "mp", "pl", "pls", "mai+", "真", "0.15")),
    GREEN("maimai GreeN", "超", "grn", 12000, "#D0FD00",
        arrayOf("green", "g", "gr", "gre", "超", "0.2", "0.20")),
    GREEN_PLUS("maimai GreeN PLUS", "檄", "grp", 13000, "#D0FD00",
        arrayOf("greenplus", "g+", "gp", "grep", "gre+", "grn+", "gr+", "檄", "0.25")),
    ORANGE("maimai ORANGE", "橙", "org", 14000, "#FF6400",
        arrayOf("orange", "o", "or", "org", "橙", "0.3", "0.30")),
    ORANGE_PLUS("maimai ORANGE PLUS", "暁", "orp", 15000, "#FF6400",
        arrayOf("orangeplus", "op", "o+", "orgp", "orp", "or+", "org+", "晓", "暁", "0.35")),
    PINK("maimai PiNK", "桃", "pnk", 16000, "#FE006F",
        arrayOf("pink", "p", "pnk", "pk", "桃", "0.4", "0.40")),
    PINK_PLUS("maimai PiNK PLUS", "櫻", "pkp", 17000, "#FE006F",
        arrayOf("pinkplus", "pp", "p+", "pink+", "pk+", "pnk+", "樱", "樱", "0.45")),
    MURASAKI("maimai MURASAKi", "紫", "msk", 18000, "#A863A8",
        arrayOf("murasaki", "k", "ms", "紫", "0.5", "0.50")),
    MURASAKI_PLUS("maimai MURASAKi PLUS", "菫", "msp", 18500, "#A863A8",
        arrayOf("murasakiplus", "murasaki+", "kp", "k+", "msp", "ms+", "msk+", "菫", "0.55")),
    MILK("maimai MiLK", "白", "mlk", 19000, "#F4F4F4",
        arrayOf("milk", "white", "l", "mk", "白", "0.6", "0.60")),
    MILK_PLUS("maimai MiLK PLUS", "雪", "mkp", 19500, "#F4F4F4",
        arrayOf("milkplus", "snow", "l+", "lp", "mk+", "mlk+", "mkp", "mlkp", "雪", "0.65")),
    FINALE("maimai FiNALE", "輝", "fnl", 19900, "#C69C6E",
        arrayOf("finale", "final", "n", "fn", "0.7", "輝", "辉", "0.70")),

    DX("maimai でらっくす", "熊", "dx", 20000, "#7ECEF4",
        arrayOf("deluxe", "yuu", "kuma", "bear", "d", "dx", "dlx", "1.0", "舞萌dx2020", "舞萌dx", "舞萌", "20", "1.00")),
    DX_PLUS("maimai でらっくす PLUS", "華", "dxp", 20500, "#7ECEF4",
        arrayOf("deluxeplus", "hana", "ka", "d+", "dp", "dxp", "dlxp", "deluxe+", "dx+", "dlx+", "華", "华", "1.05")),
    SPLASH("maimai でらっくす Splash", "爽", "spl", 21000, "#79DDB4",
        arrayOf("splash", "sou", "sl", "s", "1.1", "爽", "舞萌dx2021", "舞萌2021", "2021", "21", "1.10")),
    SPLASH_PLUS("maimai でらっくす Splash PLUS", "煌", "spp", 21500, "#79DDB4",
        arrayOf("splashplus", "kou", "spp", "sp", "s+", "splp", "splash+", "sl+", "spl+", "1.15")),
    UNIVERSE("maimai でらっくす UNiVERSE", "宙", "uni", 22000, "#00A0E9",
        arrayOf("universe", "u", "un", "uv", "unv", "宙", "舞萌dx2022", "舞萌2022", "2022", "22", "1.2", "1.20")),
    UNIVERSE_PLUS("maimai でらっくす UNiVERSE PLUS", "星", "unp", 22500, "#00A0E9",
        arrayOf("universeplus", "star", "planet", "up", "u+", "unp", "uvp", "unvp", "universe+", "un+", "uv+", "uni+", "unv+", "星", "1.25")),
    FESTIVAL("maimai でらっくす FESTiVAL", "祭", "fes", 23000, "#C59EFE",
        arrayOf("festival", "matsuri", "sai", "f", "fs", "fst", "祭", "舞萌dx2023", "舞萌2023", "2023", "23", "1.3", "1.30")),
    FESTIVAL_PLUS("maimai でらっくす FESTiVAL PLUS", "祝", "fep", 23500, "#C59EFE",
        arrayOf("festivalplus", "iwau", "syuku", "fp", "f+", "fsp", "fesp", "festival+", "fs+", "fes+", "fst+", "1.35")),
    BUDDIES("maimai でらっくす BUDDiES", "双", "bud", 24000, "#FFCD43",
        arrayOf("buddies", "double", "b", "bd", "双", "舞萌dx2024", "舞萌2024", "2024", "24", "1.4", "1.40")),
    BUDDIES_PLUS("maimai でらっくす BUDDiES PLUS", "宴", "bup", 24500, "#FFCD43",
        arrayOf("buddiesplus", "utage", "b+", "bp", "bdp", "budp", "buddies+", "bd+", "bud+", "宴", "1.45")),
    PRISM("maimai でらっくす PRiSM", "鏡", "pri", 25000, "#7DFDDD",
        arrayOf("prism", "mirror", "r", "pr", "prs", "鏡", "镜", "舞萌dx2025", "舞萌2025", "2025", "25", "1.5", "1.50")),
    PRISM_PLUS("maimai でらっくす PRiSM PLUS", "稜", "prp", 25500, "#7DFDDD",
        arrayOf("prismplus", "rp", "r+", "prp", "prip", "prsp", "pr+", "pri+", "prs+", "prism+", "稜", "棱", "舞萌dx2026", "舞萌2026", "2026", "26", "1.55")),
    CIRCLE("maimai でらっくす CiRCLE", "丸", "cir", 26000, "#FF43B5",
        arrayOf("circle", "maru", "c", "ci", "cle", "丸", "1.6", "1.60")),
    CIRCLE_PLUS("maimai でらっくす CiRCLE PLUS", "珠", "cip", 26500, "#FF43B5",
        arrayOf("circleplus", "syu", "tama", "treasure", "cp", "c+", "cirp", "clep", "circle+", "ci+", "cir+", "cle+", "猪", "珠", "1.65")),
    MAGICAL("maimai でらっくす MAGiCAL", "魔", "mgc", 27000, "#60FA5E",
        arrayOf("magical", "ma", "a", "mg", "魔", "1.7", "1.70")),
    MAGICAL_PLUS("maimai でらっくす MAGiCAL PLUS", "奏", "mgp", 27500, "#60FA5E",
        arrayOf("magicalplus", "ap", "a+", "mgcp", "magical+", "mg+", "mgc+", "奏", "1.75")),
    ;

    companion object {
        val newestVersion = PRISM // 当前国服最新版本

        private val ALIAS_MAP: Map<String, MaiVersion> = buildMap {
            MaiVersion.entries.forEach { version ->
                if (version == DEFAULT) return@forEach

                if (version.full.isNotEmpty()) put(version.full.lowercase(), version)
                if (version.abbreviation.isNotEmpty()) put(version.abbreviation.lowercase(), version)
                if (version.code.isNotEmpty()) put(version.code.lowercase(), version)
                if (version.value > 0) put(version.value.toString(), version)

                version.aliases.forEach { alias ->
                    put(alias.lowercase(), version)
                }
            }
        }

        private val VERSIONS_ASC = MaiVersion.entries
            .filter { it.value > 0 }
            .sortedBy { it.value }

        private val VALUE_MAP: Map<Int, MaiVersion> = entries
            .filter { it != DEFAULT }
            .associateBy { it.value }

        fun getVersion(str: String?): MaiVersion {
            if (str == null) return DEFAULT

            val cleanStr = str
                .replace(REGEX_PLUS, "+")
                .replace(REGEX_SEPARATOR, "")
                .lowercase()

            ALIAS_MAP[cleanStr]?.let { return it }

            cleanStr.toIntOrNull()?.let { intVal ->
                VALUE_MAP[intVal]?.let { return it }
            }

            return DEFAULT
        }

        fun getNameList(versions: List<MaiVersion>): List<String> {
            return versions.map(MaiVersion::full)
        }

        fun getCodeList(versions: List<MaiVersion>): List<String> {
            return versions.map(MaiVersion::code)
        }

        fun getVersionFromAbbr(abbreviation: String): MaiVersion {
            return MaiVersion.entries.firstOrNull { it.abbreviation == abbreviation } ?: DEFAULT
        }

        private val VERSION_VALUES = VERSIONS_ASC.map { it.value }

        fun getVersionFromValue(value: Int): MaiVersion {
            val index = VERSION_VALUES.binarySearch(value)
            val insertPoint = if (index >= 0) index else -index - 2
            return if (insertPoint >= 0) VERSIONS_ASC[insertPoint] else DEFAULT
        }

        fun getVersionListOrNewest(str: String?): List<MaiVersion> {
            val l = getVersionList(str)

            return if (l.isEmpty() || l.contains(DEFAULT)) {
                listOf(newestVersion)
            } else {
                l
            }
        }

        fun getVersionList(str: String?): List<MaiVersion> {
            if (str == null) return listOf(DEFAULT)

            val out = mutableSetOf<MaiVersion>()
            val strList = str.split(REGEX_SEPARATOR_NO_SPACE)

            if (strList.isEmpty()) return listOf(DEFAULT)

            for (s in strList) {
                val v = MaiVersion.getVersion(s)

                if (v != DEFAULT) out.add(v)
            }

            if (out.isEmpty()) return listOf(DEFAULT)

            return out.toList()
        }

        fun List<MaiVersion>.listToString(): String {
            return this
                .filter { it != DEFAULT && it.full.isNotBlank() }
                .joinToString(separator = ", ", prefix = "[", postfix = "]", transform = MaiVersion::full)
        }
    }
}
