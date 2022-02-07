package com.now.nowbot.service.MessageService;

import com.now.nowbot.config.Permission;
import com.now.nowbot.dao.BindDao;
import com.now.nowbot.model.BinUser;
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.throwable.TipsException;
import com.now.nowbot.throwable.serviceException.BindException;
import com.now.nowbot.util.ASyncMessageUtil;
import com.now.nowbot.util.BindingUtil;
import com.now.nowbot.util.QQMsgUtil;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.message.MessageReceipt;
import net.mamoe.mirai.message.data.At;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;

@Service("bind")
public class BindService implements MessageService {
    public record bind(Long key, MessageReceipt<Contact> receipt, Long qq){}
    public static final Map<Long, bind> BIND_MSG_MAP = new ConcurrentHashMap<>();
    OsuGetService osuGetService;
    BindDao bindDao;
    @Autowired
    public BindService(OsuGetService osuGetService, BindDao bindDao){
        this.osuGetService = osuGetService;
        this.bindDao = bindDao;
    }

    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable{

        if (Permission.isSupper(event.getSender().getId())){
            At at = QQMsgUtil.getType(event.getMessage(), At.class);
            if (matcher.group("un") != null){
                var user = bindDao.getUser(at.getTarget());

                if (bindDao.unBind(user)){
                    throw new BindException(BindException.Type.BIND_Client_RelieveBindSuccess);
                }else {
                    throw new BindException(BindException.Type.BIND_Client_RelieveBindFailed);
                }
            }
            if (at != null) {
                // 只有管理才有权力@人绑定,提示就不改了
                event.getSubject().sendMessage("请发送绑定用户名");
                var lock = ASyncMessageUtil.getLock(event.getSubject().getId(), event.getSender().getId());
                var s = ASyncMessageUtil.getEvent(lock);//阻塞,注意超时判空
                if (s != null) {
                    event.getSubject().sendMessage("正在为" + at.getTarget() + "绑定 >>" + s.getMessage().contentToString());
                }else {
                    event.getSubject().sendMessage("超时或错误,结束接受");
                }
                return;
            }
        }
        //将当前毫秒时间戳作为 key
        long timeMillis = System.currentTimeMillis();
        //群聊验证是否绑定
        if ((event.getSubject() instanceof Group)) {
            BinUser user = null;
            try {
                user = bindDao.getUser(event.getSender().getId());
            } catch (TipsException e) {//未绑定时会出现file not find
                String state = event.getSender().getId() + "+" + timeMillis;
                //将消息回执作为 value
                var receipt = event.getSubject().sendMessage(new At(event.getSender().getId()).plus(osuGetService.getOauthUrl(state)));
                //默认110秒后撤回
                receipt.recallIn(110 * 1000);
                //此处在 controller.msgController 处理
                BIND_MSG_MAP.put(timeMillis, new bind(timeMillis, receipt, event.getSender().getId()));
                return;
            }
            throw new BindException(BindException.Type.BIND_Client_AlreadyBound);
        }
        //私聊不验证是否绑定
        String state = event.getSender().getId() + "+" + timeMillis;
        var receipt = event.getSubject().sendMessage(osuGetService.getOauthUrl(state));
        receipt.recallIn(110 * 1000);
        BIND_MSG_MAP.put(timeMillis, new bind(timeMillis, receipt, event.getSender().getId()));
    }
}
