package com.now.nowbot.service.messageServiceImpl

import com.fasterxml.jackson.databind.JsonNode
import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.osu.LazerScore
import com.now.nowbot.model.osu.OsuUser
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.qq.tencent.TencentMessageService
import com.now.nowbot.service.ImageService
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.messageServiceImpl.BPAnalysisService.BAParam
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.service.osuApiService.OsuCalculateApiService
import com.now.nowbot.service.osuApiService.OsuScoreApiService
import com.now.nowbot.service.osuApiService.OsuUserApiService
import com.now.nowbot.throwable.botRuntimeException.IllegalStateException
import com.now.nowbot.util.*
import com.now.nowbot.util.InstructionUtil.getMode
import com.now.nowbot.util.InstructionUtil.getUserWithoutRange
import org.springframework.stereotype.Service
import java.text.DecimalFormat
import java.util.concurrent.atomic.AtomicBoolean
import java.util.regex.Matcher
import kotlin.math.roundToInt

@Service("UU_BA")
class UUBAService(
    private val userApiService: OsuUserApiService,
    private val beatmapApiService: OsuBeatmapApiService,
    private val scoreApiService: OsuScoreApiService,
    private val calculateApiService: OsuCalculateApiService,
    private val imageService: ImageService,
) : MessageService<BAParam>, TencentMessageService<BAParam> {

    @Throws(Throwable::class)
    override fun isHandle(
            event: MessageEvent,
            messageText: String,
            data: DataValue<BAParam>
    ): Boolean {

        val matcher = Instruction.UU_BA.matcher(messageText)
        if (!matcher.find()) return false

        data.value = getParam(event, matcher)

        return true
    }

    @Throws(Throwable::class)
    override fun handleMessage(event: MessageEvent, param: BAParam): ServiceCallStatistic? {
        val lines = param.getText()

        val panelParam = lines.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val image = imageService.getPanelAlpha(*panelParam)

        try {
            event.reply(image)
        } catch (_: Exception) {
            throw IllegalStateException.Send("最好成绩分析（文字版）")
        }

        return ServiceCallStatistic.build(event, userID = param.user.userID, mode = param.user.currentOsuMode)
    }

    override fun accept(event: MessageEvent, messageText: String): BAParam? {
        val matcher = OfficialInstruction.UU_BA.matcher(messageText)
        if (!matcher.find()) return null

        return getParam(event, matcher)
    }

    override fun reply(event: MessageEvent, param: BAParam): MessageChain? {
        val lines = param.getText()
        return MessageChain(lines)
    }

    private fun getParam(event: MessageEvent, matcher: Matcher): BAParam {
        val isMyself = AtomicBoolean(false)
        val mode = getMode(matcher)

        val user: OsuUser
        val bests: List<LazerScore>

        val id = UserIDUtil.getUserIDWithoutRange(event, matcher, mode, isMyself)

        if (id != null) {
            val async = AsyncMethodExecutor.awaitPairCallableExecute(
                { userApiService.getOsuUser(id, mode.data!!) },
                {
                    val ss = scoreApiService.getBestScores(id, mode.data!!)

                    calculateApiService.applyBeatMapChanges(ss)
                    calculateApiService.applyStarToScores(ss)

                    ss
                }
            )

            user = async.first
            bests = async.second.toList()
        } else {
            user = getUserWithoutRange(event, matcher, mode, isMyself)
            bests = scoreApiService.getBestScores(user.userID, mode.data)

            calculateApiService.applyBeatMapChanges(bests)
            calculateApiService.applyStarToScores(bests)
        }

        val mapperIDs = bests.flatMap { it.beatmap.mapperIDs }.toSet()

        val async2 = AsyncMethodExecutor.awaitPairCallableExecute(
            { beatmapApiService.extendBeatmapInScore(bests) },
            { userApiService.getUsers(mapperIDs) },
        )

        return BAParam(user, async2.first, isMyself.get(), async2.second, 2)

    }

    companion object {
        fun BAParam.getText(): String {
            val node = JacksonUtil.toNode(JacksonUtil.toJson(this.toMap())) as JsonNode

            val name = node["user"]["username"].asText()
            val mode = OsuMode.getMode(node["user"]["mode"].asText()).fullName

            val length = node["length_attr"].toList().toTriple("length")
            val combo = node["combo_attr"].toList().toTriple("combo")
            val star = node["star_attr"].toList().toTriple("star")
            val bpm = node["bpm_attr"].toList().toTriple("bpm")

            val mappers = node["favorite_mappers"].take(5)
                .joinToString("\n") {
                    "${it["username"].asText()}: ${it["map_count"]}x ${it["pp_count"].asDouble().roundToInt()}PP"
                }

            val mods = node["mods_attr"].take(5)
                .joinToString("\n") {
                    "${it["index"].asText()}: ${it["map_count"]}x ${it["pp_count"].asDouble().roundToInt()}PP (${it["percent"].asDouble().times(100).round2()}%)"
                }

            return """
                $name: $mode
                ---
                length: mid: #${length[1].first} ${length[1].second.secondsToTime()},
                max: #${length[0].first} ${length[0].second.secondsToTime()}, min: #${length[2].first} ${length[2].second.secondsToTime()}
                ---
                combo: mid: #${combo[1].first} ${combo[1].second}x,
                max: #${combo[0].first} ${combo[0].second}x, min: #${combo[2].first} ${combo[2].second}x
                ---
                star: mid: #${star[1].first} ${star[1].second}*,
                max: #${star[0].first}* ${star[0].second}, min: #${star[2].first} ${star[2].second}*
                ---
                bpm: mid: #${bpm[1].first} ${bpm[1].second},
                max: #${bpm[0].first} ${bpm[0].second}, min: #${bpm[2].first} ${bpm[2].second}
            """.trimIndent() + "\n---\nmappers:\n" + mappers + "\n---\nmods:\n" + mods
        }

        private fun List<JsonNode>.toTriple(name: String = "length"): List<Pair<Int, String>> {
            return this.map {
                it["ranking"].asInt() to it[name].asText()
            }
        }

        private fun String.secondsToTime(): String {
            val sec = this.toIntOrNull() ?: return "00:00"

            val minutes = sec / 60
            val seconds = sec % 60
            return "%02d:%02d".format(minutes, seconds)
        }

        private val df = DecimalFormat("#.00")

        private fun Double.round2(): String {
            return df.format(this)
        }
    }
}
