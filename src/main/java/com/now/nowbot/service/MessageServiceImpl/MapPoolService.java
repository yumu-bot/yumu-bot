package com.now.nowbot.service.MessageServiceImpl;

import com.fasterxml.jackson.databind.JsonNode;
import com.now.nowbot.config.NowbotConfig;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.throwable.TipsException;
import com.now.nowbot.util.ASyncMessageUtil;
import com.now.nowbot.util.Instructions;
import com.now.nowbot.util.QQMsgUtil;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.util.Optional;

@Service("MAP_POOL")
public class MapPoolService implements MessageService<MapPoolService.PoolParam> {

    private static final String           api   = NowbotConfig.BS_API_URL;
    private static final Optional<String> token = NowbotConfig.BS_TOKEN;
    @Resource
    ImageService imageService;
    @Resource
    WebClient webClient;

    @Override
    public boolean isHandle(MessageEvent event, DataValue<PoolParam> data) throws TipsException {
        var m = Instructions.MAP_POOL.matcher(event.getRawMessage().trim());
        if (! m.find()) {
            return false;
        }
        ;
        if (StringUtils.hasText(m.group("id"))) {
            try {
                int id = Integer.parseInt(m.group("name"));
                data.setValue(new PoolParam(id, null));
            } catch (NumberFormatException e) {
                throw new TipsException("id 解析错误, 请确保只有数字");
            }
        }
        data.setValue(new PoolParam(0, m.group("name")));
        return true;
    }

    @Override
    public void HandleMessage(MessageEvent event, PoolParam param) throws Throwable {
        final int id;
        byte[] img;
        if (StringUtils.hasText(param.name())) {
            var result = searchByName(param.name());
            if (! result.isArray() || result.isEmpty())
                throw new TipsException(STR."未找到名称包含 \{param.name()} 的图池");
            if (result.size() == 1) {
                img = imageService.getPanelH(result.get(0));
            } else {
                StringBuilder sb = new StringBuilder("查到了多个图池, 请确认结果:\n");
                for (int i = 0; i < result.size(); i++) {
                    sb.append(i + 1).append(": ").append(result.get(i).get("name").asText()).append('\n');
                }
                sb.append("p.s. 请直接发送选项对应的数字");
                QQMsgUtil.sendImage(event.getSubject(), imageService.getPanelAlpha(sb));
                var lock = ASyncMessageUtil.getLock(event);
                var newEvent = lock.get();
                int n = 0;
                try {
                    n = Integer.parseInt(newEvent.getRawMessage());
                } catch (NumberFormatException e) {
                    throw new TipsException("输入错误");
                }
                if (n < 1 || n > result.size()) throw new TipsException("输入错误");

                img = imageService.getPanelH(result.get(n - 1));
            }
        } else {
            var p = searchById(param.id());
            img = imageService.getPanelH(p.orElseThrow(() -> new TipsException(STR."未找到id为 \{param.id()} 的图池")));
        }

        QQMsgUtil.sendImage(event.getSubject(), img);
    }

    public record PoolParam(int id, String name) {
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
