package com.now.nowbot.service.MessageServiceImpl;

import com.fasterxml.jackson.databind.JsonNode;
import com.now.nowbot.config.NowbotConfig;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuApiService.OsuBeatmapApiService;
import com.now.nowbot.throwable.ServiceException.MapPoolException;
import com.now.nowbot.throwable.TipsException;
import com.now.nowbot.util.QQMsgUtil;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.util.*;
import java.util.regex.Pattern;

@Service("MAPPOOL")
public class MapPoolService implements MessageService<String> {
    private static final Logger log = LoggerFactory.getLogger(MapPoolService.class);
    private static String           api   = NowbotConfig.BS_API_URL;
    private static Optional<String> token = NowbotConfig.BS_TOKEN;
    @Resource
    OsuBeatmapApiService osuBeatmapApiService;
    private static Pattern pattern = Pattern.compile("^!pool\\s*(?<name>\\w+)");
    @Resource
    ImageService imageService;
    @Resource
    WebClient webClient;

    @Override
    public boolean isHandle(MessageEvent event, DataValue<String> data) {
//        var m = Instructions.MAPPOOL.matcher(event.getRawMessage().trim());
        var m = pattern.matcher(event.getRawMessage().trim());
        if (! m.find()) {
            return false;
        }
        data.setValue(m.group("name"));
        return true;
    }

    @Override
    public void HandleMessage(MessageEvent event, String param) throws Throwable {
        final int id;
        byte[] img;
        try {
            id = Integer.parseInt(param);
            var p = searchById(id);
            img = imageService.getPanelH(p.orElseThrow(() -> new TipsException(STR."未找到id为 \{id} 的图池")));
        } catch (NumberFormatException formatException) {
            var result = searchByName(param);
            if (! result.isArray() || result.isEmpty()) throw new TipsException(STR."未找到名称包含 \{param} 的图池");
            if (result.size() == 1) {
                img = imageService.getPanelH(result.get(0));
            } else {
                throw new TipsException("查到了多个图池, 请确结果保唯一");
            }
        }

        QQMsgUtil.sendImage(event.getSubject(), img);

        /*
        var dataStr = matcher.group("data");
        var nameStr = matcher.group("name");

        if (dataStr == null || dataStr.isBlank()) {
            throw new MapPoolException(MapPoolException.Type.PO_Parameter_None);
        }

        var data = parseDataString(dataStr);
        var mapPool = new MapPool(nameStr, data, osuBeatmapApiService);

        if (mapPool.getModPools().isEmpty()) throw new MapPoolException(MapPoolException.Type.PO_Map_Empty);

        var from = event.getSubject();

        byte[] img;
        try {
            img = imageService.getPanelH(mapPool);
            QQMsgUtil.sendImage(from, img);
        } catch (Exception e) {
            log.error("PO 数据请求失败", e);
            throw new MapPoolException(MapPoolException.Type.PO_Send_Error);
        }
*/
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
                    } else throw new MapPoolException(MapPoolException.Type.PO_Parse_MissingMap, s, String.valueOf(i));
                }
                case 1 -> {
                    if (Objects.nonNull(mod)) {
                        if (BIDs.isEmpty()) {
                            throw new MapPoolException(MapPoolException.Type.PO_Parse_MissingMap, s, String.valueOf(i));
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

    private Optional<JsonNode> searchById(int id) {
        try {
            return webClient.get()
                    .uri(u -> UriComponentsBuilder.fromHttpUrl(api).path("/api/public/searchPool").queryParam("poolId", id).build().toUri())
                    .headers(h -> token.ifPresent(t -> h.addIfAbsent("AuthorizationX", t)))
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .blockOptional(Duration.ofSeconds(30));
        } catch (WebClientResponseException.NotFound e) {
            return Optional.empty();
        }
    }

    public JsonNode searchByName(String name) {
        var nodeOpt = webClient.get()
                .uri(u -> UriComponentsBuilder.fromHttpUrl(api).path("/api/public/searchPool").queryParam("poolName", name).build().toUri())
                .headers(h -> token.ifPresent(t -> h.addIfAbsent("AuthorizationX", t)))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .blockOptional(Duration.ofSeconds(30));
        return nodeOpt.map(node -> node.get("data")).orElseThrow();
    }
}
