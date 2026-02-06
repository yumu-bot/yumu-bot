package com.now.nowbot.model.bili

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.now.nowbot.util.JacksonUtil
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

class BiliDanmaku {
    @field:JsonProperty("code")
    var code: Int = -1

    @field:JsonProperty("message")
    var message: String = ""

    // 无实义
    // @field:JsonProperty("ttl")
    //  var total: Int = 1

    // 如果为空，则请检查当前的 code
    @field:JsonProperty("data")
    var data: DanmakuData? = null

    data class DanmakuData(
        @field:JsonProperty("admin")
        val admin: List<Danmaku>,

        @field:JsonProperty("room")
        val room: List<Danmaku>,
    )

    data class Danmaku(
        @field:JsonProperty("text")
        val text: String,

        @set:JsonProperty("dm_type")
        @get:JsonProperty("type")
        var type: String,

        @field:JsonProperty("uid")
        val id: Long,

        @field:JsonProperty("nickname")
        val name: String,

        @field:JsonProperty("uname_color")
        val nameColor: String,

        @set:JsonProperty("timeline")
        @get:JsonIgnore
        var timeline: OffsetDateTime = OffsetDateTime.now(),

        @field:JsonProperty("isadmin")
        val admin: Byte,

        @field:JsonProperty("vip")
        val VIP: Byte,

        @field:JsonProperty("svip")
        val SVIP: Byte,

        /**
         * "medal": [],
         * "title": [
         *           "",
         *           ""
         *         ],
         *         "user_level": [
         *           0,
         *           0,
         *           9868950,
         *           "\u003e50000"
         *         ],
         */

        @set:JsonProperty("rank")
        @get:JsonIgnore
        var rankInt: Int,

        @field:JsonProperty("rnd")
        val randomSeed: Int,

        @field:JsonProperty("user_title")
        val title: String,

        /**
         *         "user_title": "",
         *         "guard_level": 0,
         *         "bubble": 0,
         *         "bubble_color": "",
         *         "lpl": 0,
         *         "yeah_space_url": "",
         *         "jump_to_url": "",
         *         "check_info": {
         *           "ts": 1748727790,
         *           "ct": "5F9C8C69"
         *         },
         */

        @field:JsonProperty("voice_dm_info")
        val voiceDirectMessage: VoiceDirectMessage,

        @field:JsonProperty("emoticon")
        val exclusiveEmotion: ExclusiveEmotion,

        @set:JsonProperty("emots")
        @get:JsonIgnore
        var emotionMap: Map<String, String>

    ) {
        @get:JsonProperty("timeline")
        val timelineString: String
            get() = formatter.format(timeline)


        @get:JsonProperty("rank")
        val rank: BiliUserRank
            get() = BiliUserRank.getBiliUserRank(rankInt)

        @get:JsonProperty("emots")
        val emotions: List<Emotion>
            get() = emotionMap.mapNotNull {
                try {
                    JacksonUtil.parseObject(it.value, Emotion::class.java)
                } catch (_: Exception) {
                    null
                }
            }
    }

    data class VoiceDirectMessage(
        @field:JsonProperty("voice_url")
        val url: String,

        @field:JsonProperty("file_format")
        val format: String,

        @field:JsonProperty("text")
        val text: String,

        @field:JsonProperty("file_duration")
        val fileDuration: Long,

        @field:JsonProperty("file_id")
        val fileID: String,
    )

    /**
     * 房间独有表情
     */
    data class ExclusiveEmotion(
        @field:JsonProperty("id")
        val id: Int,

        @field:JsonProperty("emoticon_unique")
        val unique: String,

        @field:JsonProperty("text")
        val text: String,

        @field:JsonProperty("perm")
        val permission: Byte,

        @field:JsonProperty("url")
        val url: String,

        @field:JsonProperty("in_player_area")
        val inPlayerArea: Byte,

        @field:JsonProperty("bulge_display")
        val bulgeDisplay: Byte,

        @field:JsonProperty("is_dynamic")
        val dynamic: Byte,

        @field:JsonProperty("height")
        val height: Int,

        @field:JsonProperty("width")
        val width: Int,

    )

    data class Emotion(
        @field:JsonProperty("count")
        val count: Int,

        @field:JsonProperty("descript")
        val description: String,

        @field:JsonProperty("emoji")
        val emoji: String,

        @field:JsonProperty("emoticon_id")
        val id: Int,

        @field:JsonProperty("emoticon_unique")
        val unique: String,

        @field:JsonProperty("url")
        val url: String,

        @field:JsonProperty("height")
        val height: Int,

        @field:JsonProperty("width")
        val width: Int,
        )

    companion object {
        private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    }
}