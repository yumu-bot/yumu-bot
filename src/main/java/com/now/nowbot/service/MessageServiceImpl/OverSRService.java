package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.MessageService;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;

@Service("OverSR")
public class OverSRService implements MessageService {
    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        String SRStr = matcher.group("SR");
        double SR;
        String message;

        if (SRStr != null){
            try {
                SR = Double.parseDouble(SRStr);
                message = OverSR(SR);

            } catch (Exception e) {
                e.printStackTrace();
                message = "捞翔恁发嘞是个啥玩应啊？";
            }

        } else {
            message = "请输入正确的星数！"; //好像不该这么抛错
        }

        event.getSubject().sendMessage(message);
    }

    private String OverSR(double SR) {
        StringBuilder message = new StringBuilder();
        double minute;
        double hour;
        double day;

        if ((int) (SR * 100f) <= 570) {
            return "未超星";
        } else if ((int) (SR * 100f) <= 2000){
            // 超 0.01 星加 10 分钟，6星 以上所有乘以二
            if ((int) (SR * 100f) <= 600) {
                minute = (SR - 5.7f) * 1000f;
            } else {
                minute = (SR - 5.7f) * 2000f;
            }
        } else {
            return "对方真的糊了那么高星的图吗？还是说你在滥用功能...";
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
