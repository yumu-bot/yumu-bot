package com.now.nowbot.service.messageServiceImpl;

import com.now.nowbot.dao.BindDao;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.MessageService;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.random.RandomGenerator;

@Service("LOGIN")
public class LoginService implements MessageService<String> {
    public static final Map<String, LoginUser> LOGIN_USER_MAP = new ConcurrentHashMap<>();
    static final private int CODE_SIZE = 6;
    static Random random = Random.from(RandomGenerator.getDefault());
    BindDao bindDao;

    public LoginService(BindDao bindDao) {
        this.bindDao = bindDao;
        Thread.startVirtualThread(() -> {
            while (true) {
                try {
                    Thread.sleep(Duration.ofSeconds(120));
                } catch (InterruptedException ignore) {

                }
                final long t = System.currentTimeMillis();
                LOGIN_USER_MAP.entrySet().removeIf((entry) -> t - entry.getValue().time > 60 * 1000);
            }
        });
    }

    @Override
    public boolean isHandle(MessageEvent event, String messageText, DataValue<String> data) throws Throwable {
        return "!login".equals(messageText);
    }

    @Override
    public void HandleMessage(MessageEvent event, String data) throws Throwable {
        var qq = event.getSender().getId();
        var u = bindDao.getUserFromQQ(qq);
        String code;
        // 防止key重复, 循环构造随机字符串
        while (LOGIN_USER_MAP.containsKey((code = getRoStr()).toUpperCase())) {
        }
        event.getSubject().sendMessage(STR."您的登录验证码: \{code}");
        LOGIN_USER_MAP.put(code.toUpperCase(), new LoginUser(u.getOsuID(), u.getOsuName(), System.currentTimeMillis()));
    }

    static String getRoStr() {
        char[] t = new char[CODE_SIZE];
        for (int i = 0; i < CODE_SIZE; i++) {
            int temp = random.nextInt(0, 36);
            if (temp < 10) {
                t[i] = (char) ('0' + temp);
            } else {
                // 防止 'l' 与 'I', 0 与 'O' 混淆
                if (temp == 'o' - 'a') temp += 3;
                if (temp != 18 && (temp == 21 || random.nextBoolean())) {
                    temp -= 10;
                } else {
                    temp -= 42;
                }
                t[i] = (char) ('a' + temp);
            }
        }
        return new String(t);
    }

    public record LoginUser(Long uid, String name, Long time) {
    }
}
