package com.now.nowbot.listener;

import com.now.nowbot.config.Permission;
import com.now.nowbot.error.TipsError;
import com.now.nowbot.service.MessageService.MessageService;
import com.now.nowbot.service.MessageService.MsgSTemp;
import kotlin.coroutines.CoroutineContext;
import net.mamoe.mirai.event.EventHandler;
import net.mamoe.mirai.event.SimpleListenerHost;
import net.mamoe.mirai.event.events.BotInvitedJoinGroupRequestEvent;
import net.mamoe.mirai.event.events.ImageUploadEvent;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.event.events.MessagePreSendEvent;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

@Component
public class MessageListener extends SimpleListenerHost{

    private static final Logger log = LoggerFactory.getLogger(MessageListener.class);

    ApplicationContext applicationContext;
    @Autowired
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
    DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    Permission permission;
    @Autowired
    public void setPermission(Permission p){
        permission = p;
    }
    @Override
    public void handleException(@NotNull CoroutineContext context, @NotNull Throwable exception){
        if(SimpleListenerHost.getEvent(exception) instanceof MessageEvent){
            MessageEvent event = (MessageEvent) SimpleListenerHost.getEvent(exception);
            var e = SimpleListenerHost.getRootCause(exception);
            if(e instanceof TipsError) {
                event.getSubject().sendMessage(e.getMessage());
            }else {
                if (permission != null && permission.superUser != null){
                    var errdate = getExceptionAllinformation((Exception) e);
                    permission.superUser.forEach(id->{
                        event.getBot().getFriend(id).sendMessage(event.getMessage().plus("\n"+errdate+"   "+format.format(System.currentTimeMillis())));
                    });
                }
                log.info("---->",e);
            }
        }
    }

    public static String getExceptionAllinformation(Exception ex){
        String sOut = "";
        StackTraceElement[] trace = ex.getStackTrace();
        for (StackTraceElement s : trace) {
            sOut += "\tat " + s + "\r\n";
        }
        return sOut;
    }



    @Async
    @EventHandler
    public void msg(MessageEvent event) throws Throwable{
        for (var k : MsgSTemp.services.keySet()){
            var matcher = k.matcher(event.getMessage().contentToString());
            if(matcher.find()){
                var servicename = MsgSTemp.services.get(k);
                var service = (MessageService)applicationContext.getBean(servicename);
                service.HandleMessage(event, matcher);
            }
        }
    }
    @Async
    @EventHandler
    public void msg(BotInvitedJoinGroupRequestEvent event) throws Exception{
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
    public void msg(MessagePreSendEvent event){
        System.out.println(event.getMessage().contentToString());
    }
    /***
     * ImageUploadEvent 图片上传事件
     */
    @Async
    @EventHandler
    public void msg(ImageUploadEvent event){
        if(event instanceof ImageUploadEvent.Failed){
            log.info("图片上传失败");
        }
        if(event instanceof ImageUploadEvent.Succeed){
            log.info("图片上传成功");
        }
    }

}
