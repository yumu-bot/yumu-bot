package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.model.enums.MaiVersion
import com.now.nowbot.model.json.MaiSong
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.divingFishApiService.MaimaiApiService
import com.now.nowbot.util.DataUtil
import com.now.nowbot.util.Instruction
import org.springframework.stereotype.Service

// @Service("TEST")
@Service("MAI_SCORE")
class TestService(private val maimaiApiService: MaimaiApiService) : MessageService<String> {
    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: MessageService.DataValue<String>
    ): Boolean {
        /*
        if (false) {
            data.value = messageText
            return true
        } else {
            return false
        }

         */

        val matcher = Instruction.MAI_SCORE.matcher(messageText)

        if (!matcher.find()) {
            return false
        }

        data.value = matcher.group("name")
        return true
    }

    override fun HandleMessage(event: MessageEvent, text: String) {
        val songs = maimaiApiService.maimaiSongLibrary

        val result = mutableMapOf<Double, MaiSong>()

        for (s in songs) {
            val similarity = DataUtil.getStringSimilarity(text, s.value.title)

            if (similarity >= 0.3) {
                result[similarity] = s.value
            }
        }

        if (result.isEmpty()) {
            event.reply("没有找到结果！")
            return
        }

        val sort = result.toSortedMap().reversed()

        val sb = StringBuilder("\n")

        var i = 1
        for(e in sort) {
            val code = MaiVersion.getCodeList(MaiVersion.getVersionList(e.value.info.version)).first()
            val category = MaiVersion.getCategoryAbbreviation(e.value.info.genre)

            sb.append("#${i}:").append(" ")
                .append(String.format("%.0f", e.key * 100)).append("%").append(" ")
                .append("[${e.value.songID}]").append(" ")
                .append(e.value.title).append(" ")
                .append("[${code}]").append(" / ")
                .append("[${category}]").append("\n")

            i++

            if (i >= 6) break
        }

        val img = maimaiApiService.getMaimaiCover((sort[sort.firstKey()]?.songID ?: 0).toLong())

        sb.removeSuffix("\n")

        event.reply(MessageChain.MessageChainBuilder().addText("搜索结果：\n").addImage(img).addText(sb.toString()).build())
    }
}
