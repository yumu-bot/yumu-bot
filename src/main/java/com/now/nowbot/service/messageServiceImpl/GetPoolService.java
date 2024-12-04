package com.now.nowbot.service.messageServiceImpl;

import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.model.mappool.old.MapPoolDto;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService;
import com.now.nowbot.throwable.serviceException.MapPoolException;
import com.now.nowbot.util.CmdUtil;
import com.now.nowbot.util.DataUtil;
import com.now.nowbot.util.Instruction;
import jakarta.annotation.Resource;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;

@Service("GET_POOL")
public class GetPoolService implements MessageService<GetPoolService.GetPoolParam> {
    private static final Logger log = LoggerFactory.getLogger(MapPoolService.class);
    @Resource
    ImageService imageService;
    @Resource
    OsuBeatmapApiService osuBeatmapApiService;

    public record GetPoolParam(Map<String, List<Long>> map, String name, OsuMode mode) {}

    @Override
    public boolean isHandle(@NotNull MessageEvent event, @NotNull String messageText, @NotNull DataValue<GetPoolParam> data) throws Throwable {
        var matcher = Instruction.GET_POOL.matcher(messageText);
        if (!matcher.find()) return false;

        var dataStr = matcher.group("data");
        var nameStr = matcher.group("name");

        if (! StringUtils.hasText(dataStr)) {
            try {
                var md = DataUtil.getMarkdownFile("Help/getpool.md");
                var image = imageService.getPanelA6(md, "help");
                event.reply(image);
                return true;
            } catch (Exception e) {
                throw new MapPoolException(MapPoolException.Type.GP_Instructions);
            }
        }

        var dataMap = parseDataString(dataStr);
        var mode = CmdUtil.getMode(matcher, getFirstMapMode(dataStr)).getData();

        data.setValue(new GetPoolParam(dataMap, nameStr, mode));
        return true;
    }

    private OsuMode getModeOrElse(OsuMode setMode, OsuMode mapMode) {
        if (mapMode == OsuMode.OSU) {
            if (OsuMode.isDefaultOrNull(setMode)) {
                return mapMode;
            } else {
                return setMode;
            }
        } else if (OsuMode.isDefaultOrNull(mapMode)) {
            return setMode;
        } else {
            return mapMode;
        }
    }

    @Override
    public void HandleMessage(MessageEvent event, GetPoolParam param) throws Throwable {
        var mapPool = new MapPoolDto(param.name(), param.map(), osuBeatmapApiService);

        if (mapPool.getModPools().isEmpty()) throw new MapPoolException(MapPoolException.Type.GP_Map_Empty);

        try {
            var image = imageService.getPanelH(mapPool, param.mode());
            event.reply(image);
        } catch (Exception e) {
            log.error("GP 数据请求失败", e);
            throw new MapPoolException(MapPoolException.Type.GP_Send_Error);
        }
    }

    @NonNull
    public OsuMode getFirstMapMode(String dataStr) {
        String[] dataStrArray = dataStr.trim().split("[\"\\s,，\\-|:]+");
        if (dataStr.isBlank() || dataStrArray.length == 0) return OsuMode.DEFAULT;

        long bid = 0L;

        for (var s : dataStrArray) {
            if (s == null || s.isBlank()) continue;

            try {
                bid = Long.parseLong(s);
                break;
            } catch (NumberFormatException ignored) {

            }
        }

        if (bid == 0L) {
            return OsuMode.DEFAULT;
        } else {
            try {
                var b = osuBeatmapApiService.getBeatMapFromDataBase(bid);
                return b.getMode();
            } catch (Exception e) {
                return OsuMode.DEFAULT;
            }
        }

    }

    @Nullable
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
