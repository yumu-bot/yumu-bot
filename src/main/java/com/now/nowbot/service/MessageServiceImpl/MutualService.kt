package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.dao.BindDao;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.qq.message.AtMessage;
import com.now.nowbot.qq.message.MessageChain;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuApiService.OsuUserApiService;
import com.now.nowbot.throwable.ServiceException.BindException;
import com.now.nowbot.util.Instruction;
import com.now.nowbot.util.QQMsgUtil;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

@Service("MUTUAL")
public class MutualService implements MessageService<List<MutualService.MutualParam>> {
    private static final Logger log = LoggerFactory.getLogger(MutualService.class);

    @Resource
    OsuUserApiService userApiService;

    @Resource
    BindDao bindDao;

    public record MutualParam(Long uid, Long qq, String name) {}

    @Override
    public boolean isHandle(MessageEvent event, String messageText, DataValue<List<MutualParam>> data) throws Throwable {
        var m = Instruction.MUTUAL.matcher(messageText);
        if (!m.find()) return false;

        var name = m.group("names");
        var atList = QQMsgUtil.getTypeAll(event.getMessage(), AtMessage.class);

        List<MutualParam> users;

        if (! CollectionUtils.isEmpty(atList)){
            users = atList.stream().map(this::at2Mutual).toList();
        } else if (StringUtils.hasText(name)){
            users = Arrays.stream(name.split(",")).map(this::name2Mutual).toList();
        } else {
            users = List.of(qq2Mutual(event.getSender().getId()));
        }

        data.setValue(users);
        return true;
    }

    @Override
    public void HandleMessage(MessageEvent event, List<MutualParam> users) throws Throwable {
        var from = event.getSubject();

        try {
            from.sendMessage(mutual2MessageChain(users)).recallIn(60 * 1000);
        } catch (Exception e) {
            log.error("添加好友：发送失败！", e);
        }
    }

    private MutualParam at2Mutual(AtMessage at) {
        return qq2Mutual(at.getTarget());
    }

    private MutualParam qq2Mutual(long qq) {
        try {
            var u = bindDao.getUserFromQQ(qq);
            return new MutualParam(u.getOsuID(), qq, u.getOsuName());
        } catch (BindException e) {
            return new MutualParam(null, qq, STR."\{qq} : 未绑定或绑定状态失效！");
        }
    }

    private MutualParam name2Mutual(String name) {
        try {
            Long id = userApiService.getOsuId(name);
            return new MutualParam(id, null, name);
        } catch (Exception e) {
            return new MutualParam(null, null, STR."\{name} : 找不到玩家或网络错误！");
        }
    }

    private MessageChain mutual2MessageChain(Collection<MutualParam> users) {
        var sb = new MessageChain.MessageChainBuilder();
        users.forEach(u -> {
            if (Objects.isNull(u.uid)) {
                sb.addText('\n' + u.name);
                return;
            }

            if (Objects.nonNull(u.qq)) {
                sb.addAt(u.qq);
            }

            sb.addText(STR."\n\{u.name}：https://osu.ppy.sh/users/\{u.uid}");

            /*
            var name4Url = u.data
                    .replaceAll("\\s", "%20")
                    .replaceAll("-", "%2D")
                    .replaceAll("\\[", "%5B")
                    .replaceAll("]", "%5D")
                    .replaceAll("_", "%5F");

            var m = NumberPattern.matcher(name4Url);

            if (m.find()) {
                //有数字，只能 uid
                sb.addText(STR." \{u.data} : : https://osu.ppy.sh/users/\{u.uid}\n");
            } else {
                sb.addText(STR." \{u.data} : https://osu.ppy.sh/users/\{name4Url}\n");
            }

             */
        });
        return sb.build();
    }
}
