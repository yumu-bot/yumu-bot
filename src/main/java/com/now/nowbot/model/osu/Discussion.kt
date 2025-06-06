package com.now.nowbot.model.osu

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import com.now.nowbot.model.osu.OsuUser.Companion.merge2OsuUserList
import com.now.nowbot.util.JacksonUtil

@JsonInclude(JsonInclude.Include.NON_NULL) @JsonIgnoreProperties(ignoreUnknown = true)
data class Discussion(
    @JsonProperty("beatmaps")
    var beatMaps: List<Beatmap>? = null,

    @JsonProperty("reviews_config")
    var reviewsConfig: ReviewsConfig,

    @JsonProperty("users")
    var users: List<OsuUser> = listOf(),

    @JsonProperty("discussions")
    var discussions: List<DiscussionDetails> = listOf(),

    @JsonProperty("included_discussions")
    var includedDiscussions: List<DiscussionDetails> = listOf(),

    @JsonProperty("cursor")
    var cursor: Cursor? = null,

    @JsonProperty("cursor_string")
    var cursorString: String? = null,
) {
    @JsonIgnore
    var beatMapSet: Beatmapset? = null

    @JsonProperty("beatmapsets") fun parseBeatMapSet(data: JsonNode) {
        if (!data.hasNonNull("beatmapsets") || !data["beatmapsets"].isArray) return
        beatMapSet = JacksonUtil.parseObject(
            data["beatmapsets"].first(),
            Beatmapset::class.java
        )
    }

    data class ReviewsConfig(
        @JsonProperty("max_blocks")
        val maxBlocks: Int
    )

    data class Cursor(
        @JsonProperty("page") val page: Int,
        @JsonProperty("limit") val limit: Int,
    )

    fun mergeDiscussion(that: Discussion, sort: String?) {
        this.cursorString = that.cursorString
        this.cursor = that.cursor

        // discussions, includedDiscussions合并
        if ("id_asc" == sort) {
            if (that.discussions.isNotEmpty()) {
                if (this.discussions.isNotEmpty()) {
                    that.discussions += this.discussions
                } else {
                    this.discussions = that.discussions
                }
            }

            if (that.includedDiscussions.isNotEmpty()) {
                if (this.includedDiscussions.isNotEmpty()) {
                    that.includedDiscussions += this.includedDiscussions
                } else {
                    this.includedDiscussions = that.includedDiscussions
                }
            }
        } else {
            if (that.discussions.isNotEmpty()) {
                if (this.discussions.isNotEmpty()) {
                    this.discussions += that.discussions
                } else {
                    this.discussions = that.discussions
                }
            }
            if (that.includedDiscussions.isNotEmpty()) {
                if (this.includedDiscussions.isNotEmpty()) {
                    this.includedDiscussions += that.includedDiscussions
                } else {
                    this.includedDiscussions = that.includedDiscussions
                }
            }
        }

        // user去重
        this.users = merge2OsuUserList(users, that.users)
    }

    /**
     * 把谱面难度名字嵌入到 DiscussionDetails 里
     * @param diffs 难度 bid 和难度名的 map
     */
    fun addDifficulty4DiscussionDetails(diffs: Map<Long, String>) {
        discussions.forEach { it.difficulty = diffs[it.beatmapID] }
    }

    companion object {
        /**
         * 置顶未解决的讨论
         */
        fun toppingUnsolvedDiscussionDetails(discussions: List<DiscussionDetails>): List<DiscussionDetails> {
            val unsolved = discussions.filter {
                val c = it.canBeResolved
                val r = it.resolved

                c && !r
            }

            val canBeResolved = discussions - unsolved.toSet()

            return unsolved + canBeResolved
        }
    }
}
