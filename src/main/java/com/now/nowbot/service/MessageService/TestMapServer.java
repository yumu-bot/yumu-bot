package com.now.nowbot.service.MessageService;

import com.now.nowbot.model.enums.Mod;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.util.DataUtil;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.regex.Matcher;
import java.util.stream.Stream;

@Service("test-map")
public class TestMapServer implements MessageService{
    @Resource
    OsuGetService osuGetService;
    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        int bid = Integer.parseInt(matcher.group("d"));
        String mod = matcher.group("mode");


        var info = osuGetService.getBeatMapInfo(bid);
        var sb = new StringBuilder();

        sb.append(bid).append(',');
        sb.append(info.getBeatMapSet().getArtistUTF()).append(' ').append('-').append(' ');
        sb.append(info.getBeatMapSet().getTitleUTF()).append(' ');
        sb.append('(').append(info.getBeatMapSet().getCreator()).append(')').append(' ');
        sb.append('[').append(info.getVersion()).append(']').append(',');


        if (mod == null || mod.trim().equals("")){

            sb.append(info.getDifficultyRating()).append(',')
                    .append(info.getBpm()).append(',')
                    .append(String.format("%d", (int) (Math.floor(info.getTotalLength() / 60f))))
                    .append(':')
                    .append(String.format("%02d", (int) (info.getTotalLength() % 60f)))
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
        var a = osuGetService.getAttributes((long)bid, modInt);
        sb.append('(').append(info.getBeatMapSet().getMapperId()).append(')');
        sb.append(a.getStarRating()).append(',')
                .append(info.getBpm()).append(',')
                .append(info.getHitLength()).append('\n');
        sb.append(a.getMaxCombo()).append(',')
                .append(DataUtil.CS(info.getCS(), modInt)).append(',')
                .append(a.getApproachRate()).append(',')
                .append(DataUtil.OD(info.getOD(), modInt));

        event.getSubject().sendMessage(sb.toString());
    }
}
