package com.now.nowbot.service.MessageServiceImpl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.mikuac.shiro.core.BotContainer;
import com.now.nowbot.config.FileConfig;
import com.now.nowbot.model.JsonData.MicroUser;
import com.now.nowbot.qq.contact.Group;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.util.JacksonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import reactor.netty.transport.ProxyProvider;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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

    private static final Map<Long, Long> UserCache = new HashMap<>();

    private static int lock = 0;
    private final Path CachePath;

    public StatisticalOverPPService(WebClient.Builder webClient,
                                    BotContainer botContainer,
                                    OsuGetService osuGetService,
                                    FileConfig config
    ) {
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

        CachePath = Path.of(config.getRoot(), "StatisticalOverPPService.json");
        try {
            if (Files.isRegularFile(CachePath)) {
                String jsonStr = Files.readString(CachePath);
                HashMap<Long, Long> cache = JacksonUtil.parseObject(jsonStr, new TypeReference<HashMap<Long, Long>>() {
                });
                if (Objects.nonNull(cache)) {
                    UserCache.putAll(cache);
                }
            } else {
                Files.createFile(CachePath);
            }
        } catch (IOException e) {
            log.error("文件操作失败", e);
        }
    }

    private Long getOsuId(Long qq) {
        if (UserCache.containsKey(qq)) {
            return UserCache.get(qq);
        }
        Long id = client.get()
                .uri("https://api.bleatingsheep.org/api/Binding/{qq}", qq)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .<Long>handle((json, sink) -> {
                    if (!json.hasNonNull("userId")) {
                        sink.error(WebClientResponseException.create(404, "NOT FOUND", null, null, null));
                        return;
                    }
                    sink.next(json.get("osuId").asLong());
                })
                .block();
        UserCache.put(qq, id);
        return id;

    }

    public float getOsuBp1(Long osuId) {
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

                    sink.next(b1.get("pp").asDouble(0));
                })
                .block()
                .floatValue();
    }

    @Override
    public boolean isHandle(MessageEvent event, DataValue<Long> data) throws Throwable {
        if (!(event.getSubject() instanceof Group) || lock == 0) {
            return false;
        }
        lock = 3;
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
        var from = event.getSubject();
        if (from instanceof Group groupSend) {
            try {
                work(groupSend, group);
            } catch (Exception e) {
                log.error("出现错误:", e);
            } finally {
                lock = 0;
                Files.writeString(CachePath, Objects.requireNonNull(JacksonUtil.toJson(UserCache)));
            }
        }
    }

    private void work(Group group, Long groupId) throws Exception {
        com.mikuac.shiro.core.Bot bot = bots.robots.get(1563653406L);
        if (Objects.isNull(bot)) {
            group.sendMessage("主bot未在线");
            return;
        }
        group.sendMessage("开始统计: " + groupId);

        var groupInfo = bot.getGroupMemberList(groupId).getData();
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

        int count = 0;
        for (var u : groupInfo) {
            long qq = u.getUserId();
            long id;
            try {
                Thread.sleep(1000);
                id = getOsuId(qq);
                float bp1 = getOsuBp1(id);
                nowOsuId.put(id, qq);
                usersBP1.put(qq, bp1);
                log.info("统计 [{}] 信息获取成功. {}pp", qq, bp1);
            } catch (WebClientResponseException.TooManyRequests err) {
                Thread.sleep(20000);
                id = getOsuId(qq);
                float bp1 = getOsuBp1(id);
                nowOsuId.put(id, qq);
                usersBP1.put(qq, bp1);
                log.info("统计 [{}] 信息获取成功. {}pp", qq, bp1);
            } catch (Exception err) {
                log.error("统计出现异常: {}", qq, err);
                users.put(qq, null);
            }
            count++;
            if (count % 50 == 0) {
                group.sendMessage(String.format("%d 统计进行到 %.2f%%", groupId, 100f * count / groupInfo.size()));
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

        users.entrySet().stream()
                .sorted(Comparator.<Map.Entry<Long, MicroUser>, Double>comparing(e -> {
                    if (Objects.isNull(e.getValue())) return 0D;
                    return e.getValue().getRulesets().getOsu().getPP();
                }).reversed())
                .forEach(entry -> {
                    sb.append('\'').append(entry.getKey()).append(',');
                    if (Objects.isNull(entry.getValue())) {
                        sb.append("加载失败").append('\n');
                        return;
                    }
                    sb.append(entry.getValue().getId()).append(',');
                    sb.append(entry.getValue().getUserName()).append(',');
                    sb.append(entry.getValue().getRulesets().getOsu().getPP()).append(',');
                    sb.append(usersBP1.get(entry.getKey())).append('\n');
                });
        group.sendFile(sb.toString().getBytes(StandardCharsets.UTF_8), groupId + ".csv");
    }
}
