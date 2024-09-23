package com.now.nowbot.model.jsonData

import com.fasterxml.jackson.annotation.JsonProperty
import com.now.nowbot.model.jsonData.MaiBestPerformance.Charts
import java.util.Objects

class MaiBestPerformance {
    // 看用户段位信息，其中0-10对应初学者-十段，11-20对应真初段-真十段，21-22对应真皆传-里皆传
    @JsonProperty("additional_rating") var dan: Int = 0

    // b40 和 b50 一样，但区别是 dx 其实是最新版本的 b15，sd 是综合的 b35
    // 这就是 BP
    @JsonProperty("charts") var charts: Charts = Charts()

    data class Charts(
        @JsonProperty("dx") val deluxe: MutableList<MaiScore> = mutableListOf<MaiScore>(),
        @JsonProperty("sd") val standard: MutableList<MaiScore> = mutableListOf<MaiScore>(),
    )

    // 在游戏里的名字
    @JsonProperty("nickname") var name: String = ""

    // 牌子信息，比如“舞神”
    var plate: String = ""

    // PP，理论上限 1w6 多
    var rating: Int = 0

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
    )

    fun getUser(): User {
        val best35 =
            this.charts.standard
                .stream()
                .map<Int> { obj: MaiScore? -> obj!!.getRating() }
                .filter { obj: Int? -> Objects.nonNull(obj) }
                .reduce { a: Int, b: Int -> Integer.sum(a, b) }
                .orElse(0)
        val best15 =
            this.charts.deluxe
                .stream()
                .map<Int> { obj: MaiScore? -> obj!!.getRating() }
                .filter { obj: Int? -> Objects.nonNull(obj) }
                .reduce { a: Int, b: Int -> Integer.sum(a, b) }
                .orElse(0)

        return User(this.name, this.probername, this.dan, this.plate, this.rating, best35, best15)
    }
}
