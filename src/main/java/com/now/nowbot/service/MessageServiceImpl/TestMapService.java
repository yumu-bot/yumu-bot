package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.model.enums.Mod;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuApiService.OsuBeatmapApiService;
import com.now.nowbot.util.DataUtil;
import com.now.nowbot.util.Pattern4ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.stream.Stream;

@Service("TESTMAP")
public class TestMapService implements MessageService<Matcher> {
    @Resource
    OsuBeatmapApiService beatmapApiService;

    @Override
    public boolean isHandle(MessageEvent event, DataValue<Matcher> data) {
        var m = Pattern4ServiceImpl.TESTMAP.matcher(event.getRawMessage().trim());
        if (m.find()) {
            data.setValue(m);
            return true;
        } else return false;
    }

    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        int bid = Integer.parseInt(matcher.group("d"));
        String mod = matcher.group("mode");


        var info = beatmapApiService.getBeatMapInfo(bid);
        var sb = new StringBuilder();

        sb.append(bid).append(',');
        sb.append(info.getBeatMapSet().getArtistUTF()).append(' ').append('-').append(' ');
        sb.append(info.getBeatMapSet().getTitleUTF()).append(' ');
        sb.append('(').append(info.getBeatMapSet().getMapperName()).append(')').append(' ');
        sb.append('[').append(info.getVersion()).append(']').append(',');


        if (mod == null || mod.trim().isEmpty()){

            sb.append(String.format("%.2f", info.getDifficultyRating())).append(',')
                    .append(info.getBPM()).append(',')
                    .append(String.format("%d", Math.round(Math.floor(info.getTotalLength() / 60f))))
                    .append(':')
                    .append(String.format("%02d", Math.round(info.getTotalLength() % 60f)))
                    .append(',');
            sb.append(info.getMaxCombo()).append(',')
                    .append(info.getCS()).append(',')
                    .append(info.getAR()).append(',')
                    .append(info.getOD());

            event.getSubject().sendMessage(sb.toString());
            return;
        }

        var mods = mod.split(",");
        int modInt = Stream.of(mods).map(Mod::fromStr).map(e -> e.value).reduce(0, (v, a)-> v|a);
        var a = beatmapApiService.getAttributes((long) bid, modInt);
        float newTotalLength = getNewTotalLength (info.getTotalLength(), modInt);

        sb.append(String.format("%.2f", a.getStarRating())).append(',')
                .append(info.getBPM()).append(',')
                .append(String.format("%d", Math.round(Math.floor(newTotalLength / 60f))))
                .append(':')
                .append(String.format("%02d", Math.round(newTotalLength % 60f)))
                .append(',');
        sb.append(a.getMaxCombo()).append(',')
                .append(String.format("%.2f", Math.round(DataUtil.CS(info.getCS(), modInt) * 100f) / 100f)).append(',')
                .append(String.format("%.2f", (Math.round(a.getApproachRate()) * 100f) / 100f)).append(',')
                .append(String.format("%.2f", Math.round(DataUtil.OD(info.getOD(), modInt) * 100f) / 100f));

        event.getSubject().sendMessage(sb.toString());
    }

    private Float getNewTotalLength(Integer totalLength, int modInt) {
        if (Mod.hasDt(modInt)) {
            return (totalLength / 1.5f);
        } else if (Mod.hasHt(modInt)) {
            return (totalLength * 1.5f);
        } else {
            return totalLength * 1f;
        }
    }
}
