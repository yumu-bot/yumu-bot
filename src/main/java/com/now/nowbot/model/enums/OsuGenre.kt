package com.now.nowbot.model.enums

enum class OsuGenre(val id: Byte, val formalName: String, val aliases: List<String>) {
    UNSPECIFIED(1, "unspecified", listOf("u", "un", "uns", "unspecified", "未指定", "默认")),
    VIDEO_GAME(2, "video game", listOf("v", "vg", "vgm", "videogame", "电子游戏", "游戏")),
    ANIME(3, "anime", listOf("a", "an", "ani", "manga", "anime", "动漫")),
    ROCK(4, "rock", listOf("r", "rk", "rock", "摇滚")),
    POP(5, "pop", listOf("p", "pp", "pop", "流行")),
    OTHER(6, "other", listOf("o", "ot", "oth", "other", "其他")),
    NOVELTY(7, "novelty", listOf("n", "nv", "nvt", "novel", "novelty", "新奇", "鬼畜")),
    HIP_HOP(9, "hip hop", listOf("h", "hh", "hip", "hop", "hiphop", "嘻哈")),
    ELECTRONIC(10, "electronic", listOf("e", "el", "ele", "elect", "electro", "electric", "electronic", "电子", "舞曲")),
    METAL(11, "metal", listOf("m", "mt", "mtl", "metal", "金属")),
    CLASSICAL(12, "classical", listOf("c", "cl", "cls", "classic", "classical", "古典")),
    FOLK(13, "folk", listOf("f", "fk", "folk", "民谣")),
    JAZZ(14, "jazz", listOf("j", "jz", "jazz", "爵士"));

    companion object {
        // 缓存：ID -> 名字
        private val ID_MAP = entries.associateBy { it.id }

        // 缓存：所有别名 -> Enum实例
        private val ALIAS_MAP = entries.flatMap { genre ->
            genre.aliases.map { it to genre }
        }.toMap()

        fun getByte(genre: String?): Byte? {
            val key = genre?.trim()?.lowercase() ?: return null
            return ALIAS_MAP[key]?.id
        }

        fun getFormalName(id: Byte?): String {
            return ID_MAP[id]?.formalName ?: "unknown"
        }
    }
}