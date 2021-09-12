package com.now.nowbot.listener;

import com.alibaba.fastjson.JSONObject;
import com.now.nowbot.config.Permission;
import com.now.nowbot.entity.MsgLite;
import com.now.nowbot.mapper.MessageMapper;
import com.now.nowbot.throwable.RequestError;
import com.now.nowbot.service.MessageService.MessageService;
import com.now.nowbot.throwable.RunError;
import com.now.nowbot.throwable.TipsException;
import com.now.nowbot.throwable.TipsRuntimeException;
import com.now.nowbot.util.Instruction;
import com.now.nowbot.util.SendmsgUtil;
import kotlin.coroutines.CoroutineContext;
import net.mamoe.mirai.event.EventHandler;
import net.mamoe.mirai.event.SimpleListenerHost;
import net.mamoe.mirai.event.events.*;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.MessageSource;
import net.mamoe.mirai.message.data.QuoteReply;
import org.jetbrains.annotations.NotNull;
import org.languagetool.rules.patterns.Match;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class MessageListener extends SimpleListenerHost {

    private static final Logger log = LoggerFactory.getLogger(MessageListener.class);

    private ApplicationContext applicationContext;
    @Autowired
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
    //todo 封装dao层
    @Autowired
    MessageMapper messageMapper;
    DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /***
     * FIXME 方法内部处理不了的异常才抛出由上层进行处理,否则直接catch,出现未知异常一定要error级别, 待修改
     * 异常集中处理
     * @param context
     * @param exception
     */
    @Override
    public void handleException(@NotNull CoroutineContext context, @NotNull Throwable exception) {
        if (SimpleListenerHost.getEvent(exception) instanceof MessageEvent) {
            MessageEvent event = (MessageEvent) SimpleListenerHost.getEvent(exception);
            if (event == null) return;
            var e = SimpleListenerHost.getRootCause(exception);
            if (e instanceof TipsException || e instanceof TipsRuntimeException) {
                event.getSubject().sendMessage(e.getMessage());
            } else if (e instanceof ConnectException || e instanceof UnknownHttpStatusCodeException) {
                event.getSubject().sendMessage("网络连接超时，请稍后再试");
            } else if (e instanceof RestClientException && e.getCause() instanceof RequestError) {
                RequestError reser = (RequestError) e.getCause();
                if (reser.status.value() == 404 || reser.status.getReasonPhrase().equals("Not Found")) {
                    event.getSubject().sendMessage("请求目标不存在");
                }else if(reser.status.getReasonPhrase().equals("Bad Request")){
                    event.getSubject().sendMessage("出现请求错误，可能为您的令牌已失效，请尝试更新令牌(私发bot\"!bind\")\n若仍未解决，请耐心等待bug修复");
                }
            } else if (e instanceof RunError) {
                log.error("严重异常:", e);
            } else {
                log.info("捕捉其他异常", e);
            }
        }
    }

    /***
     * 监听消息分发
     * @param event
     * @throws Throwable
     */
    @Async
    @EventHandler
    public void msg(MessageEvent event) throws Throwable {
        //判断是否拉黑用户
        if (Permission.FRILED_BLACK.contains(event.getSender().getId())) {
            return;
        }
        for(var ins : Instruction.values()){
            Matcher matcher = Pattern.compile(ins.getRegex()).matcher(event.getMessage().contentToString());
            if (matcher.find() && applicationContext != null) {
                var service = (MessageService) applicationContext.getBean(ins.getName());
                service.HandleMessage(event, matcher);
            }
        }
    }

    /***
     * 处理群邀请
     * @param event
     * @throws Exception
     */
    @Async
    @EventHandler
    public void msg(BotInvitedJoinGroupRequestEvent event) throws Exception {
        StringBuffer sb = new StringBuffer();
        if(Permission.GROUP_BLACK.contains(event.getGroupId())){
            sb.append("收到来自黑名单群组 \n")
                    .append(event.getGroupName())
                    .append('(')
                    .append(event.getGroupId())
                    .append(')')
                    .append("已拒绝");
        } else {
            sb.append("接收到来自 \n")
                    .append(event.getGroupName())
                    .append('(')
                    .append(event.getGroupId())
                    .append(')')
                    .append("加群邀请，已同意");
            event.accept();
        }
        //发送给管理群
        event.getBot().getGroup(746671531L).sendMessage(sb.toString());
    }

    /***
     * 处理添加好友请求(默认同意
     * @param event
     * @throws Exception
     */
    @Async
    @EventHandler
    public void msg(NewFriendRequestEvent event) throws Exception {
        event.accept();
    }

    /***
     * 发送消息前的调用,检测是否复读
     * @param event
     * @throws Exception
     */
    @Async
    @EventHandler
    public void msg(MessagePreSendEvent event) throws Exception {
        SendmsgUtil.check(event);
    }

    /***
     * 输出发送的消息
     * @param event
     */
    @Async
    @EventHandler
    public void msg(MessagePostSendEvent event) {
        log.info(event.getMessage().contentToString());
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
