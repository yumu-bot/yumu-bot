package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.dao.ServiceCallStatisticsDao
import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.model.osu.Covers.Companion.CoverType
import com.now.nowbot.model.osu.Covers.Companion.CoverType.*
import com.now.nowbot.model.osu.Covers.Companion.CoverType.Companion.getString
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
import com.now.nowbot.throwable.botRuntimeException.IllegalArgumentException
import com.now.nowbot.throwable.botRuntimeException.IllegalStateException
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.command.REG_SEPERATOR
import okhttp3.internal.toLongOrDefault
import org.springframework.stereotype.Service
import java.net.URI
import java.nio.file.Files
import java.time.LocalDateTime

@Service("GET_COVER") class GetCoverService(
    private val beatmapApiService: OsuBeatmapApiService,
    private val beatmapMirrorApiService: OsuBeatmapMirrorApiService,
    private val dao: ServiceCallStatisticsDao
) : MessageService<GetCoverService.CoverParam>, TencentMessageService<GetCoverService.CoverParam> {
    data class CoverParam(val type: CoverType, val bids: List<Long>)

    override fun isHandle(
        event: MessageEvent, messageText: String, data: DataValue<CoverParam>
    ): Boolean {
        val matcher = Instruction.GET_COVER.matcher(messageText)
        val matcher2 = Instruction.GET_BG.matcher(messageText)

        val type: CoverType
        val dataStr: String?

        if (matcher.find()) {
            type = CoverType.getCovetType(matcher.group("type"))
            dataStr = matcher.group("data")
        } else if (matcher2.find()) {
            type = FULL_SIZE
            dataStr = matcher2.group("data")
        } else {
            return false
        }

        val bids = if (dataStr.isNullOrBlank()) {
            val last = dao.getLastBeatmapID(event.subject.id, null, LocalDateTime.now().minusHours(24L))

            if (last != null) {
                listOf(last)
            } else {
                throw IllegalArgumentException.WrongException.BeatmapID()
            }
        } else {
            parseDataString(dataStr)
        }

        data.value = CoverParam(type, bids)
        return true
    }

    @Throws(Throwable::class) override fun handleMessage(event: MessageEvent, param: CoverParam): ServiceCallStatistic? {
        var chain: MessageChain
        var beatmaps: List<Beatmap>

        if (param.type == FULL_SIZE) {
            try {
                beatmaps = beatmapApiService.getBeatmaps(param.bids)
                chain = getRawBackground(param.bids)

                event.replyMessageChain(chain)
            } catch (_: Exception) {

                //val receipt = event.reply(NoSuchElementException.LocalBG().message)

                beatmaps = beatmapApiService.getBeatmaps(param.bids)
                chain = getBackground(FULL_SIZE, beatmaps)

                event.replyMessageChain(chain)

                //receipt.recallIn(30 * 1000L)
            }
        } else {
            beatmaps = beatmapApiService.getBeatmaps(param.bids)
            chain = getBackground(param.type, beatmaps)

            event.replyMessageChain(chain)
        }

        return ServiceCallStatistic.builds(event, beatmapIDs = beatmaps.map { it.beatmapID }, beatmapsetIDs = beatmaps.map { it.beatmapsetID })
    }

    override fun accept(event: MessageEvent, messageText: String): CoverParam? {
        return null
    }

    override fun reply(event: MessageEvent, param: CoverParam): MessageChain {
        var chain: MessageChain

        if (param.type == FULL_SIZE) try {
            chain = getRawBackground(param.bids)
        } catch (_: Exception) {
            val beatmaps = beatmapApiService.getBeatmaps(param.bids)
            chain = getBackground(FULL_SIZE, beatmaps)
        } else {
            val beatmaps = beatmapApiService.getBeatmaps(param.bids)
            chain = getBackground(param.type, beatmaps)
        }

        return chain
    }
    private fun getRawBackground(bids: List<Long>): MessageChain {
        val builder = MessageChainBuilder()

        bids.forEach {
            val path = try {
                beatmapMirrorApiService.getFullBackgroundPath(it)
            } catch (_: Exception) {
                throw IllegalStateException.Fetch("完整背景")
            }

            if (path == null) {
                throw IllegalStateException("抱歉，服务器暂时没有找到完整背景服务...")
            }

            // TODO 超过 10M 的文件可能发不出
            val file = try {
                Files.readAllBytes(path)
            } catch (_: Exception) {
                throw IllegalStateException.ReadFile("完整背景")
            }

            builder.addImage(file)
        }

        return builder.build()
    }

    companion object {

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

        private fun getBackground(type: CoverType, beatmaps: List<Beatmap>): MessageChain {
            val builder = MessageChainBuilder()

            beatmaps.forEach {
                val covers = it.beatmapset!!.covers
                val url = covers.getString(type)

                builder.addImage(URI.create(url).toURL())
            }

            return builder.build()
        }
    }
}
