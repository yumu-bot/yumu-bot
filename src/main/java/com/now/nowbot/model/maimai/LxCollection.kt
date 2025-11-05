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
        @field:JsonProperty("difficulties")
        val difficulties: List<Byte>?,

        @field:JsonProperty("full_combo")
        val combo: String?,

        @field:JsonProperty("full_chain")
        val chain: String?,

        @field:JsonProperty("rank")
        val rank: String,

        @field:JsonProperty("collection_required_song")
        val songs: CollectionRequiredSong?,

        @field:JsonProperty("completed")
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
