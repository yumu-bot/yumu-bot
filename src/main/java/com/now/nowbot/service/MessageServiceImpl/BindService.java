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
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskExecutor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Service("BIND")
public class BindService implements MessageService<BindService.BindParam> {

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

    // full: 全绑定，只有 bot 开发可以这样做
    public record BindParam(@NonNull Long qq, String name, boolean at, boolean unbind, boolean isSuper, boolean isFull) {}

    @Override
    public boolean isHandle(MessageEvent event, String messageText, DataValue<BindParam> data) throws Throwable {
        var m = Instructions.BIND.matcher(messageText);
        if (!m.find()) return false;

        //!bind 给个提示
        if (Objects.isNull(m.group("ym")) && Objects.nonNull(m.group("bind"))) {
            var from = event.getSubject();
            var receipt = from.sendMessage(BindException.Type.BIND_Question_BindRetreat.message);

            var lock = ASyncMessageUtil.getLock(event, 30 * 1000);
            event = lock.get();

            if (Objects.isNull(event) || ! event.getRawMessage().toUpperCase().contains("OK")) {
                from.recall(receipt);
                return false;
            } else {
                from.recall(receipt);
            }
        }

        var meQQ = event.getSender().getId();

        var qq = m.group("qq");
        var name = m.group("name");
        var at = QQMsgUtil.getType(event.getMessage(), AtMessage.class);
        boolean unbind = Objects.nonNull(m.group("un")) || Objects.nonNull(m.group("ub"));
        boolean isSuper = Permission.isSuper(meQQ);
        boolean isFull = Objects.nonNull(m.group("full"));

        if (Objects.nonNull(at)) {
            data.setValue(new BindParam(at.getTarget(), null, true, unbind, isSuper, isFull));
            return true;
        }

        if (Objects.nonNull(name) && Strings.isNotBlank(name)) {
            if (Objects.nonNull(qq)) {
                data.setValue(new BindParam(Long.parseLong(qq), name, false, unbind, isSuper, isFull));
                return true;
            } else {
                data.setValue(new BindParam(meQQ, name, false, unbind, isSuper, isFull));
                return true;
            }
        }

        if (Objects.nonNull(qq) && Strings.isNotBlank(qq) && ! qq.trim().equals("0")) {
            data.setValue(new BindParam(Long.parseLong(qq), null, false, unbind, isSuper, isFull));
            return true;
        }

        data.setValue(new BindParam(meQQ, null, false, unbind, isSuper, isFull));
        return true;
    }

    @Override
    public void HandleMessage(MessageEvent event, BindParam param) throws Throwable {
        var me = event.getSender().getId();

        //解绑自己的权限下放。这个不应该是超级管理员的专属权利。
        if (param.unbind && me == param.qq) {
            unbindQQ(me);
            return;
        }

        if (Objects.nonNull(param.name) && me == param.qq) {
            bindQQName(event, param.name, me);
            return;
        }

        //超级管理员的专属权利：艾特绑定和全 QQ 移除绑定
        if (param.isSuper) {
            if (Objects.nonNull(param.name) && me != param.qq) {
                bindQQName(event, param.name, param.qq);
            }
            if (param.at) {
                bindQQAt(event, param.qq);
                return;
            }
            if (param.unbind) { //超管也可以解绑自己，就不写 me != param.qq 了
                unbindQQ(param.qq);
                return;
            }
        }

        bindQQ(event, me, param.isFull);
    }

    //默认绑定路径
    private void bindQQ(MessageEvent event, long qq, boolean isFull) throws BindException {
        var from = event.getSubject();
        BinUser binUser;

        //检查是否已经绑定
        var qqLiteFromQQ = bindDao.getQQLiteFromQQ(qq);
        QQBindLite qqBindLite;
        if (qqLiteFromQQ.isPresent() && (qqBindLite = qqLiteFromQQ.get()).getBinUser().isAuthorized()) {
            binUser = qqBindLite.getBinUser();
            try {
                var osuUser = userApiService.getPlayerInfo(binUser, OsuMode.DEFAULT);
                if (!osuUser.getUID().equals(binUser.getOsuID())) {
                    throw new RuntimeException();
                }

                from.sendMessage(
                        String.format(BindException.Type.BIND_Progress_BindingRecoverInfo.message, binUser.getOsuID(), binUser.getOsuName())
                );

                var lock = ASyncMessageUtil.getLock(event);
                var s = lock.get();
                if (Objects.isNull(s) || !s.getRawMessage().toUpperCase().contains("OK")) {
                    return;
                }

            } catch (HttpClientErrorException.Unauthorized | WebClientResponseException.Unauthorized | BindException e) {
                throw e;
            } catch (Exception ignored) {
                // 如果符合，直接允许绑定
            }
        }

        // 需要绑定
        // 将当前毫秒时间戳作为 key
        long timeMillis = System.currentTimeMillis();
        String state = STR."\{qq}+\{timeMillis}";

        // 将消息回执作为 value
        state = userApiService.getOauthUrl(state, isFull);
        var send = new MessageChain.MessageChainBuilder()
                .addAt(qq)
                .addText("\n")
                .addText(state)
                .build();

        MessageReceipt receipt;
        if (Objects.nonNull(from)) {
            receipt = from.sendMessage(send);

            from.recallIn(receipt, 110 * 1000);
            //此处在 controller.msgController 处理
            putBind(timeMillis, new Bind(timeMillis, receipt, qq));
        }
    }

