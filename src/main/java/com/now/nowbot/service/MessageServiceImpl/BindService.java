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
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.throwable.ServiceException.BindException;
import com.now.nowbot.util.ASyncMessageUtil;
import com.now.nowbot.util.QQMsgUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service("BIND")
public class BindService implements MessageService<Matcher> {

    public static final Map<Long, Bind> BIND_MSG_MAP = new ConcurrentHashMap<>();
    private static boolean CLEAR = false;

    OsuGetService osuGetService;

    BindDao bindDao;

    TaskExecutor taskExecutor;

    @Autowired
    public BindService(OsuGetService osuGetService, BindDao bindDao, TaskExecutor taskExecutor) {
        this.osuGetService = osuGetService;
        this.bindDao = bindDao;
        this.taskExecutor = taskExecutor;
    }

    Pattern pattern = Pattern.compile("^[!！]\\s*(?i)(ym)?(bi(?!nd)|((ym)|(?<un>(un)))bind)(\\s*(?<name>[0-9a-zA-Z\\[\\]\\-_ ]+))?");

    @Override
    public boolean isHandle(MessageEvent event, DataValue<Matcher> data) {
        var m = pattern.matcher(event.getRawMessage().trim());
        if (m.find()) {
            data.setValue(m);
            return true;
        } else return false;
    }

    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        var from = event.getSubject();
        if (Permission.isSuper(event.getSender().getId())) {
            var at = QQMsgUtil.getType(event.getMessage(), AtMessage.class);
            if (matcher.group("un") != null && at != null) {
                unbindQQ(at.getTarget());
                return;
            }
            if (at != null) {
                bindQQAt(event, at.getTarget());
                return;
            }
        }

        if (matcher.group("un") != null) {
            from.sendMessage("解绑请联系管理员");
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
        BinUser user = null;
        try {
            user = bindDao.getUserFromQQ(event.getSender().getId());
        } catch (BindException ignore) {
            // do nothing
        }

        var qqBindOpt = bindDao.getQQLiteFromQQ(event.getSender().getId());
        QQBindLite qqBindLite;
        if (qqBindOpt.isPresent() && (qqBindLite = qqBindOpt.get()).getBinUser().isAuthorized()) {
            user = qqBindLite.getBinUser();
            try {
                var getUDate = osuGetService.getPlayerInfo(user, OsuMode.DEFAULT);
                if (!getUDate.getUID().equals(user.getOsuID())) throw new RuntimeException();
                from.sendMessage("您已绑定 (" + user.getOsuID() + ") " + user.getOsuName() + "。\n但您仍可以重新绑定。" +
                        "\n若无必要请勿绑定, 多次绑定仍无法使用则大概率为bug, 请联系开发者" +
                        "\n回复 OK 重新绑定。");
                var lock = ASyncMessageUtil.getLock(event);
                var s = lock.get();
                if (!(s != null && s.getRawMessage().trim().equalsIgnoreCase("OK"))) {
                    return;
                }
            } catch (Exception e) {
                if (e instanceof HttpClientErrorException.Unauthorized unauthorized) {
                    // 已失效, 直接允许绑定
                } else {
                    throw e;
                }
            }
        }

        // 需要绑定
        String state = event.getSender().getId() + "+" + timeMillis;
        //将消息回执作为 value
        state = osuGetService.getOauthUrl(state);
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
        var user = bindDao.getQQLiteFromQQ(qqId);
        if (user.isEmpty()) {
            throw new BindException(BindException.Type.BIND_Player_NoBind);
        }

        if (bindDao.unBindQQ(user.get().getBinUser())) {
            throw new BindException(BindException.Type.BIND_Client_RelieveBindSuccess);
        } else {
            throw new BindException(BindException.Type.BIND_Client_RelieveBindFailed);
        }
    }

    private void bindQQAt(MessageEvent event, long qq) {
        // 只有管理才有权力@人绑定,提示就不改了
        var from = event.getSubject();
        from.sendMessage("你叫啥名呀？告诉我吧");
        var lock = ASyncMessageUtil.getLock(event);
        var s = lock.get();//阻塞,注意超时判空
        if (s == null) {
            throw new BindException(BindException.Type.BIND_Client_BindingOvertime);
        }
        String nameStr = s.getRawMessage();
        Long id;
        try {
            id = osuGetService.getOsuId(nameStr);
        } catch (Exception e) {
            throw new BindException(BindException.Type.BIND_Player_NotFound);
        }
        var buser = bindDao.getQQLiteFromOsuId(id);
        if (buser.isEmpty()) {
            from.sendMessage("正在将" + qq + "绑定到 (" + id + ")" + nameStr + "上");
            bindDao.bindQQ(qq, new BinUser(id, nameStr));
            throw new BindException(BindException.Type.BIND_Player_Success);
        }
        var u = buser.get();
        from.sendMessage(u.getOsuUser().getOsuName() + "绑定在 QQ " + qq + " 上，是否覆盖？回复 OK 生效");
        s = lock.get();
        if (s != null && s.getRawMessage().startsWith("OK")) {
            bindDao.bindQQ(qq, u.getOsuUser());
            throw new BindException(BindException.Type.BIND_Player_Success);
        } else {
            throw new BindException(BindException.Type.BIND_Client_BindingRefused);
        }
    }

    private void bindQQName(MessageEvent event, String name, long qq) {
        Contact from = event.getSubject();
        var nuserOpt = bindDao.getQQLiteFromQQ(qq);
        if (nuserOpt.isPresent()) throw new BindException(BindException.Type.BIND_Client_AlreadyBound);

        long osuUserId;
        try {
            osuUserId = osuGetService.getOsuId(name);
        } catch (Exception e) {
            throw new BindException(BindException.Type.BIND_Player_NotFound);
        }

        var buser = bindDao.getQQLiteFromOsuId(osuUserId);
        if (buser.isPresent()) {
            from.sendMessage(name + " 已绑定 (" + buser.get().getQq() + ")，若绑定错误，请尝试重新绑定！(命令不要带上任何参数)\n(!ymbind / !ymbi / !bi)");
            return;
        }

        from.sendMessage("""
                直接绑定osu用户名的方式即将删除, 请使用直接发送 '!ymbind' 不带任何参数的形式绑定
                如果执意使用绑定用户名的方式, 请回答下面问题:
                设随机变量 X 与 Y 相互独立且都服从 U(0,1), 则 P(X+Y<1) 为
                """);
        var lock = ASyncMessageUtil.getLock(event, 30000);
        event = lock.get();
        if (event == null) {
            from.sendMessage("回答超时");
            return;
        }
        if (!event.getRawMessage().contains("0.5") && !event.getRawMessage().contains("1/2")) {
            from.sendMessage("回答错误");
            return;
        }
        bindDao.bindQQ(qq, new BinUser(osuUserId, name));
        from.sendMessage("已将 " + qq + " 绑定到 (" + osuUserId + ") " + name + " 上");
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