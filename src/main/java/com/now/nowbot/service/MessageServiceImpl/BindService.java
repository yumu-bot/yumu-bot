package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.config.Permission;
import com.now.nowbot.dao.BindDao;
import com.now.nowbot.entity.bind.QQBindLite;
import com.now.nowbot.model.BinUser;
import com.now.nowbot.model.enums.OsuMode;
import com.now.nowbot.qq.contact.Contact;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.qq.message.AtMessage;
import com.now.nowbot.qq.message.MessageChain;
import com.now.nowbot.qq.message.MessageReceipt;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuApiService.OsuUserApiService;
import com.now.nowbot.throwable.ServiceException.BindException;
import com.now.nowbot.util.ASyncMessageUtil;
import com.now.nowbot.util.Instructions;
import com.now.nowbot.util.QQMsgUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;

@Service("BIND")
public class BindService implements MessageService<Matcher> {

    public static final Map<Long, Bind> BIND_MSG_MAP = new ConcurrentHashMap<>();
    private static boolean CLEAR = false;
    OsuUserApiService userApiService;

    BindDao bindDao;

    TaskExecutor taskExecutor;

    @Autowired
    public BindService(OsuUserApiService userApiService, BindDao bindDao, TaskExecutor taskExecutor) {
        this.userApiService = userApiService;
        this.bindDao = bindDao;
        this.taskExecutor = taskExecutor;
    }

