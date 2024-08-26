package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.dao.BindDao;
import com.now.nowbot.model.BinUser;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.qq.message.MessageChain;
import com.now.nowbot.qq.tencent.TencentMessageService;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.util.Instruction;
import com.now.nowbot.util.OfficialInstruction;
import jakarta.annotation.Resource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;

import static com.now.nowbot.util.command.CommandPatternStaticKt.FLAG_MODE;

@Service("SET_MODE")
public class SetModeService implements MessageService<String>, TencentMessageService<String> {
    @Resource
    BindDao bindDao;

    @Override
    public boolean isHandle(MessageEvent event, String messageText, DataValue<String> data) {
        var m = Instruction.SET_MODE.matcher(messageText);
        if (m.find()) {
            data.setValue(m.group(FLAG_MODE));
            return true;
        } else return false;
    }

    @Override
    public void HandleMessage(MessageEvent event, String modeStr) throws Throwable {
        var user = bindDao.getUserFromQQ(event.getSender().getId());
        var from = event.getSubject();
        var message = getReply(modeStr, user);
        from.sendMessage(message);
    }

    @Override
    public @Nullable String Accept(@NotNull MessageEvent event, @NotNull String messageText) {
        var m = OfficialInstruction.SET_MODE.matcher(messageText);
        if (m.find()) return m.group(FLAG_MODE);
        return null;
    }

    @Override
    public @Nullable MessageChain Reply(@NotNull MessageEvent event, String data) throws Throwable {
        var user = bindDao.getUserFromQQ(event.getSender().getId());
        if (user.isAuthorized()) return getReply(data, user);
        return new MessageChain("需要先绑定 yumu 才能使用哦");
    }

    MessageChain getReply(String modeStr, BinUser user) throws Throwable {
        var mode = OsuMode.getMode(modeStr);
        if (mode == OsuMode.DEFAULT) {
            return new MessageChain("未知的格式,修改请使用0(osu),1(taiko),2(catch),3(mania)");
        }
        user.setOsuMode(mode);
        bindDao.updateMod(user.getOsuID(), mode);
        return new MessageChain("已修改主模式为: "+mode.getName());
    }
}
