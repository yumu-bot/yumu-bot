package com.now.nowbot.model.maimai

import com.fasterxml.jackson.annotation.JsonProperty

data class LxCollection(
    val id: Int,
    val name: String,
    val color: String? = null,
    val level: Int? = null,
    val required: CollectionRequired? = null,

    ) {
    data class CollectionRequired(
        @JsonProperty("difficulties")
        val difficulties: List<Byte>?,

        @JsonProperty("full_combo")
        val combo: String?,

        @JsonProperty("full_chain")
        val chain: String?,

        @JsonProperty("rank")
        val rank: String,

        @JsonProperty("collection_required_song")
        val songs: CollectionRequiredSong?,

        @JsonProperty("completed")
        val completed: Boolean?,

        )

    data class CollectionRequiredSong(
        val id: Long,

        val title: String,

        // val type: SongType,

        val completed: Boolean? = false,

        val completedDifficulties: List<Byte>? = listOf()
    )
}
