package com.now.nowbot.model.maimai

import com.fasterxml.jackson.annotation.JsonProperty

class MaiVersionScore {
    @JsonProperty("verlist") var scores: MutableList<MaiScoreSimplified> = mutableListOf()
}
