package com.now.nowbot.model.json

import com.fasterxml.jackson.annotation.JsonProperty
import com.now.nowbot.model.enums.MaiVersion

class MaiBestScore {
    // 看用户段位信息，其中0-10对应初学者-十段，11-20对应真初段-真十段，21-22对应真皆传-里皆传
    @JsonProperty("additional_rating") var dan: Int = 0

    // b40 和 b50 一样，但区别是 dx 其实是最新版本的 b15，sd 是综合的 b35
    // 这就是 BP，在所有成绩里没有
    @JsonProperty("charts") var charts: Charts = Charts()

    // 这就是所有成绩，在 BP 里没有
    @JsonProperty("records") var records = mutableListOf<MaiScore>()

    data class Charts(
            @JsonProperty("dx") val deluxe: MutableList<MaiScore> = mutableListOf(),
            @JsonProperty("sd") val standard: MutableList<MaiScore> = mutableListOf(),
    )

    // 在游戏里的名字
    @JsonProperty("nickname") var name: String = ""

    // 牌子信息，比如“舞神”
    @JsonProperty("plate")
    // @JsonDeserialize(using = PlateJsonDeserializer::class)
    var plate: String? = ""

    // PP，理论上限 1w6 多
    var rating: Int? = 0

    // 没有用
    // @JsonIgnoreProperties("user_general_data")
    // String general;
    // 在查分器里的名字
    @JsonProperty("username") var probername: String = ""

    @JvmRecord
    data class User(
            val name: String?,
            val probername: String?,
            val dan: Int?,
            val plate: String?,
            val rating: Int?,
            val base: Int?,
            val additional: Int?,
            val platename: String?,
    )

    fun getUser(): User {
        val best35 = this.charts.standard.sumOf { it.rating }
        val best15 = this.charts.deluxe.sumOf { it.rating }
        val plateName = getPlateName(this.plate)

        return User(this.name, this.probername, this.dan, this.plate, this.rating, best35, best15, plateName)
    }

    private fun getPlateName(plate: String?) : String {
        if (plate == null || plate.length < 2) return ""

        val code = MaiVersion.getVersionFromAbbr(plate.substring(0, 1)).let { if (it != MaiVersion.DEFAULT) {
            it.code
        } else {
            return ""
        }}

        val rank = when (plate.substring(1)) {
            "神" -> "ap"
            "将" -> "sss"
            "极", "極" -> "fc"
            "舞", "舞舞" -> "fdx" // 舞舞

            else -> return ""
        }

        return code + rank
    }

    /*
    class PlateJsonDeserializer : JsonDeserializer<String?>() {
        @Throws(IOException::class)
        override fun deserialize(p: JsonParser, text: DeserializationContext): String {
            return p.text ?: ""
        }
    }

     */
}
