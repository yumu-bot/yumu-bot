package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.aop.CheckPermission;
import com.now.nowbot.entity.ServiceCallLite;
import com.now.nowbot.mapper.ServiceCallRepository;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.util.Instructions;
import com.now.nowbot.util.QQMsgUtil;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

@Service("SERVICECOUNT")
public class ServiceCountService implements MessageService<Integer> {
    private final ServiceCallRepository serviceCallRepository;
    private final ImageService imageService;
    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yy/MM/dd HH:mm");

    public ServiceCountService(ServiceCallRepository serviceCallRepository, ImageService imageService) {
        this.serviceCallRepository = serviceCallRepository;
        this.imageService = imageService;
    }

    @Override
    public boolean isHandle(MessageEvent event, DataValue<Integer> data) throws Throwable {
        var matcher = Instructions.SERVICECOUNT.matcher(event.getRawMessage().trim());
        if (! matcher.find()) return false;

        //if (! event.getRawMessage().startsWith("统计服务调用")) return false;
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
        StringBuilder sb = new StringBuilder();
        List<ServiceCallLite.ServiceCallResult> result;
        if (Objects.isNull(hours) || hours == 0) {
            result = serviceCallRepository.countAll();
            sb.append("## 时间段：迄今为止\n");
        } else {
            var now = LocalDateTime.now();
            var before = now.minusHours(hours);
            //event.getSubject().sendMessage(STR."处理 [\{before.format(dateTimeFormatter)}] - [\{now.format(dateTimeFormatter)}]");
            sb.append(STR."## 时间段：**\{before.format(dateTimeFormatter)}** - **\{now.format(dateTimeFormatter)}**\n");
            result = serviceCallRepository.countBetween(before, now);
        }

        sb.append("""
                | 服务名 | 调用次数 | 平均用时 | 最大用时 | 最小用时 |
                |:-------|:--------:|:---------:|:---------:|:---------:|
                """);
        Consumer<ServiceCallLite.ServiceCallResult> work = r -> sb
                .append('|').append(r.getService())
                .append('|').append(r.getSize())
                .append('|').append(Math.round(r.getAvgTime()) / 1000D).append('s')
                .append('|').append(r.getMaxTime() / 1000D).append('s')
                .append('|').append(r.getMinTime() / 1000D).append('s')
                .append("|\n");
        result.forEach(work);
        var s = imageService.getPanelA6(sb.toString(), "service");
        QQMsgUtil.sendImage(event.getSubject(), s);
    }
}
