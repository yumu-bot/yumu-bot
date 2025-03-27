package com.now.nowbot.service.messageServiceImpl;

import com.now.nowbot.aop.CheckPermission;
import com.now.nowbot.config.Permission;
import com.now.nowbot.entity.ServiceCallLite;
import com.now.nowbot.mapper.ServiceCallRepository;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.throwable.GeneralTipsException;
import com.now.nowbot.util.Instruction;
import org.jetbrains.annotations.NotNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
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
    public boolean isHandle(@NotNull MessageEvent event, @NotNull String messageText, @NotNull DataValue<Integer> data) throws Throwable {
        var matcher = Instruction.SERVICE_COUNT.matcher(messageText);
        if (! matcher.find()) return false;

        if (!Permission.isSuperAdmin(event.getSender().getId())) {
            throw new GeneralTipsException(GeneralTipsException.Type.G_Permission_Super);
        }

        var d = matcher.group("days");
        var h = matcher.group("hours");
        Integer hours = 0;
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
                hours = null;
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

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime before;

        if (Objects.isNull(hours)) {
            before = now.minusHours(24);
            result = serviceCallRepository.countBetween(before, now);
            sb.append("## 时间段：今天之内\n");
        } else if (hours == 0) {
            before = LocalDateTime.of(1900, 1, 1, 0, 0, 0);
            result = serviceCallRepository.countAll();
            sb.append("## 时间段：迄今为止\n");
        } else {
            before = now.minusHours(hours);
            sb.append(STR."## 时间段：**\{before.format(dateTimeFormatter)}** - **\{now.format(dateTimeFormatter)}**\n");
            result = serviceCallRepository.countBetween(before, now);
        }

        Map<String, Long> r1 = serviceCallRepository.countBetweenLimit(before, now, 0.01)
                .stream().collect(Collectors.toMap(
                ServiceCallLite.ServiceCallResultLimit::getService,
                ServiceCallLite.ServiceCallResultLimit::getData
        ));
        Map<String, Long> r50 = serviceCallRepository.countBetweenLimit(before, now, 0.50)
                .stream().collect(Collectors.toMap(
                ServiceCallLite.ServiceCallResultLimit::getService,
                ServiceCallLite.ServiceCallResultLimit::getData
        ));
        Map<String, Long> r80 = serviceCallRepository.countBetweenLimit(before, now, 0.80)
                .stream().collect(Collectors.toMap(
                ServiceCallLite.ServiceCallResultLimit::getService,
                ServiceCallLite.ServiceCallResultLimit::getData
        ));
        Map<String, Long> r99 = serviceCallRepository.countBetweenLimit(before, now, 0.99)
                .stream().collect(Collectors.toMap(
                ServiceCallLite.ServiceCallResultLimit::getService,
                ServiceCallLite.ServiceCallResultLimit::getData
        ));

        Consumer(sb, result, r1, r50, r80, r99);

        var image = imageService.getPanelA6(sb.toString(), "service");
        event.reply(image);
    }

    //
    private void Consumer(StringBuilder sb, List<ServiceCallLite.ServiceCallResult> result,
                          Map<String, Long> r1, Map<String, Long> r50, Map<String, Long> r80, Map<String, Long> r99) {
        if (Objects.isNull(result)) return;

        sb.append("""
                | 服务名 | 调用次数 | 最长用时 (100%) | 99% | 80% | 50% | 1% | 最短用时 (0%) |
                | :-- | :-: | :-: | :-: | :-: | :-: | :-: | :-: |
                """);

        int sum = 0;
        var maxList = new ArrayList<Long>();
        var r99List = new ArrayList<Long>();
        var r80List = new ArrayList<Long>();
        var r50List = new ArrayList<Long>();
        var r1List = new ArrayList<Long>();
        var minList = new ArrayList<Long>();

        for (var r : result) {
            var s = r.getService();
            var size = Optional.ofNullable(r.getSize()).orElse(0);

            sum += size;
            maxList.add(r.getMaxTime() * size);
            r99List.add(r99.getOrDefault(s, 0L) * size);
            r80List.add(r80.getOrDefault(s, 0L) * size);
            r50List.add(r50.getOrDefault(s, 0L) * size);
            r1List.add(r1.getOrDefault(s, 0L) * size);
            minList.add(r.getMinTime() * size);

            sb.append("| ").append(r.getService())
                    .append(" | ").append(size)
                    .append(" | ").append(getString(r.getMaxTime())).append('s')
                    .append(" | ").append(getString(r99.getOrDefault(s, 0L))).append('s')
                    .append(" | ").append(getString(r80.getOrDefault(s, 0L))).append('s')
                    .append(" | ").append(getString(r50.getOrDefault(s, 0L))).append('s')
                    .append(" | ").append(getString(r1.getOrDefault(s, 0L))).append('s')
                    .append(" | ").append(getString(r.getMinTime())).append('s')
                    .append(" |\n");
        }

        sb.append("| ").append("总计和平均")
                .append(" | ").append(sum)
                .append(" | ").append(getString(getListAverage(maxList, sum))).append('s')
                .append(" | ").append(getString(getListAverage(r99List, sum))).append('s')
                .append(" | ").append(getString(getListAverage(r80List, sum))).append('s')
                .append(" | ").append(getString(getListAverage(r50List, sum))).append('s')
                .append(" | ").append(getString(getListAverage(r1List, sum))).append('s')
                .append(" | ").append(getString(getListAverage(minList, sum))).append('s')
                .append(" |\n");
    }

    //数组求平均值
    private float getListAverage(List<Long> list, int sum) {
        if (CollectionUtils.isEmpty(list) || sum == 0) return 0f;
        else return 1f * list.stream().reduce(Long::sum).orElse(0L) / sum;
    }

    //1926ms -> 1.9s
    private <T extends Number> String getString(@Nullable T millis) {
        if (millis == null) return "0";

        String str = String.format("%.1f", Math.round(millis.floatValue() / 100f) / 10f);

        if (str.endsWith(".0")) {
            return str.replace(".0", "");
        }

        return str;
    }
}
