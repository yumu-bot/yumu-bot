package com.now.nowbot.service.MessageService;

import com.now.nowbot.throwable.LogException;
import net.mamoe.mirai.event.events.MessageEvent;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;

@Service("OverSR")
public class OverSRService implements MessageService{
    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        String SRStr = matcher.group("SR");
        double SR;
        String message;

        if (SRStr != null){
            SR = Double.parseDouble(SRStr);
            message = OverSR(SR);
        } else {
            throw new LogException("请输入正确的星数！", null);
        }

        event.getSubject().sendMessage(message);
    }

    private String OverSR(double SR) {
        StringBuilder message = new StringBuilder();
        double minute;
        double hour;
        double day;

        if (SR <= 5.7f) {
            return "未超星";
        } else {
            // 超 0.01 星加 10 分钟，6星 以上所有乘以二
            if (SR < 6f) {
                minute = (SR - 5.7f) * 1000f;
            } else {
                minute = (SR - 5.7f) * 2000f;
            }
        }

        message.append("已超星，预计禁言：");

        if (minute >= 1440f) {
            day = Math.floor(minute / 1440f);
            hour = Math.floor((minute - (day * 1440f)) / 60f);
            minute = Math.floor(minute - (day * 1440f) - (hour * 60f));

            message.append(String.format("%d", (int) day))
                    .append("天")
                    .append(String.format("%d", (int) hour))
                    .append("时")
                    .append(String.format("%d", (int) minute))
                    .append("分");
        } else if (minute >= 60f) {
            hour = Math.floor(minute / 60f);
            minute = Math.floor(minute - (hour * 60f));

            message.append(String.format("%d", (int) hour))
                    .append("时")
                    .append(String.format("%d", (int) minute))
                    .append("分");
        } else {
            message.append(String.format("%d", (int) minute))
                    .append("分");
        }

        return message.toString();
    }
}
