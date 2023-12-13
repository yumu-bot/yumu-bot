package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.aop.CheckPermission;
import com.now.nowbot.entity.ServiceCallLite;
import com.now.nowbot.mapper.ServiceCallRepository;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.util.QQMsgUtil;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;

@Service("SERVICE_CALL_COUNT")
public class ServiceCallCountService implements MessageService<Object> {
    private final ServiceCallRepository serviceCallRepository;
    private final ImageService          imageService;

    public ServiceCallCountService(ServiceCallRepository serviceCallRepository,
                                   ImageService imageService) {
        this.serviceCallRepository = serviceCallRepository;
        this.imageService = imageService;
    }

    @Override
    public boolean isHandle(MessageEvent event, DataValue<Object> data) throws Throwable {
        return event.getRawMessage().equals("统计服务调用");
    }

    @Override
    @CheckPermission(isSuperAdmin = true)
    public void HandleMessage(MessageEvent event, Object matcher) throws Throwable {
        StringBuilder sb = new StringBuilder("""
                | 服务名 | 调用次数 | 平均用时 | 最大用时 | 最小用时 |
                |-------|--------|---------|---------|---------|
                """);
        var result = serviceCallRepository.countAll();
        Consumer<ServiceCallLite.ServiceCallResult> work = r -> sb
                .append('|').append(r.getService())
                .append('|').append(r.getSize())
                .append('|').append(r.getAvgTime().intValue())
                .append('|').append(r.getMaxTime())
                .append('|').append(r.getMinTime())
                .append("|\n");
        result.forEach(work);
        var s = imageService.getPanelAlpha(sb);
        QQMsgUtil.sendImage(event.getSubject(), s);
    }
}
