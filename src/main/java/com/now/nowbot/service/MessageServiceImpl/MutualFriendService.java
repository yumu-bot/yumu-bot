package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.dao.BindDao;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.qq.message.AtMessage;
import com.now.nowbot.qq.message.MessageChain;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuApiService.OsuUserApiService;
import com.now.nowbot.throwable.ServiceException.BindException;
import com.now.nowbot.throwable.TipsException;
import com.now.nowbot.util.Instructions;
import com.now.nowbot.util.QQMsgUtil;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service("MUTUAL")
public class MutualFriendService implements MessageService<Matcher> {
    private static final Logger log = LoggerFactory.getLogger(MutualFriendService.class);
    @Resource
    OsuUserApiService userApiService;

    @Resource
    BindDao bindDao;

    static private final Pattern userNameTest = Pattern.compile("^\\d{5,12}$");

    MutualFriendService(OsuUserApiService userApiService, BindDao bindDao) {
        this.userApiService = userApiService;
        this.bindDao = bindDao;
    }

    @Override
    public boolean isHandle(MessageEvent event, DataValue<Matcher> data) {
        var m = Instructions.MUTUAL.matcher(event.getRawMessage().trim());
        if (m.find()) {
            data.setValue(m);
            return true;
        } else return false;
    }

    record OsuMuUser(Long uid, Long qq, String name) {}
    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        List<OsuMuUser> users;


        var ats = QQMsgUtil.getTypeAll(event.getMessage(), AtMessage.class);
        if (!CollectionUtils.isEmpty(ats)){
            users = ats.stream().map(this::atToMu).toList();
            event.getSubject().sendMessage(getMessage(users));
            return;
        }

        var name = matcher.group("names");
        if (StringUtils.hasText(name)){
            users = Arrays.stream(name.split(",")).map(this::nameToMu).toList();
        } else {
            users = List.of(qqToMu(event.getSender().getId()));
        }

        event.getSubject().sendMessage(getMessage(users));
    }

    private OsuMuUser atToMu(AtMessage at) {
        return qqToMu(at.getTarget());
    }

    private OsuMuUser qqToMu(long qq) {
        try {
            var u = bindDao.getUserFromQQ(qq);
            return new OsuMuUser(u.getOsuID(), qq, u.getOsuName());
        } catch (BindException e) {
            return new OsuMuUser(null, qq, STR."# \{qq} : 未绑定或已经掉绑");
        }
    }

    private OsuMuUser nameToMu(String name) {
        try {
            Long id = userApiService.getOsuId(name);
            return new OsuMuUser(id, null, name);
        } catch (Exception e) {
            return new OsuMuUser(null, null, STR."# \{name} : 找不到玩家或网络错误！");
        }
    }

    private String getMessage(Collection<OsuMuUser> users) {
        StringBuilder sb = new StringBuilder();
        users.forEach(u -> {
            if (Objects.isNull(u.uid)) {
                sb.append(u.name).append('\n');
                return;
            }
            var name = u.name.replaceAll(" ", "%20");
            var m = userNameTest.matcher(name);
            if (m.find()) {
                sb.append(STR."# \{name} : https://osu.ppy.sh/users/\{u.uid}\n");
                return;
            }

            sb.append(STR."# \{Objects.nonNull(u.qq) ? new AtMessage(u.qq).getCQ() : ""} \{u.name} : https://osu.ppy.sh/users/\{name}\n");
        });
        return sb.toString();
    }
}
