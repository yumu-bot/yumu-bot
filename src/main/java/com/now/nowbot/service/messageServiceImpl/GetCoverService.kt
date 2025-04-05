package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.model.json.BeatMap
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.qq.message.MessageChain.MessageChainBuilder
import com.now.nowbot.qq.tencent.TencentMessageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.service.osuApiService.OsuBeatmapMirrorApiService
import com.now.nowbot.throwable.GeneralTipsException
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.command.REG_SEPERATOR
import okhttp3.internal.toLongOrDefault
import org.springframework.stereotype.Service
import java.net.URI

@Service("GET_COVER") class GetCoverService(private val beatmapApiService: OsuBeatmapApiService, private val beatmapMirrorApiService: OsuBeatmapMirrorApiService) :
    MessageService<GetCoverService.CoverParam>, TencentMessageService<GetCoverService.CoverParam> {
    @JvmRecord data class CoverParam(val type: Type, val bids: List<Long>)

    enum class Type {
        RAW, CARD, CARD_2X, COVER, COVER_2X, LIST, LIST_2X, SLIM_COVER, SLIM_COVER_2X
    }

    @Throws(Throwable::class) override fun isHandle(
        event: MessageEvent, messageText: String, data: DataValue<CoverParam>
    ): Boolean {
        val matcher = Instruction.GET_COVER.matcher(messageText)
        val matcher2 = Instruction.GET_BG.matcher(messageText)

        if (matcher.find()) {
            val type = when (matcher.group("type")) {
                "raw", "r", "full", "f", "background", "b" -> Type.RAW
                "list", "l", "l1" -> Type.LIST
                "list2", "list2x", "list@2x", "l2" -> Type.LIST_2X
                "c", "card", "c1" -> Type.CARD
                "card2", "card2x", "card@2x", "c2" -> Type.CARD_2X
                "slim", "slimcover", "s", "s1" -> Type.SLIM_COVER
                "slim2", "slim2x", "slim@2x", "slimcover2", "slimcover2x", "slimcover@2x", "s2" -> Type.SLIM_COVER_2X
                "2", "cover2", "cover2x", "cover@2x", "o2" -> Type.COVER_2X
                null -> Type.COVER
                else -> Type.COVER
            }

            val dataStr: String? = matcher.group("data")
            if (dataStr.isNullOrBlank()) throw GeneralTipsException(GeneralTipsException.Type.G_Null_BID)
            val bids = parseDataString(dataStr)

            data.value = CoverParam(type, bids)
            return true
        } else if (matcher2.find()) {

            val dataStr: String? = matcher2.group("data")
            if (dataStr.isNullOrBlank()) throw GeneralTipsException(GeneralTipsException.Type.G_Null_BID)
            val bids = parseDataString(dataStr)

            data.value = CoverParam(Type.RAW, bids)
            return true
        } else {
            return false
        }
    }

    @Throws(Throwable::class) override fun HandleMessage(event: MessageEvent, param: CoverParam) {
        val messageChains: List<MessageChain>

        if (param.type == Type.RAW) {
            messageChains = getMessageChains(param.bids, beatmapMirrorApiService)
        } else {
            val beatmaps = getBeatMaps(param.bids, beatmapApiService)
            messageChains = getMessageChains(param.type, beatmaps)
        }

        event.replyMessageChains(messageChains)
    }

    companion object {
        private fun MessageEvent.replyMessageChains(messages: List<MessageChain>) {
            if (messages.isEmpty()) return
            else if (messages.size == 1) {
                this.reply(messages.first())
            } else {
                for (msg in messages) {
                    this.reply(msg)
                    Thread.sleep(1000L)
                }
            }
        }


        private fun parseDataString(
            dataStr: String,
        ): List<Long> {
            val dataStrArray = dataStr.trim().split(REG_SEPERATOR.toRegex(), 0)

            if (dataStrArray.isEmpty()) return listOf()
            return dataStrArray.filter { it.isNotBlank() }.map { it.toLongOrDefault(0L) }
        }

        private fun getBeatMaps(
            bids: List<Long>, beatmapApiService: OsuBeatmapApiService
        ): List<BeatMap> {
            if (bids.isEmpty()) return listOf()
            return bids.map { beatmapApiService.getBeatMapFromDataBase(it) }
        }

        // 获取全幅
        private fun getMessageChains(bids: List<Long>, beatmapMirrorApiService: OsuBeatmapMirrorApiService): List<MessageChain> {
            val list = mutableListOf<MessageChain>()
            var builder = MessageChainBuilder()

            for (i in bids.indices) {
                val b = bids[i]

                val path = try {
                    beatmapMirrorApiService.getFullBackgroundPath(b)
                } catch (e: Exception) {
                    throw GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Fetch, "完整背景")
                }

                val imageStr = path?.toString() ?: throw GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Fetch, "完整背景")

                // qq 一次性只能发 20 张图
                if ((i >= 20) && (i % 20 == 0)) {
                    list.add(builder.build())
                    builder = MessageChainBuilder()
                }

                builder.addImage(imageStr)
            }

            if (builder.isNotEmpty) {
                list.add(builder.build())
            }

            return list
        }

        private fun getMessageChains(type: Type, beatmaps: List<BeatMap>): List<MessageChain> {
            val list = mutableListOf<MessageChain>()
            var builder = MessageChainBuilder()

            for (i in beatmaps.indices) {
                val b = beatmaps[i]

                val covers = b.beatMapSet!!.covers
                val url = when (type) {
                    Type.LIST -> covers.list
                    Type.LIST_2X -> covers.list2x
                    Type.CARD -> covers.card
                    Type.CARD_2X -> covers.card2x
                    Type.SLIM_COVER -> covers.slimcover
                    Type.SLIM_COVER_2X -> covers.slimcover2x
                    Type.COVER_2X -> covers.cover2x
                    else -> covers.cover
                }
                val imageUri = URI.create(url)

                // qq 一次性只能发 20 张图
                if ((i >= 20) && (i % 20 == 0)) {
                    list.add(builder.build())
                    builder = MessageChainBuilder()
                }

                builder.addImage(imageUri.toURL())
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
        val messageChains: List<MessageChain>

        if (param.type == Type.RAW) {
            messageChains = getMessageChains(param.bids, beatmapMirrorApiService)
        } else {
            val beatmaps = getBeatMaps(param.bids, beatmapApiService)
            messageChains = getMessageChains(param.type, beatmaps)
        }

        return messageChains.first()
    }
}
