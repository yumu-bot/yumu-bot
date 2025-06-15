package com.now.nowbot.listener;

import com.mikuac.shiro.annotation.GroupMessageHandler;
import com.mikuac.shiro.annotation.common.Shiro;
import com.mikuac.shiro.common.utils.ShiroUtils;
import com.mikuac.shiro.core.Bot;
import com.mikuac.shiro.dto.event.message.GroupMessageEvent;
import com.now.nowbot.config.Permission;
import com.now.nowbot.permission.PermissionImplement;
import com.now.nowbot.qq.message.MessageChain;
import com.now.nowbot.service.IdempotentService;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.throwable.BotException;
import com.now.nowbot.throwable.botRuntimeException.LogException;
import com.now.nowbot.util.ContextUtil;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.core.annotation.Order;
import org.springframework.core.codec.DecodingException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.UnknownHttpStatusCodeException;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.Map;

////////////////////////////////////////////////////////////////////
//                          _ooOoo_                               //
//                         o8888888o                              //
//                         88" . "88                              //
//                         (| ^_^ |)                              //
//                         O\  =  /O                              //
//                      ____/`---'\____                           //
//                    .'  \\|     |//  `.                         //
//                   /  \\|||  :  |||//  \                        //
//                  /  _||||| -:- |||||-  \                       //
//                  |   | \\\  -  /// |   |                       //
//                  | \_|  ''\---/''  |   |                       //
//                  \  .-\__  `-`  ___/-. /                       //
//                ___`. .'  /--.--\  `. . ___                     //
//              ."" '<  `.___\_<|>_/___.'  >'"".                  //
//            | | :  `- \`.;`\ _ /`;.`/ - ` : | |                 //
//            \  \ `-.   \_ __\ /__ _/   .-` /  /                 //
//      ========`-.____`-.___\_____/___.-`____.-'========         //
//                           `=---='                              //
//      ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^        //
//              佛祖保佑       永不宕机     永无BUG                   //
////////////////////////////////////////////////////////////////////

@Shiro
@Order(9)
@Component("OneBotListener")
@SuppressWarnings("unused")
public class OneBotListener {
    static int RECALL_TIME = 1000 * 100;
    Logger log = LoggerFactory.getLogger(OneBotListener.class);
    private static Map<String, MessageService> messageServiceMap = null;

    @Resource
    IdempotentService idempotentService;

    public void init(Map<String, MessageService> beanMap) throws BeansException {
        messageServiceMap = beanMap;
    }

    @GroupMessageHandler()
    @Async
    public void handle(Bot bot, GroupMessageEvent onebotEvent) {
        var groupId = onebotEvent.getGroupId();
        var message = ShiroUtils.unescape(onebotEvent.getMessage());
        var messageId = String.format(
                "[%s|%s]%s(%s)",
                groupId.toString(),
                onebotEvent.getSender().getUserId().toString(),
                onebotEvent.getSubType(),
                onebotEvent.getTime().toString()
                );
        if (!idempotentService.checkByMessageId(messageId)) {
            return;
        }
        log.debug("收到消息 {}", messageId);
        var nowTime = System.currentTimeMillis();
        if (onebotEvent.getTime() < 1e10) {
            nowTime /= 1000;
        }
        // 对于超过 30秒 的消息直接舍弃, 解决重新登陆后疯狂刷命令
        if (nowTime - onebotEvent.getTime() > 30) return;
        var event = new com.now.nowbot.qq.onebot.event.GroupMessageEvent(bot, onebotEvent);
        // if (event.getGroup().getId() != 746671531) return;
        if (event.getSender().getId() == 365246692L) {
            ContextUtil.setContext("isTest", Boolean.TRUE);
        }
        try {
            PermissionImplement.onMessage(event, this::errorHandle);
        } finally {
            ContextUtil.remove();
        }
    }

    public void errorHandle(com.now.nowbot.qq.event.MessageEvent event, Throwable e) {
        var from = event.getSubject();

        if (e instanceof BotException botException) {
            // 有些网络请求异常被包装在这里
            if (botException.hasImage()) {
                event.reply(botException.getImage());
            } else {
                event.reply(e.getMessage()).recallIn(RECALL_TIME);
            }
        } else if (e instanceof SocketTimeoutException || e instanceof ConnectException || e instanceof UnknownHttpStatusCodeException) {
            log.info("连接超时:", e);
            event.reply("请求超时 (HTTP 408 Request Timeout)\n可能是 Bot 达到了 API 请求上限。\n请稍后再试。").recallIn(RECALL_TIME);
        } else if (e instanceof LogException) {
            log.info(e.getMessage());
        } else if (e instanceof IllegalStateException) {
            event.reply(new MessageChain(e.getCause().getMessage()));
        } else if (e instanceof IllegalArgumentException) {
            log.error("正则异常", e);
        } else if (e instanceof DecodingException) {
            log.error("JSON 解码异常", e);
        } else {
            if (Permission.isSuperAdmin(event.getSender().getId())) {
                event.reply(e.getMessage()).recallIn(RECALL_TIME);
            }
            log.error("捕捉其他异常", e);
        }
    }
}
