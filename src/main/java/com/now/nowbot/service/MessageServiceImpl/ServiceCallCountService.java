package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.aop.CheckPermission;
import com.now.nowbot.entity.ServiceCallLite;
import com.now.nowbot.mapper.ServiceCallRepository;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.util.QQMsgUtil;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

@Service("SERVICE_CALL_COUNT")
public class ServiceCallCountService implements MessageService<Integer> {
    private final ServiceCallRepository serviceCallRepository;
    private final ImageService          imageService;
    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yy/MM/dd:HH");

    public ServiceCallCountService(ServiceCallRepository serviceCallRepository,
                                   ImageService imageService) {
        this.serviceCallRepository = serviceCallRepository;
        this.imageService = imageService;
    }

    @Override
    public boolean isHandle(MessageEvent event, DataValue<Integer> data) throws Throwable {
        if (! event.getRawMessage().startsWith("统计服务调用")) return false;
        var s = event.getRawMessage().split("\\s+");
        if (s.length == 2) {
            try {
                data.setValue(Integer.parseInt(s[1]));
            } catch (NumberFormatException ignore) {
            }
        }
        return true;
    }

    @Override
    @CheckPermission(isSuperAdmin = true)
    public void HandleMessage(MessageEvent event, Integer hours) throws Throwable {
        StringBuilder sb = new StringBuilder("""
                | 服务名 | 调用次数 | 平均用时 | 最大用时 | 最小用时 |
                |-------|--------|---------|---------|---------|
                """);
        List<ServiceCallLite.ServiceCallResult> result;
        if (Objects.isNull(hours)) {
            result = serviceCallRepository.countAll();
        } else {
            var now = LocalDateTime.now();
            var bef = now.minusHours(hours);
            event.getSubject().sendMessage(STR."处理 [\{bef.format(dateTimeFormatter)}] - [\{now.format(dateTimeFormatter)}]");
            result = serviceCallRepository.countBetwen(bef, now);
        }
        Consumer<ServiceCallLite.ServiceCallResult> work = r -> sb
                .append('|').append(r.getService())
                .append('|').append(r.getSize())
                .append('|').append(r.getAvgTime().intValue())
                .append('|').append(r.getMaxTime())
                .append('|').append(r.getMinTime())
                .append("|\n");
        result.forEach(work);
        var s = imageService.getMarkdownImage(sb.toString(), 600);
        QQMsgUtil.sendImage(event.getSubject(), s);
    }
}
