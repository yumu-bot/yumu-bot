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
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.service.OsuApiService.OsuUserApiService;
import com.now.nowbot.throwable.ServiceException.BindException;
import com.now.nowbot.util.ASyncMessageUtil;
import com.now.nowbot.util.Instruction;
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
import java.util.function.Predicate;

import static com.now.nowbot.util.command.CommandPatternStaticKt.FLAG_NAME;
import static com.now.nowbot.util.command.CommandPatternStaticKt.FLAG_QQ_ID;

@Service("BIND")
public class BindService implements MessageService<BindService.BindParam> {
    private static final Logger                log          = LoggerFactory.getLogger(BindService.class);
    public static final  Map<Long, BindData>   BIND_MSG_MAP = new ConcurrentHashMap<>();
    private static final Map<Long, List<Long>> BIND_CACHE   = new ConcurrentHashMap<>();
    private static       boolean               CLEAR        = false;

    @Resource
    OsuUserApiService userApiService;
    @Resource
    BindDao           bindDao;
    @Resource
    TaskExecutor      taskExecutor;
    @Resource
    ImageService      imageService;


    // full: 全绑定，只有 bot 开发可以这样做
    public record BindParam(@NonNull Long qq,
                            String name,
                            boolean at,
                            boolean unbind,
                            boolean isSuper,
                            boolean isFull
    ) {
    }