    private void unbindQQ(Long qq) throws BindException {
        if (Objects.isNull(qq)) throw new BindException(BindException.Type.BIND_Player_NoQQ);
        var bind = bindDao.getQQLiteFromQQ(qq);
        if (bind.isEmpty()) {
            throw new BindException(BindException.Type.BIND_Player_NoBind);
        }

        if (bindDao.unBindQQ(bind.get().getBinUser())) {
            throw new BindException(BindException.Type.BIND_UnBind_Successes, qq);
        } else {
            throw new BindException(BindException.Type.BIND_UnBind_Failed);
        }
    }

    private void bindQQAt(MessageEvent event, long qq) {
        // 只有管理才有权力@人绑定,提示就不改了
        var from = event.getSubject();
        from.sendMessage(BindException.Type.BIND_Receive_NoName.message);

        var lock = ASyncMessageUtil.getLock(event);
        var s = lock.get();

        if (Objects.isNull(s)) {
            throw new BindException(BindException.Type.BIND_Receive_Overtime);
        }

        String name = s.getRawMessage();
        Long UID;

        try {
            UID = userApiService.getOsuId(name);
        } catch (HttpClientErrorException.Forbidden | WebClientResponseException.Forbidden e) {
            throw new BindException(BindException.Type.BIND_Player_Banned);
        } catch (Exception e) {
            throw new BindException(BindException.Type.BIND_Player_NotFound);
        }
        var bind = bindDao.getQQLiteFromOsuId(UID);

        if (bind.isEmpty()) {
            from.sendMessage(
                    String.format(BindException.Type.BIND_Progress_Binding.message, qq, UID, name)
            );
            bindDao.bindQQ(qq, new BinUser(UID, name));
            throw new BindException(BindException.Type.BIND_Response_Success);
        }

        var u = bind.get();

        from.sendMessage(
                String.format(BindException.Type.BIND_Progress_BindingRecover.message, u.getOsuUser().getOsuName(), qq)
        );
        //from.sendMessage(STR."\{u.getOsuUser().getOsuName()}绑定在 QQ \{qq} 上，是否覆盖？回复 OK 生效");

        s = lock.get();
        if (Objects.nonNull(s) && s.getRawMessage().toUpperCase().startsWith("OK")) {
            bindDao.bindQQ(qq, u.getOsuUser());
            from.sendMessage(BindException.Type.BIND_Response_Success.message);
        } else {
            from.sendMessage(BindException.Type.BIND_Receive_Refused.message);
        }
    }

    private void bindQQName(MessageEvent event, String name, long qq) {
        Contact from = event.getSubject();
        var u = bindDao.getQQLiteFromQQ(qq);
        if (u.isPresent()) throw new BindException(BindException.Type.BIND_Response_AlreadyBound);

        long UID;
        try {
            UID = userApiService.getOsuId(name);
        } catch (HttpClientErrorException.Forbidden | WebClientResponseException.Forbidden e) {
            throw new BindException(BindException.Type.BIND_Player_Banned);
        } catch (Exception e) {
            throw new BindException(BindException.Type.BIND_Player_NotFound);
        }

        var userFromID = bindDao.getQQLiteFromOsuId(UID);
        if (userFromID.isPresent()) {
            from.sendMessage(
                    String.format(BindException.Type.BIND_Response_AlreadyBoundInfo.message, userFromID.get().getQq(), name)
            );
            //from.sendMessage(STR."\{name} 已绑定 (\{userFromID.get().getQq()})，若绑定错误，请尝试重新绑定！(命令不要带上任何参数)\n(!ymbind)");
            return;
        }

        from.sendMessage(BindException.Type.BIND_Question_BindByName.message);

        /*
        from.sendMessage("""
                将弃用直接绑定用户名的方法, 请直接发送 '!ymbind' 绑定，并且不带任何参数。
                如果您执意使用绑定用户名的方式, 请回答下面问题:
                设随机变量 X 与 Y 相互独立且都服从 U(0,1), 则 P(X+Y<1) 为
                """);

        from.sendMessage("""
                别看了，乖乖发送 !ymbind 绑定吧，不要带任何参数。
                现在没法直接单独绑定用户名，请点击链接绑定。
                记得不要挂科学上网。
                """);

         */
        var lock = ASyncMessageUtil.getLock(event, 30000);
        event = lock.get();

        if (Objects.isNull(event)) {
            //from.sendMessage("回答超时，撤回绑定请求。");
            throw new BindException(BindException.Type.BIND_Question_Overtime);
        }

        if (!event.getRawMessage().contains("0.5") && !event.getRawMessage().contains("1/2") && !event.getRawMessage().contains("50%")) {
            //from.sendMessage("回答错误，请重试。");
            throw new BindException(BindException.Type.BIND_Question_Wrong);
        }

        //from.sendMessage(STR."已将 \{qq} 绑定到 (\{UID}) \{name} 上");
        from.sendMessage(
                String.format(BindException.Type.BIND_Progress_Binding.message, qq, UID, name)
        );

        bindDao.bindQQ(qq, new BinUser(UID, name));
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