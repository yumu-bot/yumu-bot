package com.now.nowbot.service.MessageServiceImpl

import com.now.nowbot.model.JsonData.BeatMap
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.qq.message.MessageChain.MessageChainBuilder
import com.now.nowbot.qq.tencent.TencentMessageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.OsuApiService.OsuBeatmapApiService
import com.now.nowbot.util.Instruction
import java.net.URI
import okhttp3.internal.toLongOrDefault
import org.springframework.stereotype.Service
import java.util.regex.Pattern

@Service("GET_COVER")
class GetCoverService(val beatmapApiService: OsuBeatmapApiService? = null) :
        MessageService<GetCoverService.CoverParam>,
        TencentMessageService<GetCoverService.CoverParam> {
    @JvmRecord data class CoverParam(val type: Type, val bids: MutableList<Long>)

    enum class Type {
        CARD,
        CARD_2X,
        COVER,
        COVER_2X,
        LIST,
        LIST_2X,
        SLIM_COVER,
        SLIM_COVER_2X
    }

    @Throws(Throwable::class)
    override fun isHandle(
            event: MessageEvent,
            messageText: String,
            data: DataValue<CoverParam>
    ): Boolean {
        val matcher = Instruction.GET_COVER.matcher(messageText)
        if (!matcher.find()) {
            return false
        }

        val type =
                when (matcher.group("type")) {
                    "list",
                    "l",
                    "l1" -> Type.LIST
                    "list2",
                    "list2x",
                    "list@2x",
                    "l2" -> Type.LIST_2X
                    "c",
                    "card",
                    "c1" -> Type.CARD
                    "card2",
                    "card2x",
                    "card@2x",
                    "c2" -> Type.CARD_2X
                    "slim",
                    "slimcover",
                    "s",
                    "s1" -> Type.SLIM_COVER
                    "slim2",
                    "slim2x",
                    "slim@2x",
                    "slimcover2",
                    "slimcover2x",
                    "slimcover@2x",
                    "s2" -> Type.SLIM_COVER_2X
                    "2",
                    "cover2",
                    "cover2x",
                    "cover@2x",
                    "o2" -> Type.COVER_2X
                    null, -> Type.COVER
                    else -> Type.COVER
                }

        val bids = matcher.group("data")

        data.value = CoverParam(type, parseDataString(bids))
        return true
    }

    @Throws(Throwable::class)
    override fun HandleMessage(event: MessageEvent, param: CoverParam) {
        val beatmaps = getBeatMap(param.bids, beatmapApiService)
        val messageChains = getMessageChains(param.type, beatmaps)

        for (msg in messageChains) {
            event.reply(msg)
            Thread.sleep(1000)
        }
    }

    companion object {
        private fun parseDataString(
                dataStr: String,
        ): MutableList<Long> {
            val dataStrArray = dataStr.trim().split(Pattern.compile("[,，|:：\\s]+"), 0)
            val list = mutableListOf<Long>()

            if (dataStrArray.isNullOrEmpty()) return mutableListOf()

            for (str in dataStrArray) {
                if (str.isBlank()) continue
                val bid = str.toLongOrDefault(0)
                list.add(bid)
            }

            return list
        }

        private fun getBeatMap(
                bids: MutableList<Long>,
                beatmapApiService: OsuBeatmapApiService?
        ): List<BeatMap> {
            val list = mutableListOf<BeatMap>()

            if (bids.isNullOrEmpty()) return listOf()

            for (bid in bids) {
                list.add(beatmapApiService!!.getBeatMapFromDataBase(bid))
            }

            return list
        }

        private fun getMessageChains(type: Type, beatmaps: List<BeatMap>): List<MessageChain> {
            val list = mutableListOf<MessageChain>()
            var builder = MessageChainBuilder()

            for (i in 1..beatmaps.size) {
                val b = beatmaps.get(i - 1)

                val covers = b.beatMapSet!!.covers
                val url =
                        when (type) {
                            Type.LIST -> covers.list
                            Type.LIST_2X -> covers.list2x
                            Type.CARD -> covers.card
                            Type.CARD_2X -> covers.card2x
                            Type.SLIM_COVER -> covers.slimcover
                            Type.SLIM_COVER_2X -> covers.slimcover2x
                            Type.COVER_2X -> covers.cover2x
                            else -> covers.cover
                        }
                val imageUrl = URI.create(url).toURL()

                // qq 一次性只能发 20 张图
                if ((i - 1) >= 20 && (i - 1) % 20 == 0) {
                    Thread.sleep(1000L)
                    list.add(builder.build())
                    builder = MessageChainBuilder()
                }

                builder.addImage(imageUrl)
            }

            if (builder.isNotEmpty) {
                list.add(builder.build())
            }

            return list
        }
    }

    override fun accept(event: MessageEvent, messageText: String): CoverParam? {
        return null
    }

    override fun reply(event: MessageEvent, param: CoverParam): MessageChain {
        val beatmaps = getBeatMap(param.bids, beatmapApiService)
        val messageChains = getMessageChains(param.type, beatmaps)

        return messageChains.first()
    }
}
