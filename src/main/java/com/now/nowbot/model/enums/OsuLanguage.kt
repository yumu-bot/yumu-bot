package com.now.nowbot.model.enums

enum class OsuLanguage(val id: Byte, val formalName: String, val aliases: List<String>) {
    UNSPECIFIED(1, "unspecified", listOf("u", "un", "uns", "unspecified", "未指定", "默认")),
    ENGLISH(2, "english", listOf("e", "en", "gb", "eng", "gbr", "england", "english", "英语", "英文", "英")),
    JAPANESE(3, "japanese", listOf("j", "ja", "jpn", "japan", "japanese", "日语", "日文", "日本", "日")),
    CHINESE(4, "chinese", listOf("c", "cn", "chn", "china", "chinese", "中文", "汉语", "中", "汉")),
    INSTRUMENTAL(5, "instrumental", listOf("i", "in", "ins", "instrument", "instrumental", "器乐", "纯音乐", "器", "纯", "音")),
    KOREAN(6, "korean", listOf("k", "kr", "kor", "korea", "korean", "韩语", "韩文", "韩")),
    FRENCH(7, "french", listOf("f", "fr", "fra", "france", "french", "法语", "法文", "法")),
    GERMAN(8, "german", listOf("g", "ge", "ger", "germany", "german", "德语", "德文", "德")),
    SWEDISH(9, "swedish", listOf("w", "sw", "swe", "sweden", "swedish", "瑞典语", "瑞典文", "瑞典", "瑞")),
    SPANISH(10, "spanish", listOf("s", "sp", "esp", "spa", "spain", "spanish", "西班牙语", "西班牙文", "西班牙", "西", "西语")),
    ITALIAN(11, "italian", listOf("t", "it", "ita", "italy", "italian", "意大利语", "意大利文", "意大利", "意")),
    RUSSIAN(12, "russian", listOf("r", "ru", "rus", "russia", "russian", "俄语", "俄文", "俄罗斯", "俄")),
    POLISH(13, "polish", listOf("p", "po", "pol", "poland", "polish", "波兰语", "波兰文", "波兰", "波")),
    OTHER(14, "other", listOf("o", "ot", "oth", "any", "other", "others", "其他"));

    companion object {
        private val ID_MAP = entries.associateBy { it.id }
        private val ALIAS_MAP = entries.flatMap { lang ->
            lang.aliases.map { it to lang }
        }.toMap()

        fun getByte(language: String?): Byte? {
            val key = language?.trim()?.lowercase() ?: return null
            return ALIAS_MAP[key]?.id
        }

        fun getFormalName(id: Byte?): String {
            return ID_MAP[id]?.formalName ?: "unknown"
        }
    }
}