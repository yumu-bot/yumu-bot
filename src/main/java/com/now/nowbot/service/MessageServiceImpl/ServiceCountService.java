package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.aop.CheckPermission;
import com.now.nowbot.config.Permission;
import com.now.nowbot.entity.ServiceCallLite;
import com.now.nowbot.mapper.ServiceCallRepository;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.throwable.TipsException;
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

        if (!Permission.isSuperAdmin(event.getSender().getId())) {
            throw new TipsException("只有超级管理员 (OP，原批) 可以使用此功能！");
        }

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
        List<ServiceCallLite.ServiceCallResultLimit> r1;
        List<ServiceCallLite.ServiceCallResultLimit> r80;
        List<ServiceCallLite.ServiceCallResultLimit> r99;
        if (Objects.isNull(hours) || hours == 0) {
            result = serviceCallRepository.countAll();
            var now = LocalDateTime.now();
            r1 = serviceCallRepository.countBetweenLimit(now.minusHours(24), now, 0.01);
            r80 = serviceCallRepository.countBetweenLimit(now.minusHours(24), now, 0.80);
            r99 = serviceCallRepository.countBetweenLimit(now.minusHours(24), now, 0.99);
            sb.append("## 时间段：迄今为止\n");
        } else {
            var now = LocalDateTime.now();
            var before = now.minusHours(hours);
            //event.getSubject().sendMessage(STR."处理 [\{before.format(dateTimeFormatter)}] - [\{now.format(dateTimeFormatter)}]");
            sb.append(STR."## 时间段：**\{before.format(dateTimeFormatter)}** - **\{now.format(dateTimeFormatter)}**\n");
            result = serviceCallRepository.countBetween(before, now);
            r1 = serviceCallRepository.countBetweenLimit(before, now, 0.01);
            r80 = serviceCallRepository.countBetweenLimit(before, now, 0.80);
            r99 = serviceCallRepository.countBetweenLimit(before, now, 0.99);
        }
        Map<String, Long> r1map = r1.stream().collect(Collectors.toMap(
                ServiceCallLite.ServiceCallResultLimit::getService,
                ServiceCallLite.ServiceCallResultLimit::getData
        ));
        Map<String, Long> r80map = r80.stream().collect(Collectors.toMap(
                ServiceCallLite.ServiceCallResultLimit::getService,
                ServiceCallLite.ServiceCallResultLimit::getData
        ));
        Map<String, Long> r99map = r99.stream().collect(Collectors.toMap(
                ServiceCallLite.ServiceCallResultLimit::getService,
                ServiceCallLite.ServiceCallResultLimit::getData
        ));

        sb.append("""
                | 服务名 | 调用次数 | 最大用时 | 平均用时 | 最小用时 | 1% 用时 | %80 用时 | 99% 用时 |
                |:-------|:--------:|:---------:|:---------:|:---------:|:---------:|
                """);
        Consumer<ServiceCallLite.ServiceCallResult> work = r -> sb
                .append('|').append(r.getService())
                .append('|').append(r.getSize())
                .append('|').append(r.getMaxTime() / 1000D).append('s')
                .append('|').append(Math.round(r.getAvgTime()) / 1000D).append('s')
                .append('|').append(r.getMinTime() / 1000D).append('s')
                .append('|').append(r1map.getOrDefault(r.getService(), 0L) / 1000D).append('s')
                .append('|').append(r80map.getOrDefault(r.getService(), 0L) / 1000D).append('s')
                .append('|').append(r99map.getOrDefault(r.getService(), 0L) / 1000D).append('s')
                .append("|\n");
        result.forEach(work);

        var image = imageService.getPanelA6(sb.toString(), "service");
        from.sendImage(image);
    }
}
