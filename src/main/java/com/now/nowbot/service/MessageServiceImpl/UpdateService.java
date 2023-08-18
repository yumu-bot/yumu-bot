package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.aop.CheckPermission;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.util.UpdateUtil;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;

@Service("update")
public class UpdateService implements MessageService {
    @Override
    @CheckPermission(supperOnly = true)
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        event.getSubject().sendMessage("即将更新重启...");
        UpdateUtil.update();
    }
}
