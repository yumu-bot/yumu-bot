package com.now.nowbot.listener;

import com.mikuac.shiro.annotation.GroupMessageHandler;
import com.mikuac.shiro.annotation.common.Shiro;
import com.mikuac.shiro.common.utils.ShiroUtils;
import com.mikuac.shiro.core.Bot;
import com.mikuac.shiro.dto.event.message.GroupMessageEvent;
import com.now.nowbot.config.Permission;
import com.now.nowbot.qq.onebot.event.MessageEvent;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.throwable.BotException;
import com.now.nowbot.throwable.LogException;
import com.now.nowbot.throwable.PermissionException;
import com.now.nowbot.util.ASyncMessageUtil;
import com.now.nowbot.util.ContextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.UnknownHttpStatusCodeException;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.Map;


@Shiro
@Order(5)
@Component("OneBotListener")
public class OneBotListener {
    static int RECAL_TIME = 1000 * 100;
    Logger log = LoggerFactory.getLogger(OneBotListener.class);
    private static Map<String, MessageService> messageServiceMap = null;

    public void init(Map<String, MessageService> beanMap) throws BeansException {
        messageServiceMap = beanMap;
    }

    @GroupMessageHandler()
    @Async
    public void handle(Bot bot, GroupMessageEvent onebotEvent) {
        var event = new com.now.nowbot.qq.onebot.event.GroupMessageEvent(bot, onebotEvent);
        log.trace("收到消息[{}] -> {}", event.getSubject().getId(), ShiroUtils.unescape(onebotEvent.getRawMessage()));
        if (event.getSender().getId() == 365246692L) {
            ContextUtil.setContext("isTest", Boolean.TRUE);
        }
        ASyncMessageUtil.put(event);
        String textMessage = event.getTextMessage().trim();
        for (var ins : Permission.getAllService()) {
            //功能关闭 优先级高于aop拦截
            if (Permission.isServiceClose(ins) && !Permission.isSuperAdmin(event.getSender().getId())) continue;
            if (Permission.checkStopListener()) break;
            try {
                var service = messageServiceMap.get(ins);
                var d = new MessageService.DataValue();
                if (service.isHandle(event, textMessage, d)) {
                    service.HandleMessage(event, d.getValue());
                }
            } catch (Throwable e) {
                errorHandle(event, e);
            }
        }
    }

    public void errorHandle(MessageEvent event, Throwable e) {
        var from = event.getSubject();
        // 网络请求异常都在服务里处理掉了, 即使未处理也不应该直接发送出来
        if (e instanceof BotException botException) {
            if (botException.hasImage()) {
                from.sendImage(((BotException) e).getImage());
                //QQMsgUtil.sendImage(from, ((BotException) e).getImage());
            } else {
                from.sendMessage(e.getMessage()).recallIn(RECAL_TIME);
            }
        } else if (e instanceof SocketTimeoutException || e instanceof ConnectException || e instanceof UnknownHttpStatusCodeException) {
            log.info("连接超时:", e);
            from.sendMessage("请求超时 (HTTP 408 Request Timeout)\n可能是 Bot 达到了 API 请求上限。\n请稍后再试。").recallIn(RECAL_TIME);
        } else if (e instanceof LogException) {
            log.info(e.getMessage(), ((LogException) e).getThrowable());
        } else if (e instanceof IllegalArgumentException) {
            log.error("正则异常", e);
        } else if (e instanceof PermissionException) {
            log.error(e.getMessage());
        } else {
            if (Permission.isSuperAdmin(event.getSender().getId())) event.getSubject().sendMessage(e.getMessage());
            log.error("捕捉其他异常", e);
        }
    }
}
