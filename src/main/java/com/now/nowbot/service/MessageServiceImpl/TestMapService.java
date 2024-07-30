package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.model.enums.OsuMod;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuApiService.OsuBeatmapApiService;
import com.now.nowbot.util.DataUtil;
import com.now.nowbot.util.Instruction;
import com.now.nowbot.util.Instructions;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.stream.Stream;

@Service("TEST_MAP")
public class TestMapService implements MessageService<Matcher> {
    @Resource
    OsuBeatmapApiService beatmapApiService;

    @Override
    public boolean isHandle(MessageEvent event, String messageText, DataValue<Matcher> data) {
        var m = Instruction.TEST_MAP.matcher(messageText);
        if (m.find()) {
            data.setValue(m);
            return true;
        } else return false;
    }

    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        int bid = Integer.parseInt(matcher.group("id"));
        String mod = matcher.group("mod");

        var b = beatmapApiService.getBeatMapInfo(bid);
        var sb = new StringBuilder();

        sb.append(bid).append(',');
        if (b.getBeatMapSet() != null) {
            sb.append(b.getBeatMapSet().getArtistUnicode()).append(' ').append('-').append(' ');
            sb.append(b.getBeatMapSet().getTitleUnicode()).append(' ');
            sb.append('(').append(b.getBeatMapSet().getCreator()).append(')').append(' ');
        }
        sb.append('[').append(b.getDifficultyName()).append(']').append(',');


        if (mod == null || mod.trim().isEmpty()){

            sb.append(String.format("%.2f", b.getStarRating())).append(',')
                    .append(String.format("%d", Math.round(b.getBPM()))).append(',')
                    .append(String.format("%d", Math.round(Math.floor(b.getTotalLength() / 60f))))
                    .append(':')
                    .append(String.format("%02d", Math.round(b.getTotalLength() % 60f)))
                    .append(',');
            sb.append(b.getMaxCombo()).append(',')
                    .append(b.getCS()).append(',')
                    .append(b.getAR()).append(',')
                    .append(b.getOD());

            event.getSubject().sendMessage(sb.toString());
            return;
        }

        var mods = mod.split("[\"\\s,，\\-|:]+");
        int modInt = Stream.of(mods).map(OsuMod::getModFromAbbreviation).map(e -> e.value).reduce(0, (v, a)-> v|a);
        var a = beatmapApiService.getAttributes((long) bid, modInt);
        float newTotalLength = DataUtil.Length(b.getTotalLength(), modInt);

        sb.append(String.format("%.2f", a.getStarRating())).append(',')
                .append(String.format("%d", Math.round(DataUtil.BPM(b.getBPM(), modInt)))).append(',')
                .append(String.format("%d", Math.round(Math.floor(newTotalLength / 60f))))
                .append(':')
                .append(String.format("%02d", Math.round(newTotalLength % 60f)))
                .append(',');
        sb.append(a.getMaxCombo()).append(',')
                .append(String.format("%.2f", Math.round(DataUtil.CS(b.getCS(), modInt) * 100f) / 100f)).append(',')
                .append(String.format("%.2f", Math.round(DataUtil.AR(b.getAR(), modInt) * 100f) / 100f)).append(',')
                .append(String.format("%.2f", Math.round(DataUtil.OD(b.getOD(), modInt) * 100f) / 100f));

        event.getSubject().sendMessage(sb.toString());
    }
}
