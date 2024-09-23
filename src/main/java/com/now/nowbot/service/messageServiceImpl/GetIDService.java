package com.now.nowbot.service.messageServiceImpl;

import com.now.nowbot.config.Permission;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.osuApiService.OsuUserApiService;
import com.now.nowbot.throwable.GeneralTipsException;
import com.now.nowbot.util.DataUtil;
import com.now.nowbot.util.Instruction;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.regex.Matcher;

@Service("GET_ID")
public class GetIDService implements MessageService<Matcher> {
    @Resource
    OsuUserApiService userApiService;

    @Override
    public boolean isHandle(MessageEvent event, String messageText, DataValue<Matcher> data) throws Throwable {
        var m = Instruction.GET_ID.matcher(messageText);
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

        var names = DataUtil.splitString(matcher.group("data"));

        if (CollectionUtils.isEmpty(names)) throw new GeneralTipsException(GeneralTipsException.Type.G_Fetch_List);

        StringBuilder sb = new StringBuilder();

        for (var name : names) {
            if (! StringUtils.hasText(name)) {
                break;
            }

            long id;

            try {
                id = userApiService.getOsuId(name);
            } catch (Exception e) {
                sb.append("name=").append(name).append(" not found").append(',').append(' ');
                break;
            }

            sb.append(id).append(',').append(' ');
        }


        from.sendMessage(sb.substring(0, sb.length() - 2));
    }
}
