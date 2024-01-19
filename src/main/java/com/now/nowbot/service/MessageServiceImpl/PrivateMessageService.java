package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.aop.CheckPermission;
import com.now.nowbot.dao.BindDao;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuApiService.OsuUserApiService;
import com.now.nowbot.throwable.TipsException;
import com.now.nowbot.util.JacksonUtil;
import com.now.nowbot.util.QQMsgUtil;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.regex.Pattern;

@Service("PM_SERVICE")
public class PrivateMessageService implements MessageService<PrivateMessageService.Param> {
    @Resource
    OsuUserApiService userApiService;
    @Resource
    ImageService      imageService;
    @Resource
    BindDao           bindDao;
    private static final Pattern pattern = Pattern
            .compile("^!testmsg (?<type>send|get|act)\\s*(?<id>\\d+)?\\s*(?<msg>.*)?$");

    @Override
    public boolean isHandle(MessageEvent event, String messageText, DataValue<Param> data) throws Throwable {
        if (!messageText.startsWith("!testmsg")) return false;
        var m = pattern.matcher(messageText);
        if (!m.matches()) return false;
        var type = Type.valueOf(m.group("type"));
        String s;
        Long id;
        if (Objects.isNull(s = m.group("id"))) {
            id = null;
        } else {
            id = Long.parseLong(s);
        }
        s = m.group("msg");

        data.setValue(new Param(type, id, s));
        return true;
    }

    @Override
    @CheckPermission(isSuperAdmin = true)
    public void HandleMessage(MessageEvent event, Param param) throws Throwable {
        var from = event.getSubject();
        var bin = bindDao.getUserFromQQ(event.getSender().getId());
        final boolean hasParam = Objects.isNull(param.id) || Objects.isNull(param.message);
        var json = switch (param.type) {
            case send -> {
                if (hasParam) throw new TipsException("参数缺失");
                yield userApiService.sendPrivateMessage(bin, param.id, param.message);
            }
            case get -> {
                if (hasParam) throw new TipsException("参数缺失");
                yield userApiService.getPrivateMessage(bin, param.id, Long.parseLong(param.message));

            }
            case act -> {
                if (Objects.isNull(param.id)) {
                    yield userApiService.acknowledgmentPrivateMessageAlive(bin);
                } else {
                    yield userApiService.acknowledgmentPrivateMessageAlive(bin, param.id);
                }
            }
        };
        QQMsgUtil.sendImage(from, getCodeImage(JacksonUtil.objectToJsonPretty(json)));
    }

    enum Type {
        send, get, act
    }

    record Param(Type type, Long id, String message) {
    }

    private byte[] getCodeImage(String code) {
        var codeStr = STR. """
                ```
                \{ code }
                ```
                """ ;
        return imageService.getPanelA6(codeStr, "NO NAME");
    }
}
