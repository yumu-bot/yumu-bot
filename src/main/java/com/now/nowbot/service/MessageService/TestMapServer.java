package com.now.nowbot.service.MessageService;

import com.now.nowbot.model.beatmap.Mod;
import com.now.nowbot.service.OsuGetService;
import net.mamoe.mirai.event.events.MessageEvent;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
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


        var info = osuGetService.getMapInfo(bid);
        var sb = new StringBuilder();

        sb.append(bid).append(',');
        sb.append(info.getBeatMapSet().getArtist()).append('-');
        sb.append(info.getBeatMapSet().getTitle()).append('[');
        sb.append(info.getVersion()).append(']').append('\n');


        if (mod == null || mod.trim().equals("")){


            sb.append('(').append(info.getBeatMapSet().getMapperId()).append(')');
            sb.append(info.getDifficultyRating()).append(',')
                    .append(info.getBpm()).append(',')
                    .append(info.getHitLength()).append('\n');
            sb.append(info.getMaxCombo()).append(',')
                    .append(info.getCS()).append(',')
                    .append(info.getAR()).append(',')
                    .append(info.getOD());

            event.getSubject().sendMessage(sb.toString());
            return;
        }

        var mods = mod.split(",");
        var t = Stream.of(mods).map(Mod::fromStr).toList();
        var a = osuGetService.getAttributes((long)bid, t.toArray(new Mod[0]));
        sb.append('(').append(info.getBeatMapSet().getMapperId()).append(')');
        sb.append(a.getStarRating()).append(',')
                .append(info.getBpm()).append(',')
                .append(info.getHitLength()).append('\n');
        sb.append(a.getMaxCombo()).append(',')
                .append(info.getCS()).append(',')
                .append(a.getApproachRate()).append(',')
                .append(info.getOD());

        event.getSubject().sendMessage(sb.toString());
    }
}
