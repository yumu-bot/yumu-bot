package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.model.mappool.old.MapPoolDto;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuApiService.OsuBeatmapApiService;
import com.now.nowbot.throwable.ServiceException.MapPoolException;
import com.now.nowbot.util.DataUtil;
import com.now.nowbot.util.Instructions;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;

@Service("GET_POOL")
public class GetPoolService implements MessageService<Matcher> {
    private static final Logger log = LoggerFactory.getLogger(MapPoolService.class);
    @Resource
    ImageService         imageService;
    @Resource
    OsuBeatmapApiService osuBeatmapApiService;

    @Override
    public boolean isHandle(MessageEvent event, String messageText, DataValue<Matcher> data) throws Throwable {
        var m = Instructions.GET_POOL.matcher(messageText);
        if (! m.find()) {
            return false;
        }
        data.setValue(m);
        return true;
    }

    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        var from = event.getSubject();

        var dataStr = matcher.group("data");
        var nameStr = matcher.group("name");

        if (Objects.isNull(dataStr) || dataStr.isBlank()) {
            try {
                var md = DataUtil.getMarkdownFile("Help/getpool.md");
                var image = imageService.getPanelA6(md, "help");
                from.sendImage(image);
                return;
            } catch (Exception e) {
                throw new MapPoolException(MapPoolException.Type.GP_Instructions);
            }
        }

        var data = parseDataString(dataStr);
        var mapPool = new MapPoolDto(nameStr, data, osuBeatmapApiService);

        if (mapPool.getModPools().isEmpty()) throw new MapPoolException(MapPoolException.Type.GP_Map_Empty);

        try {
            var image = imageService.getPanelH(mapPool);
            from.sendImage(image);
        } catch (Exception e) {
            log.error("GP 数据请求失败", e);
            throw new MapPoolException(MapPoolException.Type.GP_Send_Error);
        }
    }

    public Map<String, List<Long>> parseDataString(String dataStr) throws MapPoolException {
        String[] dataStrArray = dataStr.trim().split("[\"\\s,，\\-|:]+");
        if (dataStr.isBlank() || dataStrArray.length == 0) return null;

        var output = new LinkedHashMap<String, List<Long>>();

        String Mods = "";
        List<Long> BIDs = new ArrayList<>();

        int status = 0; //0：收取 Mod 状态，1：收取 BID 状态，2：无需收取，直接输出。

        for (int i = 0; i < dataStrArray.length; i++) {
            String s = dataStrArray[i];
            if (s == null || s.isBlank()) continue;

            String mod = null;
            Long v = null;

            try {
                v = Long.parseLong(s);
            } catch (NumberFormatException e) {
                mod = s;
            }

            switch (status) {
                case 0 -> {
                    if (Objects.nonNull(mod)) {
                        Mods = mod;
                        mod = null;
                        status = 1;
                    } else throw new MapPoolException(MapPoolException.Type.GP_Parse_MissingMap, s, String.valueOf(i));
                }
                case 1 -> {
                    if (Objects.nonNull(mod)) {
                        if (BIDs.isEmpty()) {
                            throw new MapPoolException(MapPoolException.Type.GP_Parse_MissingMap, s, String.valueOf(i));
                        } else {
                            status = 2;
                        }
                    } else {
                        BIDs.add(v);
                    }
                }
            }

            if (status == 2 || i == dataStrArray.length - 1) {
                output.put(Mods, List.copyOf(BIDs));
                BIDs.clear();
                Mods = null;
                status = 0;

                if (Objects.nonNull(mod)) {
                    Mods = mod;
                    status = 1;
                }
            }
        }

        return output;
    }
}