    @Override
    public boolean isHandle(MessageEvent event, String messageText, DataValue<BindParam> data) throws Throwable {
        var m = Instruction.BIND.matcher(messageText);
        if (!m.find()) return false;

        var from = event.getSubject();

        var qqStr = m.group(FLAG_QQ_ID);
        var name = m.group(FLAG_NAME);
        if (name.trim().isEmpty()) name = null;
        var at = QQMsgUtil.getType(event.getMessage(), AtMessage.class);
        // 带着 ym 以及特殊短链不用问
        boolean isYmBot = messageText.substring(0, 3).contains("ym") ||
                m.group("bi") != null ||
                m.group("un") != null ||
                m.group("ub") != null;

        //!bind 给个提示
        if (
                !isYmBot &&
                        Objects.nonNull(m.group("bind"))
        ) {
            //!bind osu
            if (StringUtils.hasText(name) && name.contains("osu")) {
                if (userApiService.isPlayerExist(name)) {
                    var user = userApiService.getPlayerInfo(name);
                    name = user.getUsername();
                } else {
                    log.info("绑定：退避成功：!bind osu <data>");
                    return false;
                }
            }

            // 提问
            var receipt = from.sendMessage(BindException.Type.BIND_Question_BindRetreat.message);

            var lock = ASyncMessageUtil.getLock(event, 30 * 1000);
            event = lock.get();

            if (Objects.isNull(event) || !event.getRawMessage().toUpperCase().contains("OK")) {
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

        BindParam param;

        if (Objects.nonNull(at)) {
            // bi/ub @
            param = new BindParam(at.getTarget(), name, true, unbind, isSuper, isFull);
        } else if (StringUtils.hasText(qqStr) && !qqStr.trim().equals("0")) {
            // bi qq=123
            param = new BindParam(Long.parseLong(qqStr), name, false, unbind, isSuper, isFull);
        } else {
            // bi
            param = new BindParam(qq, name, false, unbind, isSuper, isFull);
        }
        data.setValue(param);
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

        // bi name (self)
        if (Objects.nonNull(param.name) && me == param.qq) {
            bindQQName(event, param.name, me);
            return;
        }

        if (param.unbind && !param.isSuper) {
            // ub 但是不是自己, 也不是超管
            throw new BindException(BindException.Type.BIND_Me_Blacklisted);
        }

        // 超管 解绑
        if (param.isSuper && param.unbind()) {
            if (Objects.nonNull(param.name)) {
                // name
                unbindName(param.name);
            } else {
                unbindQQ(param.qq);
            }
            return;
        }

        //超级管理员的专属权利：艾特绑定和全 QQ 移除绑定
        if (param.isSuper) {
            if (Objects.nonNull(param.name)) {
                bindQQName(event, param.name, param.qq);
            } else if (param.at) {
                bindQQAt(event, param.qq);
            }
            return;
        }

        if (me == param.qq) {
            bindQQ(event, me, param.isFull);
        }

        // 不是超管，也不是自己
        throw new BindException(BindException.Type.BIND_Me_Blacklisted);
    }

    private void unbindName(String name) throws BindException {
        var uid = bindDao.getOsuId(name);
        if (uid == null)
            throw new BindException(BindException.Type.BIND_Player_HadNotBind);
        Long qq;
        try {
            qq = bindDao.getQQ(uid);
        } catch (Exception e) {
            throw new BindException(BindException.Type.BIND_Player_HadNotBind);
        }
        unbindQQ(qq);
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

                if (Objects.nonNull(osuUser) && !osuUser.getUserID().equals(binUser.getOsuID())) {
                    throw new RuntimeException();
                }

                var lock = ASyncMessageUtil.getLock(event);
                var s = lock.get();
                if (Objects.isNull(s) || !s.getRawMessage().toUpperCase().contains("OK")) {
                    from.sendMessage(BindException.Type.BIND_Receive_Refused.message);
                    return;
                }

            } catch (HttpClientErrorException.Unauthorized | WebClientResponseException.Unauthorized |
                     BindException e) {
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
        if (BIND_MSG_MAP.size() > 20 && !CLEAR) {
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
        if (check(event.getSender().getId())) {
            return;
        }
        var a = getSimplifiedQuestion(from);
        var lock = ASyncMessageUtil.getLock(event, 30000);
        event = lock.get();

        if (Objects.isNull(event)) {
            throw new BindException(BindException.Type.BIND_Question_Overtime);
        }

        if (!a.contains(event.getTextMessage().trim())) {
            throw new BindException(BindException.Type.BIND_Question_Wrong);
        }

        from.sendMessage(
                String.format(BindException.Type.BIND_Progress_Binding.message, qq, UID, name)
        );

        bindDao.bindQQ(qq, new BinUser(UID, name));
    }

    private static int find(int[][] map, int size, int start, int end) {
        int[] toMin = new int[size];
        int[] find = new int[size];
        find[start] = 1;
        Arrays.fill(toMin, Integer.MAX_VALUE);
        int point = start;
        int pointBef = start;
        for (int n = 0; n < size - 1; n++) {

            int minIndex = -1;
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
            find[minIndex] = 1;
            pointBef = point;
            point = minIndex;
        }
        return toMin[end];
    }

    // 重申一遍，门槛至少设为普通高等院校大一年级学生会接触到的难度，而不是计算机专业的学生会接触到的难度
    public Set<String> getSimplifiedQuestion(@NonNull Contact from) {
        from.sendMessage("不定积分 ∫dx 在 x=1144770 到 x=1146381 上的积分值是多少？");
        return new HashSet<>(List.of("1611", "一六一一", "guozi", "Guozi", "guo zi", "Guo Zi", "果子", "guozi on osu"));
    }

    /**
     * 检查绑定次数
     */
    boolean check(Long qq) {
        Predicate<Long> check = t -> t + 1000 * 60 * 30 < System.currentTimeMillis();
        BIND_CACHE.entrySet().removeIf(e -> {
            e.getValue().removeIf(check);
            return e.getValue().isEmpty();
        });
        var timeList = BIND_CACHE.computeIfAbsent(qq, k -> new ArrayList<>());
        timeList.removeIf(check);
        timeList.addLast(System.currentTimeMillis());
        return timeList.size() > 3;
    }

    public Set<String> getQuestion(@NonNull Contact from) {
        Random random = new Random();
        int start = random.nextInt(0, 5);
        int end = random.nextInt(5, 10);
        int[][] cost = new int[10][10];
        StringBuilder sb = new StringBuilder();
        sb.append("|.|");
        for (int i = 0; i < 10; i++) {
            sb.append(' ').append((char) ('A' + i)).append(" |");
        }
        sb.append('\n');
        sb.append("|---|---|---|---|---|---|---|---|---|---|---|");
        sb.append('\n');
        for (int i = 0; i < 100; i++) {
            if (i / 10 != i % 10) cost[i / 10][i % 10] = random.nextInt(9, 1000);
            if (i % 10 == 0) sb.append("|").append((char) ('A' + (i / 10)));
            sb.append('|').append(cost[i / 10][i % 10]);
            if (i % 10 == 9) sb.append("|\n");
        }
        // 11
        var question = STR."""
                ### 请回答本问题:

                已知有向图, 由节点 A-J 组成, 下面是使用邻接矩阵表示

                \{sb.toString()}

                请回答：\{(char) ('A' + start)} 到 \{(char) ('A' + end)} 的最短距离

                直接回复数字即可
                """;
        var result = find(cost, 10, start, end);
        log.info("bind result: {}", result);
        var image = imageService.getMarkdownImage(question, 730);
        from.sendImage(image);
        return new HashSet<>(Collections.singletonList(String.valueOf(result)));
    }
}