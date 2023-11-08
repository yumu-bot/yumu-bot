package com.now.nowbot.service.MessageServiceImpl;

import com.fasterxml.jackson.databind.JsonNode;
import com.mikuac.shiro.core.BotContainer;
import com.now.nowbot.model.JsonData.MicroUser;
import com.now.nowbot.qq.contact.Group;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuGetService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import reactor.netty.transport.ProxyProvider;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Service("STATISTICAL")
public class StatisticalOverPPService implements MessageService<Long> {
    private static final Logger log = LoggerFactory.getLogger(StatisticalOverPPService.class);
    private final BotContainer bots;
    private final WebClient client;
    private final OsuGetService osuGetService;

    public StatisticalOverPPService(WebClient.Builder webClient, BotContainer botContainer, OsuGetService osuGetService) {
        ConnectionProvider connectionProvider = ConnectionProvider.builder("connectionProvider")
                .maxIdleTime(Duration.ofSeconds(30))
                .build();
        HttpClient httpClient = HttpClient.create(connectionProvider)
                .proxy(proxy ->
                        proxy.type(ProxyProvider.Proxy.SOCKS5)
                                .host("127.0.0.1")
                                .port(7890)
                )
                .followRedirect(true)
                .responseTimeout(Duration.ofSeconds(30));
        ReactorClientHttpConnector connector = new ReactorClientHttpConnector(httpClient);
        client = webClient.clientConnector(connector).build();
        bots = botContainer;
        this.osuGetService = osuGetService;
    }

    private Long getOsuId(Long qq) {
        return client.get()
                .uri("https://api.bleatingsheep.org/api/Binding/{qq}", qq)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .<Long>handle((json, sink) -> {
                    if (!json.hasNonNull("userId")) {
                        sink.error(WebClientResponseException.create(404, "NOT FOUND", null, null, null));
                        return;
                    }
                    sink.next(json.get("userId").asLong());
                })
                .block();
    }

    private float getOsuBp1(Long osuId) {
        return client.get()
                .uri("https://osu.ppy.sh/users/{osuId}/scores/best?mode=osu&limit=1", osuId)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .<Double>handle((json, sink) -> {
                    if (!json.isArray() || json.isEmpty()) {
                        sink.error(WebClientResponseException.create(404, "NOT FOUND", null, null, null));
                        return;
                    }
                    var b1 = json.get(0);

                    sink.next(json.get("pp").asDouble(0));
                })
                .block()
                .floatValue();
    }

    @Override
    public boolean isHandle(MessageEvent event, DataValue<Long> data) throws Throwable {
        if (!(event.getSubject() instanceof Group)) {
            return false;
        }
        String message = event.getRawMessage();
        if (message.startsWith("!统计超限")) {
            if (message.endsWith("新人群")) {
                data.setValue(595985887L);
                return true;
            } else if (message.endsWith("进阶群")) {
                data.setValue(928936255L);
                return true;
            } else if (message.endsWith("高阶群")) {
                data.setValue(281624271L);
                return true;
            }
        }
        return false;

    }

    @Override
    public void HandleMessage(MessageEvent event, Long group) throws Throwable {
        com.mikuac.shiro.core.Bot bot = bots.robots.get(1563653406L);
        if (Objects.isNull(bot)) {
            event.getSubject().sendMessage("主bot未在线");
            return;
        }
        event.getSubject().sendMessage("开始统计: " + group);

        var groupInfo = bot.getGroupMemberList(group).getData();
        groupInfo = groupInfo.stream().filter(r -> r.getRole().equalsIgnoreCase("member")).toList();
        /**
         * qq-info
         */
        Map<Long, MicroUser> users = new HashMap<>(groupInfo.size());
        /**
         * qq-bp1
         */
        Map<Long, Float> usersBP1 = new HashMap<>(groupInfo.size());
        /**
         * uid-qq
         */
        Map<Long, Long> nowOsuId = new HashMap<>(50);
        for (var u : groupInfo) {
            Thread.sleep(3000);
            long qq = u.getUserId();
            long id;
            try {
                id = getOsuId(qq);
                float bp1 = getOsuBp1(id);
                nowOsuId.put(id, qq);
                usersBP1.put(qq, bp1);
                log.info("统计 [{}] 信息获取成功. {}pp", qq, bp1);
            } catch (Exception err) {
                log.error("统计出现异常: {}", qq, err);
                users.put(qq, null);
            }
            if (nowOsuId.size() >= 50) {
                var result = osuGetService.getUsers(nowOsuId.keySet());
                for (var uInfo : result) {
                    users.put(nowOsuId.get(uInfo.getId()), uInfo);
                }
                nowOsuId.clear();
            }
        }

        StringBuilder sb = new StringBuilder("qq,id,name,pp,bp1\n");

        users.entrySet().stream().sorted(Comparator.comparing(e -> e.getValue().getRulesets().getOsu().getPP()))
                .forEach(entry -> {
                    sb.append(entry.getKey()).append(',');
                    if (Objects.isNull(entry.getValue())) {
                        sb.append("加载失败").append('\n');
                        return;
                    }
                    sb.append(entry.getValue().getId()).append(',');
                    sb.append(entry.getValue().getUserName()).append(',');
                    sb.append(entry.getValue().getRulesets().getOsu().getPP()).append(',');
                    sb.append(usersBP1.get(entry.getKey())).append('\n');
                });
        var from = event.getSubject();
        if (from instanceof Group groupSend) {
            groupSend.sendFile(sb.toString().getBytes(StandardCharsets.UTF_8), group + ".csv");
        }
    }
}