    @Override
    public boolean isHandle(MessageEvent event, String messageText, DataValue<Matcher> data) {
        var m = Instructions.BIND.matcher(messageText);
        if (m.find()) {
            data.setValue(m);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        var from = event.getSubject();
        boolean isSuper = Permission.isSuper(event.getSender().getId());

        //超级管理员的专属权利
        if (isSuper) {
            var at = QQMsgUtil.getType(event.getMessage(), AtMessage.class);
            var qqStr = matcher.group("qq");

            if (matcher.group("un") != null){
                if (at != null) {
                    unbindQQ(at.getTarget());
                    return;
                } else if (qqStr != null && Long.parseLong(qqStr) > 0L) {
                    unbindQQ(Long.parseLong(qqStr));
                    return;
                } else {
                    unbindQQ(event.getSender().getId());
                    return;
                }
            } else if (at != null) {
                bindQQAt(event, at.getTarget());
                return;
            }
        }

        //绑定权限下放。这个不应该是超级管理员的专属权利。
        if (matcher.group("un") != null) {
            unbindQQ(event.getSender().getId());
            return;
        }

        var name = matcher.group("name");
        if (name != null) {
            bindQQName(event, name, event.getSender().getId());
            return;
        }
        //将当前毫秒时间戳作为 key
        long timeMillis = System.currentTimeMillis();

        //验证是否已绑定
        BinUser binUser;
        /*
        try {
            binUser = bindDao.getUserFromQQ(event.getSender().getId());
        } catch (BindException ignore) {
            // do nothing
        }
        */

        var qqLiteFromQQ = bindDao.getQQLiteFromQQ(event.getSender().getId());
        QQBindLite qqBindLite;
        if (qqLiteFromQQ.isPresent() && (qqBindLite = qqLiteFromQQ.get()).getBinUser().isAuthorized()) {
            binUser = qqBindLite.getBinUser();
            try {
                var osuUser = userApiService.getPlayerInfo(binUser, OsuMode.DEFAULT);
                if (!osuUser.getUID().equals(binUser.getOsuID())) {
                    throw new RuntimeException();
                }
                from.sendMessage(STR."您已绑定 (\{binUser.getOsuID()}) \{binUser.getOsuName()}。\n但您仍可以重新绑定。\n若无必要请勿绑定, 多次绑定仍无法使用则大概率为 BUG, 请联系开发者。\n回复 OK 重新绑定。");
                var lock = ASyncMessageUtil.getLock(event);
                var s = lock.get();
                if (!(s != null && s.getRawMessage().trim().equalsIgnoreCase("OK"))) {
                    return;
                }
            } catch (Exception e) {
                if (!(e instanceof WebClientResponseException.Unauthorized)) {
                    throw e;
                }
                //如果符合，直接允许绑定
            }
        }

        // 需要绑定
        String state = event.getSender().getId() + "+" + timeMillis;
        //将消息回执作为 value
        state = userApiService.getOauthUrl(state, Objects.nonNull(matcher.group("full")));
        var send = new MessageChain.MessageChainBuilder()
                .addAt(event.getSender().getId())
                .addText("\n")
                .addText(state)
                .build();
        var receipt = from.sendMessage(send);
        //默认110秒后撤回
        from.recallIn(receipt, 110 * 1000);
        //此处在 controller.msgController 处理
        putBind(timeMillis, new Bind(timeMillis, receipt, event.getSender().getId()));

    }

    private void unbindQQ(Long qqId) throws BindException {
        if (qqId == null) throw new BindException(BindException.Type.BIND_Player_NoQQ);
        var bind = bindDao.getQQLiteFromQQ(qqId);
        if (bind.isEmpty()) {
            throw new BindException(BindException.Type.BIND_Player_NoBind);
        }

        if (bindDao.unBindQQ(bind.get().getBinUser())) {
            throw new BindException(BindException.Type.BIND_Client_UnBindSuccess);
        } else {
            throw new BindException(BindException.Type.BIND_Client_UnBindFailed);
        }
    }

    private void bindQQAt(MessageEvent event, long qq) {
        // 只有管理才有权力@人绑定,提示就不改了
        var from = event.getSubject();
        from.sendMessage("你叫啥名呀？告诉我吧");
        var lock = ASyncMessageUtil.getLock(event);
        var s = lock.get();//阻塞,注意超时判空
        if (s == null) {
            throw new BindException(BindException.Type.BIND_Client_Overtime);
        }
        String nameStr = s.getRawMessage();
        Long id;
        try {
            id = userApiService.getOsuId(nameStr);
        } catch (Exception e) {
            throw new BindException(BindException.Type.BIND_Player_NotFound);
        }
        var bind = bindDao.getQQLiteFromOsuId(id);
        if (bind.isEmpty()) {
            from.sendMessage(STR."正在将\{qq}绑定到 (\{id})\{nameStr}上");
            bindDao.bindQQ(qq, new BinUser(id, nameStr));
            throw new BindException(BindException.Type.BIND_Player_Success);
        }
        var u = bind.get();
        from.sendMessage(STR."\{u.getOsuUser().getOsuName()}绑定在 QQ \{qq} 上，是否覆盖？回复 OK 生效");
        s = lock.get();
        if (s != null && s.getRawMessage().startsWith("OK")) {
            bindDao.bindQQ(qq, u.getOsuUser());
            throw new BindException(BindException.Type.BIND_Player_Success);
        } else {
            throw new BindException(BindException.Type.BIND_Client_Refused);
        }
    }

    private void bindQQName(MessageEvent event, String name, long qq) {
        Contact from = event.getSubject();
        var userFromQQ = bindDao.getQQLiteFromQQ(qq);
        if (userFromQQ.isPresent()) throw new BindException(BindException.Type.BIND_Client_AlreadyBound);

        long osuUserId;
        try {
            osuUserId = userApiService.getOsuId(name);
        } catch (Exception e) {
            throw new BindException(BindException.Type.BIND_Player_NotFound);
        }

        var userFromID = bindDao.getQQLiteFromOsuId(osuUserId);
        if (userFromID.isPresent()) {
            from.sendMessage(STR."\{name} 已绑定 (\{userFromID.get().getQq()})，若绑定错误，请尝试重新绑定！(命令不要带上任何参数)\n(!ymbind / !ymbi / !bi)");
            return;
        }

        from.sendMessage("""
                将弃用直接绑定用户名的方法, 请直接发送 '!ymbind' 绑定，并且不带任何参数。
                如果您执意使用绑定用户名的方式, 请回答下面问题:
                设随机变量 X 与 Y 相互独立且都服从 U(0,1), 则 P(X+Y<1) 为
                """);
        var lock = ASyncMessageUtil.getLock(event, 30000);
        event = lock.get();

        if (event == null) {
            from.sendMessage("回答超时，撤回绑定请求。");
            return;
        }

        if (!event.getRawMessage().contains("0.5") && !event.getRawMessage().contains("1/2") && !event.getRawMessage().contains("50%")) {
            from.sendMessage("回答错误，请重试。");
            return;
        }

        bindDao.bindQQ(qq, new BinUser(osuUserId, name));
        from.sendMessage(STR."已将 \{qq} 绑定到 (\{osuUserId}) \{name} 上");
    }

    public record Bind(Long key, MessageReceipt receipt, Long QQ) {
    }

    private void putBind(Long t, Bind b) {
        removeOldBind();
        if (BIND_MSG_MAP.size() > 20 && !CLEAR) {
            CLEAR = true;
            taskExecutor.execute(() -> {
                try {
                    Thread.sleep(1000 * 5);
                } catch (InterruptedException e) {
                    // ignore
                }
                removeOldBind();
                CLEAR = false;
            });
        }
        BIND_MSG_MAP.put(t, b);
    }

    public static boolean contains(Long t) {
        return BIND_MSG_MAP.containsKey(t);
    }

    public static Bind getBind(Long t) {
        removeOldBind();
        return BIND_MSG_MAP.get(t);
    }

    public static void removeBind(Long t) {
        BIND_MSG_MAP.remove(t);
    }

    private static void removeOldBind() {
        BIND_MSG_MAP.keySet().removeIf(k -> (k + 120 * 1000) < System.currentTimeMillis());
    }

}