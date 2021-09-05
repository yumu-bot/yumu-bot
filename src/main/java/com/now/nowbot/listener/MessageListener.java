package com.now.nowbot.listener;

import com.now.nowbot.config.Permission;
import com.now.nowbot.entity.RequestError;
import com.now.nowbot.service.MessageService.MessageService;
import com.now.nowbot.service.MessageService.MsgSTemp;
import com.now.nowbot.throwable.RunError;
import com.now.nowbot.throwable.TipsException;
import com.now.nowbot.util.SendmsgUtil;
import kotlin.coroutines.CoroutineContext;
import net.mamoe.mirai.event.EventHandler;
import net.mamoe.mirai.event.SimpleListenerHost;
import net.mamoe.mirai.event.events.*;
import net.mamoe.mirai.message.data.PlainText;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.UnknownHttpStatusCodeException;

import java.net.ConnectException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

@Component
public class MessageListener extends SimpleListenerHost {

    private static final Logger log = LoggerFactory.getLogger(MessageListener.class);

    private ApplicationContext applicationContext;

    @Autowired
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Override
    public void handleException(@NotNull CoroutineContext context, @NotNull Throwable exception) {
        if (SimpleListenerHost.getEvent(exception) instanceof MessageEvent) {
            MessageEvent event = (MessageEvent) SimpleListenerHost.getEvent(exception);
            var e = SimpleListenerHost.getRootCause(exception);
            if (e instanceof TipsException) {
                event.getSubject().sendMessage(e.getMessage());
            } else if (e instanceof ConnectException || e instanceof UnknownHttpStatusCodeException) {
                event.getSubject().sendMessage("网络连接超时，请稍后再试");
            } else if (e instanceof RestClientException && e.getCause() instanceof RequestError) {
                RequestError reser = (RequestError) e.getCause();
                //error : The request is missing a required parameter, includes an invalid parameter value, includes a parameter more than once, or is otherwise malformed.
                //404 ?
                if (reser.status.value() == 404 || reser.status.getReasonPhrase().equals("Not Found")) {
                    event.getSubject().sendMessage("请求目标不存在");
                }else if(reser.status.getReasonPhrase().equals("Bad Request")){
                    event.getSubject().sendMessage("出现请求错误，可能为您的令牌已失效，请尝试更新令牌(私发bot\"!bind\")\n若仍未解决，请耐心等待bug修复");
                }
            } else if (e instanceof RunError) {
                log.error("严重异常:", e);
            } else {
//                if (Permission.superUser != null) {
//                    var errdate = getExceptionAllinformation((Exception) e);
//                    Permission.superUser.forEach(id -> {
//                        event.getBot().getFriend(id).sendMessage(event.getMessage().plus("\n" + errdate + "   " + format.format(System.currentTimeMillis())));
//                    });
//                }
                log.info("---->", e);
            }
        }
    }

    public static String getExceptionAllinformation(Exception ex) {
        StringBuilder sOut = new StringBuilder();
        StackTraceElement[] trace = ex.getStackTrace();
        sOut.append(ex.getMessage());
        for (StackTraceElement s : trace) {
            sOut.append("\tat ").append(s).append("\r\n");
        }
        return sOut.toString();
    }


    @Async
    @EventHandler
    public void msg(MessageEvent event) throws Throwable {
        for (var k : MsgSTemp.services.keySet()) {
            var matcher = k.matcher(event.getMessage().contentToString());
            if (matcher.find() && applicationContext != null) {
                var servicename = MsgSTemp.services.get(k);
                var service = (MessageService) applicationContext.getBean(servicename);
                service.HandleMessage(event, matcher);
            }

        }
    }

    @Async
    @EventHandler
    public void msg(BotInvitedJoinGroupRequestEvent event) throws Exception {
        StringBuffer sb = new StringBuffer("接收到来自\n");
        sb.append(event.getGroupName())
                .append('(')
                .append(event.getGroupId())
                .append(')');
        event.getBot().getFriend(365246692).sendMessage(sb.toString());
        event.accept();
    }

    @Async
    @EventHandler
    public void msg(MessagePreSendEvent event) throws Exception {
        SendmsgUtil.check(event);
    }

    @Async
    @EventHandler
    public void msg(MessagePostSendEvent event) {
        System.out.println(event.getMessage().contentToString());
    }

    /***
     * ImageUploadEvent 图片上传事件
     */
    @Async
    @EventHandler
    public void msg(ImageUploadEvent event) {
        if (event instanceof ImageUploadEvent.Failed) {
            log.info("图片上传失败");
        }
        if (event instanceof ImageUploadEvent.Succeed) {
            log.info("图片上传成功");
        }
    }

}
