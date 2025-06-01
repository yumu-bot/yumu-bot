package com.now.nowbot.model.bili

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.now.nowbot.util.JacksonUtil
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

class BiliDanmaku {
    @JsonProperty("code")
    var code: Int = -1

    @JsonProperty("message")
    var message: String = ""

    // 无实义
    // @JsonProperty("ttl")
    //  var total: Int = 1

    // 如果为空，则请检查当前的 code
    @JsonProperty("data")
    var data: DanmakuData? = null

    data class DanmakuData(
        @JsonProperty("admin")
        val admin: List<Danmaku>,

        @JsonProperty("room")
        val room: List<Danmaku>,
    )

    data class Danmaku(
        @JsonProperty("text")
        val text: String,

        @set:JsonProperty("dm_type")
        @get:JsonProperty("type")
        var type: String,

        @JsonProperty("uid")
        val id: Long,

        @JsonProperty("nickname")
        val name: String,

        @JsonProperty("uname_color")
        val nameColor: String,

        @set:JsonProperty("timeline")
        @get:JsonIgnoreProperties
        var timeline: OffsetDateTime = OffsetDateTime.now(),

        @JsonProperty("isadmin")
        val admin: Byte,

        @JsonProperty("vip")
        val VIP: Byte,

        @JsonProperty("svip")
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
        @get:JsonIgnoreProperties
        var rankInt: Int,

        @JsonProperty("rnd")
        val randomSeed: Int,

        @JsonProperty("user_title")
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

        @JsonProperty("voice_dm_info")
        val voiceDirectMessage: VoiceDirectMessage,

        @JsonProperty("emoticon")
        val exclusiveEmotion: ExclusiveEmotion,


        @set:JsonProperty("emots")
        @get:JsonIgnoreProperties
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
                } catch (e: Exception) {
                    null
                }
            }
    }

    data class VoiceDirectMessage(
        @JsonProperty("voice_url")
        val url: String,

        @JsonProperty("file_format")
        val format: String,

        @JsonProperty("text")
        val text: String,

        @JsonProperty("file_duration")
        val fileDuration: Long,

        @JsonProperty("file_id")
        val fileID: String,
    )

    /**
     * 房间独有表情
     */
    data class ExclusiveEmotion(
        @JsonProperty("id")
        val id: Int,

        @JsonProperty("emoticon_unique")
        val unique: String,

        @JsonProperty("text")
        val text: String,

        @JsonProperty("perm")
        val permission: Byte,

        @JsonProperty("url")
        val url: String,

        @JsonProperty("in_player_area")
        val inPlayerArea: Byte,

        @JsonProperty("bulge_display")
        val bulgeDisplay: Byte,

        @JsonProperty("is_dynamic")
        val dynamic: Byte,

        @JsonProperty("height")
        val height: Int,

        @JsonProperty("width")
        val width: Int,

    )

    data class Emotion(
        @JsonProperty("count")
        val count: Int,

        @JsonProperty("descript")
        val description: String,

        @JsonProperty("emoji")
        val emoji: String,

        @JsonProperty("emoticon_id")
        val id: Int,

        @JsonProperty("emoticon_unique")
        val unique: String,

        @JsonProperty("url")
        val url: String,

        @JsonProperty("height")
        val height: Int,

        @JsonProperty("width")
        val width: Int,
        )

    companion object {
        private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    }
}