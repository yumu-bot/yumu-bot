package com.now.nowbot.service.messageServiceImpl;

import com.fasterxml.jackson.databind.JsonNode;
import com.now.nowbot.aop.CheckPermission;
import com.now.nowbot.dao.BindDao;
import com.now.nowbot.model.BindUser;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.osuApiService.OsuUserApiService;
import com.now.nowbot.throwable.TipsException;
import com.now.nowbot.util.JacksonUtil;
import jakarta.annotation.Resource;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Objects;
import java.util.regex.Pattern;

@Service("PRIVATE_MESSAGE")
public class PrivateMessageService implements MessageService<PrivateMessageService.PMParam> {
    @Resource
    OsuUserApiService userApiService;
    @Resource
    ImageService      imageService;
    @Resource
    BindDao           bindDao;
    private static final Pattern pattern = Pattern
            .compile("^!testmsg (?<type>send|get|act)\\s*(?<id>\\d+)?\\s*(?<msg>.*)?$");

    @Override
    public boolean isHandle(@NotNull MessageEvent event, @NotNull String messageText, @NotNull DataValue<PMParam> data) throws Throwable {
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

        data.setValue(new PMParam(type, id, s));
        return true;
    }

    @Override
    @CheckPermission(isSuperAdmin = true)
    public void HandleMessage(MessageEvent event, PMParam param) throws Throwable {
        var bin = bindDao.getBindFromQQ(event.getSender().getId(), true);
        JsonNode json;
        try {
            json = getJson(param, bin);
        } catch (WebClientResponseException.Forbidden e) {
            throw new TipsException("权限不足");
        }
        event.reply(getCodeImage(JacksonUtil.objectToJsonPretty(json)));
    }

    enum Type {
        send, get, act
    }

    public record PMParam(Type type, Long id, String message) {
    }

    private JsonNode getJson(PMParam param, BindUser bin) throws TipsException {
        final boolean hasParam = Objects.isNull(param.id) || Objects.isNull(param.message);
        return switch (param.type) {
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
