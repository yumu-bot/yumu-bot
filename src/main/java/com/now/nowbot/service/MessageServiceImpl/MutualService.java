package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.dao.BindDao;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.qq.message.AtMessage;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuApiService.OsuUserApiService;
import com.now.nowbot.throwable.ServiceException.BindException;
import com.now.nowbot.util.Instructions;
import com.now.nowbot.util.QQMsgUtil;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service("MUTUAL")
public class MutualService implements MessageService<Matcher> {
    //private static final Logger log = LoggerFactory.getLogger(MutualFriendService.class);
    @Resource
    OsuUserApiService userApiService;

    @Resource
    BindDao bindDao;

    static private final Pattern NumberPattern = Pattern.compile("^\\d{5,12}$");

    MutualService(OsuUserApiService userApiService, BindDao bindDao) {
        this.userApiService = userApiService;
        this.bindDao = bindDao;
    }

    @Override
    public boolean isHandle(MessageEvent event, String messageText, DataValue<Matcher> data) {
        var m = Instructions.MUTUAL.matcher(messageText);
        if (m.find()) {
            data.setValue(m);
            return true;
        } else return false;
    }

    record MutualData(Long uid, Long qq, String name) {}
    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        List<MutualData> users;

        var ats = QQMsgUtil.getTypeAll(event.getMessage(), AtMessage.class);
        if (!CollectionUtils.isEmpty(ats)){
            users = ats.stream().map(this::at2Mutual).toList();

            event.getSubject().sendMessage(parseData(users)).recallIn(60 * 1000);
            return;
        }

        var name = matcher.group("names");
        if (StringUtils.hasText(name)){
            users = Arrays.stream(name.split(",")).map(this::name2Mutual).toList();
        } else {
            users = List.of(qq2Mutual(event.getSender().getId()));
        }

        //原来可以链式调用啊
        event.getSubject().sendMessage(parseData(users)).recallIn(60 * 1000);
    }

    private MutualData at2Mutual(AtMessage at) {
        return qq2Mutual(at.getTarget());
    }

    private MutualData qq2Mutual(long qq) {
        try {
            var u = bindDao.getUserFromQQ(qq);
            return new MutualData(u.getOsuID(), qq, u.getOsuName());
        } catch (BindException e) {
            return new MutualData(null, qq, STR."\{qq} : 未绑定或已经掉绑");
        }
    }

    private MutualData name2Mutual(String name) {
        try {
            Long id = userApiService.getOsuId(name);
            return new MutualData(id, null, name);
        } catch (Exception e) {
            return new MutualData(null, null, STR."\{name} : 找不到玩家或网络错误！");
        }
    }

    private String parseData(Collection<MutualData> users) {
        StringBuilder sb = new StringBuilder();
        users.forEach(u -> {
            if (Objects.isNull(u.uid)) {
                sb.append(u.name).append('\n');
                return;
            }
            var name4Url = u.name
                    .replaceAll("\\s", "%20")
                    .replaceAll("-", "%2D")
                    .replaceAll("\\[", "%5B")
                    .replaceAll("]", "%5D")
                    .replaceAll("_", "%5F");

            var m = NumberPattern.matcher(name4Url);
            if (m.find()) {
                //有数字，只能 uid
                sb.append(STR."\{Objects.nonNull(u.qq) ? (new AtMessage(u.qq).getCQ() + '\n') : ""} \{u.name} : : https://osu.ppy.sh/users/\{u.uid}\n");
            } else {
                sb.append(STR."\{Objects.nonNull(u.qq) ? (new AtMessage(u.qq).getCQ() + '\n') : ""} \{u.name} : https://osu.ppy.sh/users/\{name4Url}\n");
            }
        });
        return sb.toString();
    }
}
