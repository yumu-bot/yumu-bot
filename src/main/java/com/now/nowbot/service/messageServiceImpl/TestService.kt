package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.model.enums.MaiVersion
import com.now.nowbot.model.enums.MaiVersion.*
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.divingFishApiService.MaimaiApiService
import com.now.nowbot.util.JacksonUtil
import org.springframework.stereotype.Service
import java.nio.file.Files
import java.nio.file.Path

@Service("TEST")
class TestService(private val maimaiApiService: MaimaiApiService) :
        MessageService<String> {
    override fun isHandle(
            event: MessageEvent,
            messageText: String,
            data: MessageService.DataValue<String>,
    ): Boolean {
        /*
        if (messageText.contains("!ymtest")) {
            data.value = messageText
            return true
        } else {
            return false
        }

        // if (messageText.contains("!test")) {
            if (false) {
            data.value = messageText
            return true
        } else {
            return false
        }

        val matcher = Instruction.MAI_SEARCH.matcher(messageText)

        if (!matcher.find()) {
            return false
        }

        data.value = ""
        return true
         */

        return false

    }

    override fun handleMessage(event: MessageEvent, param: String) {
        val l = maimaiApiService.getMaimaiSongLibrary().associateBy { it.songID }

        fun getVersionInt(maiVersion: MaiVersion): Int {
            return when(maiVersion) {
                DEFAULT -> 0
                MAIMAI -> 0
                PLUS -> 1
                GREEN -> 2
                GREEN_PLUS -> 3
                ORANGE -> 4
                ORANGE_PLUS -> 5
                PINK -> 6
                PINK_PLUS -> 7
                MURASAKI -> 8
                MURASAKI_PLUS -> 9
                MILK -> 10
                MILK_PLUS -> 11
                FINALE -> 12
                ALL_FINALE -> 0
                DX -> 13
                DX_PLUS -> 13
                SPLASH -> 14
                SPLASH_PLUS -> 15
                UNIVERSE -> 16
                UNIVERSE_PLUS -> 17
                FESTIVAL -> 18
                FESTIVAL_PLUS -> 19
                BUDDIES -> 20
                BUDDIES_PLUS -> 21
                PRISM -> 22
                PRISM_PLUS -> 23
                CIRCLE -> 24
                CIRCLE_PLUS -> 25
            }
        }

        val map = l
            .toList()
            .sortedBy { it.first }
            .associate { entry ->
                val s = entry.second

                entry.first.toString() to mapOf(
                    "name" to s.title,
                    "version" to getVersionInt(MaiVersion.getVersion(s.info.version))

                )
            }

        Files.write(Path.of("D://musicDB.json"), JacksonUtil.toJson(map).toByteArray(Charsets.UTF_8))

        /*
        // 获取 DX 歌曲
        val l = maimaiApiService.getMaimaiSongLibrary()

        val str = l.map { it.key }.sortedBy { it }.filter { it >= 10000 }.joinToString ( separator = "," )

        val file = str.toByteArray(StandardCharsets.UTF_8)

        event.replyFileInGroup(file, "dx_song_ids.txt")

         */

        /*
        val l = maimaiApiService.getMaimaiSongLibrary()

        for (e in l.values) {
            if (e.type == "DX" || e.charts.size < 5) continue

            if (e.star[4] < 14.0) continue

            val notes = e.charts[4].notes
            val dx = 3 * (notes.tap + notes.touch + notes.hold + notes.slide + notes.break_)

            if (dx >= 3600) println(e.title)
        }

         */
    }
}
