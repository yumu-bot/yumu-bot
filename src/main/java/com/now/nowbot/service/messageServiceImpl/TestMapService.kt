package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.entity.ServiceCallStatistic
import com.now.nowbot.model.osu.LazerMod
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.service.osuApiService.impl.CalculateApiImpl

import com.now.nowbot.throwable.botRuntimeException.IllegalArgumentException
import com.now.nowbot.util.Instruction
import com.now.nowbot.util.command.REG_SEPERATOR_NO_SPACE
import org.springframework.stereotype.Service
import java.util.regex.Matcher
import kotlin.math.floor
import kotlin.math.roundToInt

@Service("TEST_MAP") 
class TestMapService(private val beatmapApiService: OsuBeatmapApiService) : MessageService<Matcher> {
    
    override fun isHandle(event: MessageEvent, messageText: String, data: DataValue<Matcher>): Boolean {
        val m = Instruction.TEST_MAP.matcher(messageText)
        if (m.find()) {
            data.value = m
            return true
        } else return false
    }
    
    @Throws(Throwable::class) override fun handleMessage(event: MessageEvent, param: Matcher): ServiceCallStatistic? {
        val bid = param.group("id").toLongOrNull() ?: throw IllegalArgumentException.WrongException.BeatmapID()
        val mod = param.group("mod")
        
        val b = beatmapApiService.getBeatmap(bid)
        val sb = StringBuilder()
        
        sb.append(bid).append(',')
        if (b.beatmapset != null) {
            sb.append(b.beatmapset!!.artistUnicode).append(' ').append('-').append(' ')
            sb.append(b.beatmapset!!.titleUnicode).append(' ')
            sb.append('(').append(b.beatmapset!!.creator).append(')').append(' ')
        }
        sb.append('[').append(b.difficultyName).append(']').append(',')
        
        
        if (mod == null || mod.trim {it <= ' '} .isEmpty()) {
            sb.append(String.format("%.2f", b.starRating)).append(',')
            .append(String.format("%d", b.bpm.roundToInt())).append(',')
            .append(String.format("%d", floor((b.totalLength / 60.0)).roundToInt()))
            .append(':')
            .append(String.format("%02d", (b.totalLength % 60f).roundToInt()))
            .append(',')
            sb.append(b.maxCombo).append(',')
            .append(b.cs).append(',')
            .append(b.ar).append(',')
            .append(b.od)
            
            event.reply(sb.toString())

            return null
        }

        val mods = LazerMod.getModsList(mod
            .split(REG_SEPERATOR_NO_SPACE.toRegex())
            .dropLastWhile { it.isEmpty() }
        )
        
        val a = beatmapApiService.getAttributes(bid, b.mode, mods)
        val newTotalLength = CalculateApiImpl.applyLength(b.totalLength, mods).toFloat()
        
        sb.append(String.format("%.2f", a.starRating)).append(',')
        .append(String.format("%d", CalculateApiImpl.applyBPM(b.bpm, mods).roundToInt())).append(',')
        .append(String.format("%d", floor((newTotalLength / 60.0)).roundToInt()))
        .append(':')
        .append(String.format("%02d", (newTotalLength % 60.0).roundToInt()))
        .append(',')
        sb.append(a.maxCombo).append(',')
        .append(String.format("%.2f", CalculateApiImpl.applyCS(b.cs!!, mods))).append(',')
        .append(String.format("%.2f", CalculateApiImpl.applyAR(b.ar!!, mods))).append(',')
        .append(String.format("%.2f", CalculateApiImpl.applyOD(b.od!!, mods, b.mode)))
        
        event.reply(sb.toString())

        return null
    }
}
