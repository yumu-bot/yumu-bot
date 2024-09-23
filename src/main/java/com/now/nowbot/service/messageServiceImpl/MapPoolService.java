package com.now.nowbot.service.messageServiceImpl;

import com.fasterxml.jackson.databind.JsonNode;
import com.now.nowbot.config.NowbotConfig;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.model.mappool.now.Pool;
import com.now.nowbot.model.mappool.old.MapPoolDto;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.osuApiService.OsuBeatmapApiService;
import com.now.nowbot.throwable.TipsException;
import com.now.nowbot.util.ASyncMessageUtil;
import com.now.nowbot.util.CmdUtil;
import com.now.nowbot.util.Instruction;
import com.now.nowbot.util.JacksonUtil;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static com.now.nowbot.util.command.CommandPatternStaticKt.FLAG_NAME;

@Service("MAP_POOL")
public class MapPoolService implements MessageService<MapPoolService.PoolParam> {

    private static final String           api   = NowbotConfig.BS_API_URL;
    private static final Optional<String> token = NowbotConfig.BS_TOKEN;
    @Resource
    ImageService         imageService;
    @Resource
    OsuBeatmapApiService osuBeatmapApiService;
    @Resource
    WebClient            webClient;

    public record PoolParam(int id, String name, OsuMode mode) {}

    @Override
    public boolean isHandle(MessageEvent event, String messageText, DataValue<PoolParam> data) throws TipsException {
        var m = Instruction.MAP_POOL.matcher(messageText);
        if (!m.find()) return false;

        String name = m.group(FLAG_NAME);
        OsuMode mode = CmdUtil.getMode(m).getData();

        if (! StringUtils.hasText(name)) {
            throw new TipsException("id 解析错误, 请确保只有数字");
        }

        try {
            int id = Integer.parseInt(name);
            data.setValue(new PoolParam(id, null, mode));
        } catch (NumberFormatException e) {
            data.setValue(new PoolParam(0, name, mode));
        }

        return true;
    }

    @Override
    public void HandleMessage(MessageEvent event, PoolParam param) throws Throwable {
        byte[] image;
        if (StringUtils.hasText(param.name())) {
            var result = searchByName(param.name());
            if (result.isEmpty())
                throw new TipsException(STR."未找到名称包含 \{param.name()} 的图池");
            if (result.size() == 1) {
                image = imageService.getPanelH(new MapPoolDto(result.getFirst(), osuBeatmapApiService), param.mode());
            } else {
                StringBuilder sb = new StringBuilder("查到了多个图池, 请确认结果:\n");
                for (int i = 0; i < result.size(); i++) {
                    sb.append(i + 1).append(": ").append(result.get(i).getName()).append('\n');
                }
                sb.append("p.s. 请直接发送选项对应的数字");

                var image2 = imageService.getPanelAlpha(sb);
                event.reply(image2);

                var lock = ASyncMessageUtil.getLock(event);
                var newEvent = lock.get();
                int n = 0;
                try {
                    n = Integer.parseInt(newEvent.getRawMessage());
                } catch (NumberFormatException e) {
                    throw new TipsException("输入错误");
                }
                if (n < 1 || n > result.size()) throw new TipsException("输入错误");

                image = imageService.getPanelH(new MapPoolDto(result.get(n - 1), osuBeatmapApiService), param.mode());
            }
        } else {
            var p = searchById(param.id());
            image = imageService.getPanelH(p.map(pool -> new MapPoolDto(pool, osuBeatmapApiService)).orElseThrow(() -> new TipsException(STR."未找到id为 \{param.id()} 的图池")), param.mode());
        }

        event.reply(image);
    }

    public List<Pool> searchByName(String name) {
        var nodeOpt = webClient.get()
                .uri(u -> UriComponentsBuilder.fromHttpUrl(api).path("/api/public/searchPool").queryParam("poolName", name).build().toUri())
                .headers(h -> token.ifPresent(t -> h.addIfAbsent("AuthorizationX", t)))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .blockOptional(Duration.ofSeconds(30));
        return nodeOpt.map(node -> JacksonUtil.parseObjectList(node.get("data"), Pool.class)).orElseThrow();
    }

    public Optional<Pool> searchById(int id) {
        try {
            return webClient.get()
                    .uri(u -> UriComponentsBuilder.fromHttpUrl(api).path("/api/public/searchPool").queryParam("poolId", id).build().toUri())
                    .headers(h -> token.ifPresent(t -> h.addIfAbsent("AuthorizationX", t)))
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .<Optional<Pool>>map(json -> json.has("data") ? Optional.ofNullable(JacksonUtil.parseObject(json.get("data"), Pool.class)) : Optional.empty())
                    .block(Duration.ofSeconds(30));
        } catch (HttpClientErrorException.NotFound | WebClientResponseException.NotFound e) {
            return Optional.empty();
        }
    }
}