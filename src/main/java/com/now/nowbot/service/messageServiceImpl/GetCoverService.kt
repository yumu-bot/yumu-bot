package com.now.nowbot.service.messageServiceImpl

import com.mikuac.shiro.core.BotContainer
import com.now.nowbot.config.NewbieConfig
import com.now.nowbot.model.enums.CoverType
import com.now.nowbot.model.enums.CoverType.*
import com.now.nowbot.model.osu.Beatmap
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.ImageMessage
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
import java.nio.file.Files

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
                "raw", "r", "full", "f", "background", "b" -> RAW
                "list", "l", "l1" -> LIST
                "list2", "list2x", "list@2x", "l2" -> LIST_2X
                "c", "card", "c1" -> CARD
                "card2", "card2x", "card@2x", "c2" -> CARD_2X
                "slim", "slimcover", "s", "s1" -> SLIM_COVER
                "slim2", "slim2x", "slim@2x", "slimcover2", "slimcover2x", "slimcover@2x", "s2" -> SLIM_COVER_2X
                "2", "cover2", "cover2x", "cover@2x", "o2" -> COVER_2X
                null -> COVER
                else -> COVER
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

            data.value = CoverParam(RAW, bids)
            return true
        } else {
            return false
        }
    }

    @Throws(Throwable::class) override fun HandleMessage(event: MessageEvent, param: CoverParam) {
        val chain: MessageChain

        if (param.type == RAW) {
            // messageChains = listOf(MessageChainBuilder().addText("抱歉，应急运行时是没有 getBG 服务的呢...").build())

            chain = getRawBackground(param.bids, beatmapMirrorApiService)

            event.replyMessageChain(chain)
        } else {
            val beatmaps = getBeatMaps(param.bids, beatmapApiService)
            chain = getBackground(param.type, beatmaps)

            event.replyMessageChain(chain)
        }
    }

    companion object {
        /*
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
                    contact.sendMessage(MessageChain(msg))
                    Thread.sleep(1000L)
                }
            }
        }

         */

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
        ): List<Beatmap> {
            if (bids.isEmpty()) return listOf()
            return bids.map { beatmapApiService.getBeatMapFromDataBase(it) }
        }


        private fun getRawBackground(bids: List<Long>, beatmapMirrorApiService: OsuBeatmapMirrorApiService): MessageChain {
            val builder = MessageChainBuilder()

            bids.forEach {
                val path = try {
                    beatmapMirrorApiService.getFullBackgroundPath(it)
                } catch (e: Exception) {
                    throw GeneralTipsException(GeneralTipsException.Type.G_Malfunction_Fetch, "完整背景")
                }

                if (path == null) throw GeneralTipsException("抱歉，服务器暂时没有找到完整背景服务...")

                // TODO 超过 10M 的文件可能发不出
                val file = try {
                    Files.readAllBytes(path)
                } catch (e: Exception) {
                    throw GeneralTipsException(GeneralTipsException.Type.G_Malfunction_IOException, "完整背景")
                }

                builder.addImage(file)
            }

            return builder.build()
        }

        private fun getBackground(type: CoverType, beatmaps: List<Beatmap>): MessageChain {
            val builder = MessageChainBuilder()

            beatmaps.forEach {
                val covers = it.beatmapset!!.covers
                val url = when (type) {
                    LIST -> covers.list
                    LIST_2X -> covers.list2x
                    CARD -> covers.card
                    CARD_2X -> covers.card2x
                    SLIM_COVER -> covers.slimcover
                    SLIM_COVER_2X -> covers.slimcover2x
                    COVER_2X -> covers.cover2x
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

        if (param.type == RAW) {
            chain = getRawBackground(param.bids, beatmapMirrorApiService)
        } else {
            val beatmaps = getBeatMaps(param.bids, beatmapApiService)
            chain = getBackground(param.type, beatmaps)
        }

        return chain
    }
}
