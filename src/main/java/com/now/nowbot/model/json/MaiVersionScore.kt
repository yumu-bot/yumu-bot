package com.now.nowbot.model.json

import com.fasterxml.jackson.annotation.JsonProperty

class MaiVersionScore {
    @JsonProperty("verlist") var scores: MutableList<MaiScoreLite> = mutableListOf()
}
