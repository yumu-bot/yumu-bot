package com.now.nowbot.model.jsonData

import com.fasterxml.jackson.annotation.JsonProperty
import com.now.nowbot.model.jsonData.ChuBestPerformance.Records

class ChuBestPerformance {
    // 在游戏里的名字
    @JsonProperty("nickname") var name: String = ""

    // best30 + recent10
    // 这就是 BP
    @JsonProperty("records") var records: Records? = null

    data class Records(
        @JsonProperty("b30") val best30: MutableList<ChuScore> = mutableListOf<ChuScore>(),
        @JsonProperty("r10") val recent10: MutableList<ChuScore> = mutableListOf<ChuScore>(),
    )

    // 在查分器里的名字
    @JsonProperty("username") var probername: String = ""
}
