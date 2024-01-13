package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.aop.CheckPermission;
import com.now.nowbot.model.enums.Mod;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.util.DataUtil;
import com.now.nowbot.util.Instructions;
import org.springframework.stereotype.Service;

@Service("MAP4D")
public class Map4DCalculate implements MessageService<Map4DCalculate.param> {
    public record param(String type, float value, String mods){}
    @Override
    public boolean isHandle(MessageEvent event, String messageText, DataValue<param> data) throws Throwable {
        String message = event.getRawMessage();
        if (!message.startsWith("#cal")) return false;
        var matcher = Instructions.MAP4DCALCULATE.matcher(message);
        if (matcher.find()) {
            var d = new param(
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
    public void HandleMessage(MessageEvent event, param param) throws Throwable {
        int mod;
        if (param.mods() == null) {
            mod = 0;
        } else {
            mod = Mod.getModsValue(param.mods());
        }
        // 只针对 std 模式
        String message = switch (param.type()) {
            case "ar" -> {
                float newAr = DataUtil.AR(param.value(), mod);
                float ms = DataUtil.AR2MS(newAr);
                yield String.format("AR: %.2f, 缩圈时间: %.2fms", newAr, ms);
            }
            case "od" -> {
                float newOd = DataUtil.AR(param.value(), mod);
                float ms = DataUtil.OD2MS(newOd);
                yield String.format("OD: %.2f, 300判定区间: %.2fms", newOd, ms);
            }
            case "cs" -> {
                float newCs = DataUtil.CS(param.value(), mod);
                yield String.format("CS: %.2f", newCs);
            }
            case "hp" -> {
                float newHp = DataUtil.HP(param.value(), mod);
                yield String.format("HP: %.2f", newHp);
            }
            default -> "Unexpected value: " + param.type();
        };

        event.getSubject().sendMessage(message);
    }
}
