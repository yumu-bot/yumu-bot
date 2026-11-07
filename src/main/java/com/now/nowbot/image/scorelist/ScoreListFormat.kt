package com.now.nowbot.image.scorelist

enum class ScoreListOutputFormat {
    DEFAULT,
    ADVANCED,
}

data class ScoreListRequestPlan(
    val dataCompact: Boolean,
    val format: ScoreListOutputFormat,
)

data class ScoreListImageRequest<T>(
    val data: T,
    val format: ScoreListOutputFormat,
)

object ScoreListFormat {
    fun imageRequest(compact: Boolean): ScoreListRequestPlan {
        return ScoreListRequestPlan(
            dataCompact = false,
            format = if (compact) ScoreListOutputFormat.ADVANCED else ScoreListOutputFormat.DEFAULT,
        )
    }

    fun svgRequest(compact: Boolean): ScoreListRequestPlan {
        return ScoreListRequestPlan(dataCompact = compact, format = ScoreListOutputFormat.DEFAULT)
    }

    fun fixedAdvanced(): ScoreListOutputFormat = ScoreListOutputFormat.ADVANCED

    fun fixedDefault(): ScoreListOutputFormat = ScoreListOutputFormat.DEFAULT
}
