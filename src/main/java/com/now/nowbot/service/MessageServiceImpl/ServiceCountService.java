package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.aop.CheckPermission;
import com.now.nowbot.entity.ServiceCallLite;
import com.now.nowbot.mapper.ServiceCallRepository;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.util.Instructions;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service("SERVICE_COUNT")
public class ServiceCountService implements MessageService<Integer> {
    private final ServiceCallRepository serviceCallRepository;
    private final ImageService imageService;
    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yy/MM/dd HH:mm");

    public ServiceCountService(ServiceCallRepository serviceCallRepository, ImageService imageService) {
        this.serviceCallRepository = serviceCallRepository;
        this.imageService = imageService;
    }

    @Override
    public boolean isHandle(MessageEvent event, String messageText, DataValue<Integer> data) throws Throwable {
        var matcher = Instructions.SERVICE_COUNT.matcher(messageText);
        if (! matcher.find()) return false;

        var d = matcher.group("days");
        var h = matcher.group("hours");
        int hours = 0;
        boolean hasDays = true;

        try {
            hours += 24 * Integer.parseInt(d);
        } catch (NumberFormatException ignored) {
            hasDays = false;
        }

        try {
            hours += Integer.parseInt(h);
        } catch (NumberFormatException e) {
            if (! hasDays) {
                hours = 7 * 24;
            }
        }

        data.setValue(hours);
        return true;
    }

    @Override
    @CheckPermission(isSuperAdmin = true)
    public void HandleMessage(MessageEvent event, Integer hours) throws Throwable {
        var from = event.getSubject();
        StringBuilder sb = new StringBuilder();
        List<ServiceCallLite.ServiceCallResult> result;
        List<ServiceCallLite.ServiceCallResult$80> r80;
        if (Objects.isNull(hours) || hours == 0) {
            result = serviceCallRepository.countAll();
            var now = LocalDateTime.now();
            r80 = serviceCallRepository.countBetween$80(now.minusHours(24), now);
            sb.append("## 时间段：迄今为止\n");
        } else {
            var now = LocalDateTime.now();
            var before = now.minusHours(hours);
            //event.getSubject().sendMessage(STR."处理 [\{before.format(dateTimeFormatter)}] - [\{now.format(dateTimeFormatter)}]");
            sb.append(STR."## 时间段：**\{before.format(dateTimeFormatter)}** - **\{now.format(dateTimeFormatter)}**\n");
            result = serviceCallRepository.countBetween(before, now);
            r80 = serviceCallRepository.countBetween$80(before, now);
        }
        Map<String, Long> r80map = r80.stream().collect(Collectors.toMap(
                ServiceCallLite.ServiceCallResult$80::getService,
                ServiceCallLite.ServiceCallResult$80::getData
        ));

        sb.append("""
                | 服务名 | 调用次数 | 平均用时 | 最大用时 | 最小用时 | 90% |
                |:-------|:--------:|:---------:|:---------:|:---------:|:---------:|
                """);
        Consumer<ServiceCallLite.ServiceCallResult> work = r -> sb
                .append('|').append(r.getService())
                .append('|').append(r.getSize())
                .append('|').append(Math.round(r.getAvgTime()) / 1000D).append('s')
                .append('|').append(r.getMaxTime() / 1000D).append('s')
                .append('|').append(r.getMinTime() / 1000D).append('s')
                .append('|').append(r80map.get(r.getService()) / 1000D).append('s')
                .append("|\n");
        result.forEach(work);

        var image = imageService.getPanelA6(sb.toString(), "service");
        from.sendImage(image);
    }
}
