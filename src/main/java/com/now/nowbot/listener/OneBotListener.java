package com.now.nowbot.listener;

import com.mikuac.shiro.annotation.GroupMessageHandler;
import com.mikuac.shiro.annotation.common.Shiro;
import com.mikuac.shiro.common.utils.MsgUtils;
import com.mikuac.shiro.core.Bot;
import com.mikuac.shiro.core.BotPlugin;
import com.mikuac.shiro.dto.event.message.GroupMessageEvent;
import com.now.nowbot.config.Permission;
import com.now.nowbot.service.MessageService.MessageService;
import com.now.nowbot.throwable.*;
import com.now.nowbot.util.ASyncMessageUtil;
import com.now.nowbot.util.Instruction;
import net.mamoe.mirai.event.SimpleListenerHost;
import net.mamoe.mirai.event.events.EventCancelledException;
import org.springframework.beans.BeansException;
import org.springframework.core.annotation.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.UnknownHttpStatusCodeException;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Map;
import java.util.regex.Matcher;


@Shiro
@Order(5)
@Component("OneBotListener")
public class OneBotListener {
    Logger log = LoggerFactory.getLogger(OneBotListener.class);
    private static Map<Class<? extends MessageService>, MessageService> messageServiceMap = null;
    public void init(Map<Class<? extends MessageService>, MessageService> beanMap) throws BeansException {
        messageServiceMap = beanMap;
    }

    @GroupMessageHandler()
    public void test(Bot bot, GroupMessageEvent onebotEvent) {
        var event = new com.now.nowbot.qq.onebot.event.GroupMessageEvent(bot, onebotEvent);
        ASyncMessageUtil.put(event);
        for(var ins : Instruction.values()){
            //功能关闭 优先级高于aop拦截
            if (Permission.serviceIsClouse(ins) && !Permission.isSupper(event.getSender().getId())) continue;

            try {
                Matcher matcher = ins.getRegex().matcher(event.getMessage());
                if (matcher.find()) {
                    var service = messageServiceMap.get(ins.getaClass());
                    service.HandleMessage(event, matcher);
                }
            } catch (Throwable e) {
                errorHandle(event, e);
            }
        }
    }

    public void errorHandle(com.now.nowbot.qq.onebot.event.GroupMessageEvent event, Throwable e) {
        if (e instanceof TipsException || e instanceof TipsRuntimeException) {
//            event.getSubject().sendMessage(e.getMessage()).recallIn(RECAL_TIME);
            event.getSubject().sendMessage(e.getMessage());
        } else if (e instanceof SocketTimeoutException || e instanceof ConnectException || e instanceof UnknownHttpStatusCodeException) {
            log.info("连接超时:",e);
//            event.getSubject().sendMessage("请求超时 (HTTP 408 Request Timeout)\n可能是 Bot 达到了 API 请求上限。\n请稍后再试。").recallIn(RECAL_TIME);
            event.getSubject().sendMessage("请求超时 (HTTP 408 Request Timeout)\n可能是 Bot 达到了 API 请求上限。\n请稍后再试。");
        } else if (e instanceof RequestException reser) {
            log.info("请求错误:",e);
                /*
                if (reser.status.value() == 404 || reser.status.getReasonPhrase().equals("Not Found")) {
                    event.getSubject().sendMessage("请求失败 (HTTP 404 Not Found)\n").recallIn(RECAL_TIME);
                } else if(reser.status.value() == 400 || reser.status.getReasonPhrase().equals("Bad Request")){
                    event.getSubject().sendMessage("请求错误 (HTTP 400 Bad Request)\n请耐心等待 Bug 修复").recallIn(RECAL_TIME);
                } else if(reser.status.value() == 401 || reser.status.getReasonPhrase().equals("Unauthorized")){
                    event.getSubject().sendMessage("验证失败 (HTTP 401 Unauthorized)\n请尝试重新绑定 (!ymbind / !ymbi / !bi)").recallIn(RECAL_TIME);
                    // 出现请求错误，可能为您的令牌已失效，请尝试更新令牌(发送"!bind") 若仍未解决，请耐心等待bug修复
                } else {
                    event.getSubject().sendMessage("其他错误 (HTTP " + reser.status.value() + " " + reser.status.getReasonPhrase() + ")");
                }
                 */
            switch (reser.status.value()) {
                case 400 -> event.getSubject().sendMessage("请求错误 (HTTP 400 Bad Request)\n请耐心等待 Bug 修复");
                case 401 -> event.getSubject().sendMessage("验证失败 (HTTP 401 Unauthorized)\n您的令牌可能已失效。\n请尝试重新绑定 (!ymbind / !ymbi / !bi)\n请不要使用[!ymbind + 你的名字]这种方法。\n绑定方法可以使用 !h b 查询。");
                case 404 -> event.getSubject().sendMessage("请求失败 (HTTP 404 Not Found)\n请检查输入的内容。");
                case 408 -> event.getSubject().sendMessage("请求超时 (HTTP 408 Request Timeout)\n可能是 Bot 达到了 API 请求上限。\n请稍后再试。");
                default -> event.getSubject().sendMessage("其他错误 (HTTP " + reser.status.value() + " " + reser.status.getReasonPhrase() + ")\n请及时反馈给维护人员。");
            }

        } else if (e instanceof EventCancelledException) {
            log.info("取消消息发送 {}", e.getMessage());
        } else if (e instanceof LogException) {
            log.info(e.getMessage(), ((LogException) e).getThrowable());
        } else if (e instanceof IllegalArgumentException) {
            log.error("正则异常",e);
        } else if (e instanceof PermissionException) {
            log.error(e.getMessage());
        } else {
            if (Permission.isSupper(event.getSender().getId())) event.getSubject().sendMessage(e.getMessage());
            log.error("捕捉其他异常", e);
        }
    }
}
