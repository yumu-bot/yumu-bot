package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.model.osu.LazerMod
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.service.osuApiService.impl.CalculateApiImpl

import com.now.nowbot.throwable.botRuntimeException.IllegalArgumentException
import com.now.nowbot.util.Instruction
import org.springframework.stereotype.Service
import java.util.*
import java.util.regex.Matcher
import java.util.stream.Stream
import kotlin.math.floor

@Service("TEST_MAP") 
class TestMapService(private val beatmapApiService: OsuBeatmapApiService) : MessageService<Matcher> {
    
    override fun isHandle(event: MessageEvent, messageText: String, data: DataValue<Matcher>): Boolean {
        val m = Instruction.TEST_MAP.matcher(messageText)
        if (m.find()) {
            data.value = m
            return true
        } else return false
    }
    
    @Throws(Throwable::class) override fun HandleMessage(event: MessageEvent, param: Matcher) {
        val bid = param.group("id").toIntOrNull() ?: throw IllegalArgumentException.WrongException.BeatmapID()
        val mod = param.group("mod")
        
        val b = beatmapApiService.getBeatMap(bid)
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
            .append(String.format("%d", Math.round(b.BPM!!))).append(',')
            .append(String.format("%d", Math.round(floor((b.totalLength / 60f).toDouble()))))
            .append(':')
            .append(String.format("%02d", Math.round(b.totalLength % 60f)))
            .append(',')
            sb.append(b.maxCombo).append(',')
            .append(b.CS).append(',')
            .append(b.AR).append(',')
            .append(b.OD)
            
            event.subject.sendMessage(sb.toString())
            return 
        }
        
        val mods = LazerMod.getModsList(Stream.of(*mod.split("[\"\\s,，\\-|:]+".toRegex()).dropLastWhile {it.isEmpty()} .toTypedArray()).map { obj: String -> obj.uppercase(Locale.getDefault())} .toList())

        
        val a = beatmapApiService.getAttributes(bid.toLong(), LazerMod.getModsValue(mods))
        val newTotalLength = CalculateApiImpl.applyLength(b.totalLength, mods).toFloat()
        
        sb.append(String.format("%.2f", a.starRating)).append(',')
        .append(String.format("%d", Math.round(CalculateApiImpl.applyBPM(b.BPM, mods)))).append(',')
        .append(String.format("%d", Math.round(floor((newTotalLength / 60f).toDouble()))))
        .append(':')
        .append(String.format("%02d", Math.round(newTotalLength % 60f)))
        .append(',')
        sb.append(a.maxCombo).append(',')
        .append(String.format("%.2f", CalculateApiImpl.applyCS(b.CS!!, mods))).append(',')
        .append(String.format("%.2f", CalculateApiImpl.applyAR(b.AR!!, mods))).append(',')
        .append(String.format("%.2f", CalculateApiImpl.applyOD(b.OD!!, mods, b.mode)))
        
        event.subject.sendMessage(sb.toString())
    }
}
