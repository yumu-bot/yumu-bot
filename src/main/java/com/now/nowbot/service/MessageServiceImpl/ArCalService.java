package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.aop.CheckPermission;
import com.now.nowbot.model.enums.Mod;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.util.DateUtil;

import java.util.regex.Pattern;

@org.springframework.stereotype.Service("AR-CALC")
public class ArCalService implements MessageService<ArCalService.Parm> {
    private static Pattern pattern = Pattern.compile("#cal\\s*(?<type>[arodcshp]{2})\\s*(?<value>\\d+(\\.\\d+)?)\\s*\\+?(?<mods>([ezhdtr]{2})+)?");
    record Parm(String type, float value, String mods){}
    @Override
    public boolean isHandle(MessageEvent event, DataValue<Parm> data) throws Throwable {
        String message = event.getRawMessage();
        if (message.startsWith("#cal")) return false;
        var matcher = pattern.matcher(message);
        if (matcher.find()) {
            var d = new Parm(
                    matcher.group("type"),
                    Float.parseFloat(matcher.group("value")),
                    matcher.group("mods")
            );
            data.setValue(d);
            return true;
        }
        return false;
    }

    @Override
    @CheckPermission(isSuperAdmin = true)
    public void HandleMessage(MessageEvent event, Parm parm) throws Throwable {
        int mod;
        if (parm.mods() == null) {
            mod = 0;
        } else {
            mod = Mod.getModsValue(parm.mods());
        }
        // 只针对 std 模式
        String message = switch (parm.type()) {
            case "ar" -> {
                float newAr = DateUtil.AR(parm.value(), mod);
                float ms = DateUtil.AR2MS(newAr);
                yield String.format("AR: %.2f, 缩圈时间: %.2fms", newAr, ms);
            }
            case "od" -> {
                float newOd = DateUtil.AR(parm.value(), mod);
                float ms = DateUtil.OD2MS(newOd);
                yield String.format("OD: %.2f, 300判定区间: %.2fms", newOd, ms);
            }
            case "cs" -> {
                float newCs = DateUtil.CS(parm.value(), mod);
                yield String.format("CS: %.2f", newCs);
            }
            case "hp" -> {
                float newHp = DateUtil.HP(parm.value(), mod);
                yield String.format("HP: %.2f", newHp);
            }
            default -> "Unexpected value: " + parm.type();
        };

        event.getSubject().sendMessage(message);
    }
}
