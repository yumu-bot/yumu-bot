package com.now.nowbot.service.web

import com.fasterxml.jackson.annotation.JsonProperty
import com.now.nowbot.model.multiplayer.RoomInfo
import com.now.nowbot.util.DataUtil
import com.now.nowbot.util.JacksonUtil
import java.util.regex.Pattern

data class Quickplay(
    @field:JsonProperty("rooms")
    val rooms: List<RoomInfo> = emptyList(),

    @field:JsonProperty("type_group")
    val typeGroup: String = "quickplay",

    @field:JsonProperty("cursor_string")
    val cursorString: String? = null,

//    @field:JsonProperty("cursor")
//    val cursor: SearchCursor? = null,
)

data class QuickplaySummary(
    @field:JsonProperty("active")
    val active: Quickplay = Quickplay(),

    @field:JsonProperty("ended")
    val ended: Quickplay = Quickplay(),
)

private val quickplayDataPattern: Pattern =
    Pattern.compile("(?s)<script id=\"json-user-multiplayer-index\" type=\"application/json\">(?<json>.*?)</script>")

fun parseQuickplay(html: String): QuickplaySummary? {
    val matcher = quickplayDataPattern.matcher(html)

    if (matcher.find()) {
        val json = DataUtil.unescapeHTML(matcher.group("json"))

        return JacksonUtil.parseObject<QuickplaySummary>(json)
    }

    return null
}