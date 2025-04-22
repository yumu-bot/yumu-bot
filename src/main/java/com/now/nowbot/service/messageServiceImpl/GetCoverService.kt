package com.now.nowbot.service.messageServiceImpl

import com.mikuac.shiro.core.BotContainer
import com.now.nowbot.config.NewbieConfig
import com.now.nowbot.model.json.BeatMap
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.qq.message.MessageChain.MessageChainBuilder
import com.now.nowbot.qq.tencent.TencentMessageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.service.osuApiService.OsuBeatmapMirrorApiService
import com.now.nowbot.service.osuApiService.impl.ScoreApiImpl.CoverType
import com.now.nowbot.throwable.GeneralTipsException
import com.now.nowbot.throwable.TipsException
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.command.REG_SEPERATOR
import okhttp3.internal.toLongOrDefault
import org.springframework.stereotype.Service
import java.net.URI

@Service("GET_COVER") class GetCoverService(
    private val beatmapApiService: OsuBeatmapApiService,
    private val beatmapMirrorApiService: OsuBeatmapMirrorApiService,
    private val botContainer: BotContainer,
    private val newbieConfig: NewbieConfig
) : MessageService<GetCoverService.CoverParam>, TencentMessageService<GetCoverService.CoverParam> {
    @JvmRecord data class CoverParam(val type: CoverType, val bids: List<Long>)

    @Throws(Throwable::class) override fun isHandle(
        event: MessageEvent, messageText: String, data: DataValue<CoverParam>
    ): Boolean {
        val matcher = Instruction.GET_COVER.matcher(messageText)
        val matcher2 = Instruction.GET_BG.matcher(messageText)

        if (matcher.find()) {
            val type = when (matcher.group("type")) {
                "raw", "r", "full", "f", "background", "b" -> CoverType.RAW
                "list", "l", "l1" -> CoverType.LIST
                "list2", "list2x", "list@2x", "l2" -> CoverType.LIST_2X
                "c", "card", "c1" -> CoverType.CARD
                "card2", "card2x", "card@2x", "c2" -> CoverType.CARD_2X
                "slim", "slimcover", "s", "s1" -> CoverType.SLIM_COVER
                "slim2", "slim2x", "slim@2x", "slimcover2", "slimcover2x", "slimcover@2x", "s2" -> CoverType.SLIM_COVER_2X
                "2", "cover2", "cover2x", "cover@2x", "o2" -> CoverType.COVER_2X
                null -> CoverType.COVER
                else -> CoverType.COVER
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

            data.value = CoverParam(CoverType.RAW, bids)
            return true
        } else {
            return false
        }
    }

    @Throws(Throwable::class) override fun HandleMessage(event: MessageEvent, param: CoverParam) {
        val messageChains: List<MessageChain>

        if (param.type == CoverType.RAW) {
            // messageChains = listOf(MessageChainBuilder().addText("抱歉，应急运行时是没有 getBG 服务的呢...").build())

            messageChains = getMessageChains(param.bids, beatmapMirrorApiService)

            event.replyMessageChainsWithOfficialBot(messageChains, botContainer, newbieConfig)
        } else {
            val beatmaps = getBeatMaps(param.bids, beatmapApiService)
            messageChains = getMessageChains(param.type, beatmaps)

            event.replyMessageChains(messageChains)
        }
    }

    companion object {
        private fun MessageEvent.replyMessageChainsWithOfficialBot(messages: List<MessageChain>, botContainer: BotContainer, newbieConfig: NewbieConfig) {
            val groupID = this.subject.id

            val yumu = botContainer.robots[newbieConfig.yumuBot]
            val hydrant = botContainer.robots[newbieConfig.hydrantBot]

            if (yumu != null && yumu.groupList.data.find { groupID == it.groupId } != null) {
                if (messages.isEmpty()) return

                else if (messages.size == 1) {
                    yumu.sendGroupMsg(groupID, messages.first().rawMessage, true)
                } else {
                    for (msg in messages) {
                        yumu.sendGroupMsg(groupID, msg.rawMessage, true)
                        Thread.sleep(1000L)
                    }
                }

            } else if (hydrant != null && hydrant.groupList.data.find { groupID == it.groupId } != null) {
                if (messages.isEmpty()) return

                else if (messages.size == 1) {
                    hydrant.sendGroupMsg(groupID, messages.first().rawMessage, true)
                } else {
                    for (msg in messages) {
                        hydrant.sendGroupMsg(groupID, msg.rawMessage, true)
                        Thread.sleep(1000L)
                    }
                }
            } else if (yumu == null && hydrant == null) {
                throw TipsException("当前能发送原图的机器人账号都不在线呢...")
            } else {
                throw TipsException("这个群没有可以发送原图的机器人账号呢...")
            }
        }

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

        private fun getMessageChains(type: CoverType, beatmaps: List<BeatMap>): List<MessageChain> {
            val list = mutableListOf<MessageChain>()
            var builder = MessageChainBuilder()

            for (i in beatmaps.indices) {
                val b = beatmaps[i]

                val covers = b.beatMapSet!!.covers
                val url = when (type) {
                    CoverType.LIST -> covers.list
                    CoverType.LIST_2X -> covers.list2x
                    CoverType.CARD -> covers.card
                    CoverType.CARD_2X -> covers.card2x
                    CoverType.SLIM_COVER -> covers.slimcover
                    CoverType.SLIM_COVER_2X -> covers.slimcover2x
                    CoverType.COVER_2X -> covers.cover2x
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

        if (param.type == CoverType.RAW) {
            messageChains = getMessageChains(param.bids, beatmapMirrorApiService)
        } else {
            val beatmaps = getBeatMaps(param.bids, beatmapApiService)
            messageChains = getMessageChains(param.type, beatmaps)
        }

        return messageChains.first()
    }
}
