package com.now.nowbot.model.maimai

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonProperty

class LxMaiCollection(
    @field:JsonProperty("id")
    var collectionID: Int = 0,

    @field:JsonProperty("name")
    var name: String = "",

    // 仅 Trophy 包含。可能为 normal、bronze、silver、gold、rainbow
    @field:JsonProperty("color")
    var color: String? = null,

    @field:JsonProperty("description")
    var description: String? = null,

    @field:JsonProperty("genre")
    var genre: String? = null,

    @field:JsonProperty("required")
    var required: List<LxMaiCollectionRequired>? = null
) {
    @field:JsonProperty("type")
    // 需要自己设置
    var type: String = ""
}

class LxMaiCollectionRequired(

    @field:JsonProperty("difficulties")
    var difficulties: Set<Int>? = null,

    @field:JsonProperty("rate")
    var rate: String? = null,

    @field:JsonProperty("combo")
    @field:JsonAlias("fc")
    var combo: String? = null,

    @field:JsonProperty("sync")
    @field:JsonAlias("fs")
    var sync: String? = null,

    @field:JsonProperty("songs")
    var songs: List<LxMaiCollectionRequiredSong>? = null
)

class LxMaiCollectionRequiredSong(
    @field:JsonProperty("id")
    var songID: Int = 0,

    @field:JsonProperty("title")
    var title: String = "",

    @field:JsonProperty("type")
    var type: String = "",
)