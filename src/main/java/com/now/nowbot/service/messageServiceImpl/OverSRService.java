package com.now.nowbot.service.messageServiceImpl;

import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.throwable.serviceException.OverSRException;
import com.now.nowbot.util.Instruction;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;

@Service("OVER_SR")
public class OverSRService implements MessageService<Matcher> {

    @Override
    public boolean isHandle(@NotNull MessageEvent event, @NotNull String messageText, @NotNull DataValue<Matcher> data) {
        var m = Instruction.OVER_SR.matcher(messageText);
        if (m.find()) {
            data.setValue(m);
            return true;
        } else return false;
    }

    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        String SRStr = matcher.group("SR");
        double SR;
        String message;

        try {
            SR = Double.parseDouble(SRStr);
        } catch (Exception e) {
            if (e instanceof NullPointerException) throw new OverSRException(OverSRException.Type.OV_Parameter_Null);
            else throw new OverSRException(OverSRException.Type.OV_Parameter_Error);
        }

        message = OverSR(SR);
        event.getSubject().sendMessage(message);
    }

    private String OverSR(double SR) throws OverSRException {
        StringBuilder message = new StringBuilder();
        double minute;
        double hour;
        double day;

        if ((int) (SR * 100f) <= 570) {
            throw new OverSRException(OverSRException.Type.OV_Send_Success);
        } else if ((int) (SR * 100f) <= 2000){
            // 超 0.01 星加 10 分钟，6星 以上所有乘以二
            if ((int) (SR * 100f) <= 600) {
                minute = (SR - 5.7f) * 1000f;
            } else {
                minute = (SR - 5.7f) * 2000f;
            }
        } else {
            throw new OverSRException(OverSRException.Type.OV_Parameter_OutOfRange);
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
