package com.now.nowbot.model.bili


import com.fasterxml.jackson.annotation.JsonIgnore
import tools.jackson.databind.PropertyNamingStrategies
import tools.jackson.databind.annotation.JsonNaming
import com.fasterxml.jackson.annotation.JsonProperty
import com.now.nowbot.util.DataUtil
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
class BiliStreamer {
    @field:JsonProperty("code")
    var code: Int = -1

    @field:JsonProperty("message")
    var message: String = ""

    // 如果为空，则请检查当前的 code
    @field:JsonProperty("data")
    var data: StreamerData? = null

    data class StreamerData(
        @field:JsonProperty("info")
        val info: StreamerInfo,

        @field:JsonProperty("exp")
        val experience: StreamerExperience,

        @field:JsonProperty("follower_num")
        val followerCount: Long,

        @field:JsonProperty("room_id")
        val roomID: Long,

        @field:JsonProperty("medal_name")
        val medalName: String,

        @field:JsonProperty("glory_count")
        val gloryCount: Int,

        // 头像挂件编号
        @field:JsonProperty("pendant")
        val pendant: String,

        @field:JsonProperty("link_group_num")
        val linkGroup: Long,

        @field:JsonProperty("room_news")
        val news: RoomNews,
    )

    data class StreamerInfo(
        @field:JsonProperty("uid")
        val id: Long,

        @field:JsonProperty("uname")
        val username: String,

        @field:JsonProperty("face")
        val avatar: String,

        @field:JsonProperty("official_verify")
        val officialVerify: StreamerOfficialVerify,

        @field:JsonProperty("gender")
        val gender: Byte,
    )
    data class StreamerOfficialVerify(
        @field:JsonProperty("type")
        val type: Byte,

        @field:JsonProperty("desc")
        val description: String,
    ) {
        @get:JsonProperty("is_verify")
        val isVerify
            get() = type >= 0
    }

    data class StreamerExperience(
        @field:JsonProperty("master_level")
        val masterLevel: StreamerMasterLevel
    )


    data class StreamerMasterLevel(
        @field:JsonProperty("level")
        val level: Byte,

        @set:JsonProperty("color")
        var colorInt: Int,

        @field:JsonProperty("current")
        val current: List<Int>,

        @field:JsonProperty("next")
        val next: List<Int>
    ) {
        @get:JsonProperty("color")
        val color: String
            get() = DataUtil.int2hex(colorInt)
    }

    data class RoomNews(
        @field:JsonProperty("content")
        val content: String,

        @set:JsonProperty("ctime")
        @get:JsonIgnore
        var time: OffsetDateTime = OffsetDateTime.now(),
    ) {
        // 给 js 用
        @get:JsonProperty("ctime")
        private val timeString: String
            get() = formatter.format(time)
    }

    companion object {
        private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    }
}