package com.now.nowbot.model.bili

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.now.nowbot.util.DataUtil
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

class BiliStreamer {
    @JsonProperty("code")
    var code: Int = -1

    @JsonProperty("message")
    var message: String = ""

    // 如果为空，则请检查当前的 code
    @JsonProperty("data")
    var data: StreamerData? = null

    data class StreamerData(
        @JsonProperty("info")
        val info: StreamerInfo,

        @JsonProperty("exp")
        val experience: StreamerExperience,

        @JsonProperty("follower_num")
        val followerCount: Long,

        @JsonProperty("room_id")
        val roomID: Long,

        @JsonProperty("medal_name")
        val medalName: String,

        @JsonProperty("glory_count")
        val gloryCount: Int,

        // 头像挂件编号
        @JsonProperty("pendant")
        val pendant: String,

        @JsonProperty("link_group_num")
        val linkGroup: Long,

        @JsonProperty("room_news")
        val news: RoomNews,
    )

    data class StreamerInfo(
        @JsonProperty("uid")
        val id: Long,

        @JsonProperty("uname")
        val username: String,

        @JsonProperty("face")
        val avatar: String,

        @JsonProperty("official_verify")
        val officialVerify: StreamerOfficialVerify,

        @JsonProperty("gender")
        val gender: Byte,
    )
    data class StreamerOfficialVerify(
        @JsonProperty("type")
        val type: Byte,

        @JsonProperty("desc")
        val description: String,
    ) {
        @get:JsonProperty("is_verify")
        val isVerify
            get() = type >= 0
    }

    data class StreamerExperience(
        @JsonProperty("master_level")
        val masterLevel: StreamerMasterLevel
    )


    data class StreamerMasterLevel(
        @JsonProperty("level")
        val level: Byte,

        @set:JsonProperty("color")
        var colorInt: Int,

        @JsonProperty("current")
        val current: List<Int>,

        @JsonProperty("next")
        val next: List<Int>
    ) {
        @get:JsonProperty("color")
        val color: String
            get() = DataUtil.int2hex(colorInt)
    }

    data class RoomNews(
        @JsonProperty("content")
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