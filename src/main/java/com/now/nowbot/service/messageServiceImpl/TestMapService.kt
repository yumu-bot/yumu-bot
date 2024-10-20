package com.now.nowbot.service.messageServiceImpl

import com.now.nowbot.model.enums.OsuMod.Companion.getModsList
import com.now.nowbot.model.enums.OsuMod.Companion.getModsValue
import com.now.nowbot.qq.event.MessageEvent
import com.now.nowbot.service.MessageService
import com.now.nowbot.service.MessageService.DataValue
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService
import com.now.nowbot.util.DataUtil
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
    
    @Throws(Throwable::class) override fun HandleMessage(event: MessageEvent, matcher: Matcher) {
        val bid = matcher.group("id").toInt()
        val mod = matcher.group("mod")
        
        val b = beatmapApiService.getBeatMap(bid)
        val sb = StringBuilder()
        
        sb.append(bid).append(',')
        if (b.beatMapSet != null) {
            sb.append(b.beatMapSet!!.artistUnicode).append(' ').append('-').append(' ')
            sb.append(b.beatMapSet!!.titleUnicode).append(' ')
            sb.append('(').append(b.beatMapSet!!.creator).append(')').append(' ')
        }
        sb.append('[').append(b.difficultyName).append(']').append(',')
        
        
        if (mod == null || mod.trim {it <= ' '} .isEmpty()) {
            sb.append(String.format("%.2f", b.starRating)).append(',')
            .append(String.format("%d", Math.round(b.bpm))).append(',')
            .append(String.format("%d", Math.round(floor((b.totalLength / 60f).toDouble()))))
            .append(':')
            .append(String.format("%02d", Math.round(b.totalLength % 60f)))
            .append(',')
            sb.append(b.maxCombo).append(',')
            .append(b.cs).append(',')
            .append(b.ar).append(',')
            .append(b.od)
            
            event.subject.sendMessage(sb.toString())
            return 
        }
        
        val mods = getModsList(Stream.of(*mod.split("[\"\\s,，\\-|:]+".toRegex()).dropLastWhile {it.isEmpty()} .toTypedArray()).map { obj: String -> obj.uppercase(Locale.getDefault())} .toList())

        
        val a = beatmapApiService.getAttributes(bid.toLong(), getModsValue(mods))
        val newTotalLength = DataUtil.applyLength(b.totalLength, mods).toFloat()
        
        sb.append(String.format("%.2f", a.starRating)).append(',')
        .append(String.format("%d", Math.round(DataUtil.applyBPM(b.bpm, mods)))).append(',')
        .append(String.format("%d", Math.round(floor((newTotalLength / 60f).toDouble()))))
        .append(':')
        .append(String.format("%02d", Math.round(newTotalLength % 60f)))
        .append(',')
        sb.append(a.maxCombo).append(',')
        .append(String.format("%.2f", DataUtil.applyCS(b.cs, mods))).append(',')
        .append(String.format("%.2f", DataUtil.applyAR(b.ar, mods))).append(',')
        .append(String.format("%.2f", DataUtil.applyOD(b.od, mods)))
        
        event.subject.sendMessage(sb.toString())
    }
}
