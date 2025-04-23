package com.now.nowbot.service.messageServiceImpl

import com.mikuac.shiro.core.BotContainer
import com.now.nowbot.config.NewbieConfig
import com.now.nowbot.model.json.BeatMap
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.ImageMessage
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.qq.message.MessageChain.MessageChainBuilder
import com.now.nowbot.qq.onebot.contact.Group
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
        val chain: MessageChain

        if (param.type == CoverType.RAW) {
            // messageChains = listOf(MessageChainBuilder().addText("抱歉，应急运行时是没有 getBG 服务的呢...").build())

            chain = getMessageChain(param.bids, beatmapMirrorApiService)

            event.replyMessageChainsWithOfficialBot(chain, botContainer, newbieConfig)
        } else {
            val beatmaps = getBeatMaps(param.bids, beatmapApiService)
            chain = getMessageChain(param.type, beatmaps)

            event.replyMessageChain(chain)
        }
    }

    companion object {
        private fun MessageEvent.replyMessageChainsWithOfficialBot(chain: MessageChain, botContainer: BotContainer, newbieConfig: NewbieConfig) {
            val groupID = this.subject.id
            val messages = chain.messageList

            val yumu = botContainer.robots[newbieConfig.yumuBot]
            val hydrant = botContainer.robots[newbieConfig.hydrantBot]

            val contact: Group = if (yumu != null && yumu.groupList.data.any { groupID == it.groupId } ) {
                Group(yumu, groupID, "yumu")
            } else if (hydrant != null && hydrant.groupList.data.any { groupID == it.groupId } ) {
                Group(hydrant, groupID, "yumu")
            } else if (yumu == null && hydrant == null) {
                throw TipsException("当前能发送原图的机器人账号都不在线呢...")
            } else {
                throw TipsException("这个群没有可以发送原图的机器人账号呢...")
            }

            if (messages.isEmpty()) return
            else if (messages.size <= 20) {
                contact.sendMessage(chain)
            } else {
                for (msg in messages.chunked(20)) {
                    val b = MessageChainBuilder()
                    msg.forEach{ b.addMessage(it) }
                    contact.sendMessage(b.build())
                    Thread.sleep(1000L)
                }
            }
        }

        private fun MessageEvent.replyMessageChain(chain: MessageChain) {
            val messages = chain.messageList

            if (messages.isEmpty()) return
            else if (messages.size <= 20) {
                this.reply(chain)
            } else {
                for (msg in messages.chunked(20)) {
                    val b = MessageChainBuilder()
                    msg.filterIsInstance<ImageMessage>().forEach { b.addImage(it.data) }

                    this.reply(b.build())
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
        private fun getMessageChain(bids: List<Long>, beatmapMirrorApiService: OsuBeatmapMirrorApiService): MessageChain {
            val builder = MessageChainBuilder()

            bids.forEach {
                val path = try {
                    beatmapMirrorApiService.getFullBackgroundPath(it)
                } catch (e: Exception) {
                    throw GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Fetch, "完整背景")
                }

                val imageStr = path?.toString() ?: throw GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Fetch, "完整背景")

                builder.addImage(imageStr)
            }

            return builder.build()
        }

        private fun getMessageChain(type: CoverType, beatmaps: List<BeatMap>): MessageChain {
            val builder = MessageChainBuilder()

            beatmaps.forEach {
                val covers = it.beatMapSet!!.covers
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

                builder.addImage(URI.create(url).toURL())
            }

            return builder.build()
        }
    }

    override fun accept(event: MessageEvent, messageText: String): CoverParam? {
        return null
    }

    override fun reply(event: MessageEvent, param: CoverParam): MessageChain {
        val chain: MessageChain

        if (param.type == CoverType.RAW) {
            chain = getMessageChain(param.bids, beatmapMirrorApiService)
        } else {
            val beatmaps = getBeatMaps(param.bids, beatmapApiService)
            chain = getMessageChain(param.type, beatmaps)
        }

        return chain
    }
}
