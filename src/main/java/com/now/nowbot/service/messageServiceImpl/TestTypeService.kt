package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.dao.ServiceCallStatisticsDao
import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.model.osu.LazerMod
import com.now.nowbot.model.beatmapParse.OsuFile
import com.now.nowbot.model.enums.OsuMode
import com.now.nowbot.model.osu.Beatmap
import com.now.nowbot.model.osu.LazerMod.Companion.isAffectStarRating
import com.now.nowbot.model.skill.SkillType
import com.now.nowbot.model.skill.Skill
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.qq.message.MessageChain
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.messageServiceImpl.TestTypeService.MapTypeParam
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.service.osuApiService.OsuCalculateApiService
import com.now.nowbot.throwable.botRuntimeException.IllegalArgumentException
import com.now.nowbot.throwable.botRuntimeException.IllegalStateException
import com.now.nowbot.throwable.botRuntimeException.NoSuchElementException
import com.now.nowbot.throwable.botRuntimeException.UnsupportedOperationException
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.command.FLAG_MOD
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service("TEST_TYPE") class TestTypeService(
    private val beatmapApiService: OsuBeatmapApiService,
    private val calculateApiService: OsuCalculateApiService,
    private val dao: ServiceCallStatisticsDao
) : MessageService<MapTypeParam> {
    data class MapTypeParam(val bid: Long, val mode: OsuMode, val rate: Double = 1.0, val mods: List<LazerMod>)

    override fun isHandle(
        event: MessageEvent,
        messageText: String,
        data: DataValue<MapTypeParam>,
    ): Boolean {
        val matcher = Instruction.TEST_TYPE.matcher(messageText)
        if (!matcher.find()) {
            return false
        }

        val modsList: List<LazerMod> = LazerMod.getModsList(matcher.group(FLAG_MOD))

        val bid = matcher.group("bid")?.toLongOrNull()
            ?: dao.getLastBeatmapID(event.subject.contactID, name = null, LocalDateTime.now().minusHours(24))
            ?: throw IllegalArgumentException.WrongException.BeatmapID()

        val rate = matcher.group("rate")?.toDoubleOrNull() ?: 1.0

        if (rate < 0.1) throw IllegalArgumentException.ExceedException.RateTooSmall()
        if (rate > 5.0) throw IllegalArgumentException.ExceedException.RateTooLarge()

        data.value = MapTypeParam(bid, OsuMode.MANIA, rate, modsList)
        return true
    }

    @Throws(Throwable::class) override fun handleMessage(event: MessageEvent, param: MapTypeParam): ServiceCallStatistic? {
        val fileStr: String
        val map: Beatmap

        val isChangedRating = param.mods.isAffectStarRating()

        try {
            map = beatmapApiService.getBeatmapFromDatabase(param.bid)
            fileStr = beatmapApiService.getBeatmapFileString(param.bid)!!
        } catch (_: Exception) {
            throw NoSuchElementException.BeatmapCache(param.bid)
        }

        if (map.mode.isNotConvertAble(param.mode)) {
            throw UnsupportedOperationException.OnlyMania()
        }

        calculateApiService.applyStarToBeatmap(map, param.mode, param.mods)

        val file = try {
            OsuFile(fileStr)
        } catch (_: NullPointerException) {
            throw IllegalStateException.Fetch("谱面文件")
        }

        val mapMinus = Skill(
            file,
            param.mode,
            if (isChangedRating) {
                LazerMod.getModSpeedForStarCalculate(param.mods).toDouble()
            } else {
                param.rate
            },
        )

        // =======================

        val result = SkillType.getType(mapMinus)
        val sb = StringBuilder("\n")

        var i = 1
        for (e in result) {
            sb.append("#${i}:").append(" ").append(String.format("%.2f", e.value * 100)).append("%").append(" ")
                .append("[${e.key.chinese}]").append("\n")

            i++

            if (i >= 6) break
        }

        sb.removeSuffix("\n")

        event.reply(
            MessageChain.MessageChainBuilder().addText("这张图可能是？？：\n").addText(sb.toString()).build()
        )

        return null
    }
}
