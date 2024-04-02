package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.config.Permission;
import com.now.nowbot.dao.BindDao;
import com.now.nowbot.entity.bind.QQBindLite;
import com.now.nowbot.model.BinUser;
import com.now.nowbot.model.JsonData.OsuUser;
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
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.TaskExecutor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Service("BIND")
public class BindService implements MessageService<BindService.BindParam> {
    private static final Logger log = LoggerFactory.getLogger(BindService.class);
    public static final Map<Long, BindData> BIND_MSG_MAP = new ConcurrentHashMap<>();
    private static boolean CLEAR = false;

    @Resource
    OsuUserApiService userApiService;
    @Resource
    BindDao bindDao;
    @Resource
    TaskExecutor taskExecutor;

    // full: 全绑定，只有 bot 开发可以这样做
    public record BindParam(@NonNull Long qq, String name, boolean at, boolean unbind, boolean isSuper, boolean isFull) {}

    @Override
    public boolean isHandle(MessageEvent event, String messageText, DataValue<BindParam> data) throws Throwable {
        var m = Instructions.BIND.matcher(messageText);
        if (!m.find()) return false;

        var qqStr = m.group("qq");
        var name = m.group("name");
        var at = QQMsgUtil.getType(event.getMessage(), AtMessage.class);

        //!bind 给个提示
        if (Objects.isNull(m.group("ym")) && Objects.isNull(m.group("un")) && Objects.nonNull(m.group("bind"))) {

            //!bind osu
            if (StringUtils.hasText(name) && Pattern.matches("\\s*(?i)osu\\s*", name)) {
                OsuUser user;
                try {
                    user = userApiService.getPlayerInfo(name);
                } catch (WebClientResponseException e) {
                    log.info("绑定：退避成功：!bind osu <name>");
                    return false;
                }
                name = user.getUsername();
            }

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

        var qq = event.getSender().getId();
        boolean unbind = Objects.nonNull(m.group("un")) || Objects.nonNull(m.group("ub"));
        boolean isSuper = Permission.isSuperAdmin(qq);
        boolean isFull = Objects.nonNull(m.group("full"));

        if (Objects.nonNull(at)) {
            data.setValue(new BindParam(at.getTarget(), null, true, unbind, isSuper, isFull));
            return true;
        } else if (StringUtils.hasText(name)) {
            if (Objects.nonNull(qqStr)) {
                data.setValue(new BindParam(Long.parseLong(qqStr), name, false, unbind, isSuper, isFull));
                return true;
            } else {
                data.setValue(new BindParam(qq, name, false, unbind, isSuper, isFull));
                return true;
            }
        } else if (StringUtils.hasText(qqStr) && ! qqStr.trim().equals("0")) {
            data.setValue(new BindParam(Long.parseLong(qqStr), null, false, unbind, isSuper, isFull));
            return true;
        } else {
            data.setValue(new BindParam(qq, null, false, unbind, isSuper, isFull));
            return true;
        }
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

    /*

    public static void main(String[] args) {
        getQuestion(null);
    }

     */

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

    private static int find(int[][] map, int size, int start, int end) {
        int[] toMin = new int[size];
        int[] find = new int[size];
        find[start] = 1;
        Arrays.fill(toMin, Integer.MAX_VALUE);
        int point = start;
        int pointBef = start;
        for (int n = 0; n < size - 1; n++) {

            int minIndex = - 1;
            int min = Integer.MAX_VALUE;
            for (int i = 0; i < size; i++) {
                if (find[i] == 1) continue;
                if (map[pointBef][point] + map[point][i] >= toMin[i]) continue;
                toMin[i] = map[pointBef][point] + map[point][i];
                if (min > toMin[i]) {
                    min = toMin[i];
                    minIndex = i;
                }
            }
            if (minIndex == end) return toMin[minIndex];
            if (minIndex < 0) {
                for (int i = 0; i < size; i++) {
                    if (find[i] == 1) continue;
                    if (min > toMin[i]) {
                        min = toMin[i];
                        minIndex = i;
                    }
                }
            }
            System.out.print((char) ('A' + minIndex));
            System.out.println(STR."  \{Arrays.toString(toMin)}");
            find[minIndex] = 1;
            pointBef = point;
            point = minIndex;

        }
        return - 1;
    }

    public record BindData(Long key, MessageReceipt receipt, Long QQ) {
    }

    //默认绑定路径
    private void bindQQ(MessageEvent event, long qq, boolean isFull) throws BindException {
        var from = event.getSubject();
        BinUser binUser;
        OsuUser osuUser = null;

        //检查是否已经绑定
        var qqLiteFromQQ = bindDao.getQQLiteFromQQ(qq);
        QQBindLite qqBindLite;
        if (qqLiteFromQQ.isPresent() && (qqBindLite = qqLiteFromQQ.get()).getBinUser().isAuthorized()) {
            binUser = qqBindLite.getBinUser();
            try {
                try {
                    osuUser = userApiService.getPlayerInfo(binUser, OsuMode.DEFAULT);
                    from.sendMessage(
                            String.format(BindException.Type.BIND_Progress_BindingRecoverInfo.message, binUser.getOsuID(), binUser.getOsuName())
                    );
                } catch (WebClientResponseException.Unauthorized e) {
                    from.sendMessage(
                            String.format(BindException.Type.BIND_Progress_NeedToReBindInfo.message,
                                    binUser.getOsuID(), Optional.ofNullable(binUser.getOsuName()).orElse("?")
                            )
                    );
                }

                if (Objects.nonNull(osuUser) && ! osuUser.getUID().equals(binUser.getOsuID())) {
                    throw new RuntimeException();
                }

                var lock = ASyncMessageUtil.getLock(event);
                var s = lock.get();
                if (Objects.isNull(s) || ! s.getRawMessage().toUpperCase().contains("OK")) {
                    from.sendMessage(BindException.Type.BIND_Receive_Refused.message);
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
            putBind(timeMillis, new BindData(timeMillis, receipt, qq));
        }
    }

    public static boolean contains(Long t) {
        return BIND_MSG_MAP.containsKey(t);
    }

    public static BindData getBind(Long t) {
        removeOldBind();
        return BIND_MSG_MAP.get(t);
    }

    public static void removeBind(Long t) {
        BIND_MSG_MAP.remove(t);
    }

    private static void removeOldBind() {
        BIND_MSG_MAP.keySet().removeIf(k -> (k + 120 * 1000) < System.currentTimeMillis());
    }

    private void putBind(Long t, BindData b) {
        removeOldBind();
        if (BIND_MSG_MAP.size() > 20 && ! CLEAR) {
            CLEAR = true;
            taskExecutor.execute(() -> {
                try {
                    Thread.sleep(1000 * 5);
                } catch (InterruptedException ignored) {
                    // ignore
                }
                removeOldBind();
                CLEAR = false;
            });
        }
        BIND_MSG_MAP.put(t, b);
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
            return;
        }

        var a = getQuestion(from);
        var lock = ASyncMessageUtil.getLock(event, 30000);
        event = lock.get();

        if (Objects.isNull(event)) {
            throw new BindException(BindException.Type.BIND_Question_Overtime);
        }

        if (! a.contains(event.getTextMessage())) {
            throw new BindException(BindException.Type.BIND_Question_Wrong);
        }

        from.sendMessage(
                String.format(BindException.Type.BIND_Progress_Binding.message, qq, UID, name)
        );

        bindDao.bindQQ(qq, new BinUser(UID, name));
    }

    public static Set<String> getQuestion(@NonNull Contact contact) {
        Random random = new Random();
        int start = random.nextInt(0, 5);
        int end = random.nextInt(5, 10);
        int[][] cost = new int[10][10];
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            sb.append('\t').append((char) ('A' + i));
        }
        sb.append('\n');
        for (int i = 0; i < 100; i++) {
            if (i / 10 != i % 10) cost[i / 10][i % 10] = random.nextInt(9, 1000);
            if (i % 10 == 0) sb.append((char) ('A' + (i / 10)));
            sb.append('\t').append(cost[i / 10][i % 10]);
            if (i % 10 == 9) sb.append('\n');
        }
        var question = STR."""
                ### 请回答本问题:

                已知有向图, 由 A-J 10个节点组成, 下面是使用邻接矩阵表示

                ```
                \{sb.toString()}
                ```

                请半分钟内回答：\{(char) ('A' + start)} 到 \{(char) ('A' + end)} 的最短距离

                直接回复数字即可
                """;

//        System.out.println(question);
//        System.out.println(find(cost, 10, start, end));
        contact.sendMessage("√(0.25)是多少");
        return new HashSet<>(List.of("0.5", "1/2"));
    }
}