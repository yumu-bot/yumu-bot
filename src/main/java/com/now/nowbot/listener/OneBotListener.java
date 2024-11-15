package com.now.nowbot.listener;

import com.mikuac.shiro.annotation.GroupMessageHandler;
import com.mikuac.shiro.annotation.common.Shiro;
import com.mikuac.shiro.common.utils.ShiroUtils;
import com.mikuac.shiro.core.Bot;
import com.mikuac.shiro.dto.event.message.GroupMessageEvent;
import com.now.nowbot.config.Permission;
import com.now.nowbot.permission.PermissionImplement;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.throwable.BotException;
import com.now.nowbot.throwable.LogException;
import com.now.nowbot.throwable.PermissionException;
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
    static int RECAL_TIME = 1000 * 100;
    Logger log = LoggerFactory.getLogger(OneBotListener.class);
    private static Map<String, MessageService> messageServiceMap = null;

    public void init(Map<String, MessageService> beanMap) throws BeansException {
        messageServiceMap = beanMap;
    }

    @GroupMessageHandler()
    @Async
    public void handle(Bot bot, GroupMessageEvent onebotEvent) {
        log.trace("收到消息[{}] -> {}", onebotEvent.getGroupId(), ShiroUtils.unescape(onebotEvent.getRawMessage()));
        var nowTime = System.currentTimeMillis();
        if (onebotEvent.getTime() < 1e10) {
            nowTime /= 1000;
        }
        // 对于超过 30秒 的消息直接舍弃, 解决重新登陆后疯狂刷命令
        if (nowTime - onebotEvent.getTime() > 30) return;
        var event = new com.now.nowbot.qq.onebot.event.GroupMessageEvent(bot, onebotEvent);
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
        // 网络请求异常都在服务里处理掉了, 即使未处理也不应该直接发送出来
        if (e instanceof BotException botException) {
            if (botException.hasImage()) {
                event.reply(((BotException) e).getImage());
                //QQMsgUtil.sendImage(from, ((BotException) e).getImage());
            } else {
                event.reply(e.getMessage()).recallIn(RECAL_TIME);
            }
        } else if (e instanceof SocketTimeoutException || e instanceof ConnectException || e instanceof UnknownHttpStatusCodeException) {
            log.info("连接超时:", e);
            event.reply("请求超时 (HTTP 408 Request Timeout)\n可能是 Bot 达到了 API 请求上限。\n请稍后再试。").recallIn(RECAL_TIME);
        } else if (e instanceof LogException) {
            log.info(e.getMessage(), ((LogException) e).getThrowable());
        } else if (e instanceof IllegalArgumentException) {
            log.error("正则异常", e);
        } else if (e instanceof PermissionException) {
            log.error(e.getMessage());
        } else {
            if (Permission.isSuperAdmin(event.getSender().getId())) event.reply(e.getMessage());
            log.error("捕捉其他异常", e);
        }
    }
}
