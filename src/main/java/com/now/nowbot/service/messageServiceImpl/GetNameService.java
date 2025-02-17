package com.now.nowbot.service.messageServiceImpl;

import com.now.nowbot.config.Permission;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.osuApiService.OsuScoreApiService;
import com.now.nowbot.service.osuApiService.OsuUserApiService;
import com.now.nowbot.throwable.GeneralTipsException;
import com.now.nowbot.util.DataUtil;
import com.now.nowbot.util.Instruction;
import jakarta.annotation.Resource;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.regex.Matcher;

@Service("GET_NAME")
public class GetNameService implements MessageService<Matcher> {
    @Resource
    OsuUserApiService userApiService;
    @Resource
    OsuScoreApiService scoreApiService;

    @Override
    public boolean isHandle(@NotNull MessageEvent event, @NotNull String messageText, @NotNull DataValue<Matcher> data) throws Throwable {
        var m = Instruction.GET_NAME.matcher(messageText);
        if (m.find()) {
            data.setValue(m);
            return true;
        } else return false;
    }

    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        var from = event.getSubject();

        if (Permission.isCommonUser(event)) {
            throw new GeneralTipsException(GeneralTipsException.Type.G_Permission_Group);
        }

        var ids = DataUtil.splitString(matcher.group("data"));

        if (CollectionUtils.isEmpty(ids)) throw new GeneralTipsException(GeneralTipsException.Type.G_Fetch_List);

        StringBuilder sb = new StringBuilder();

        for (var i : ids) {
            if (! StringUtils.hasText(i)) {
                break;
            }

            long id;
            String name;

            try {
                id = Long.parseLong(i);
            } catch (NumberFormatException e) {
                sb.append("id=").append(i).append(" can't parse").append(',').append(' ');
                break;
            }

            try {
                name = userApiService.getPlayerOsuInfo(id).getUsername();
            } catch (Exception e) {
                sb.append("id=").append(id).append(" not found").append(',').append(' ');
                break;
            }

            sb.append(name).append(',').append(' ');
        }


        from.sendMessage(sb.substring(0, sb.length() - 2));
    }
}
