package com.now.nowbot.service.MessageService;

import com.now.nowbot.config.Permission;
import com.now.nowbot.dao.BindDao;
import com.now.nowbot.model.BinUser;
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.throwable.serviceException.BindException;
import com.now.nowbot.util.ASyncMessageUtil;
import com.now.nowbot.util.QQMsgUtil;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.event.events.GroupMessageEvent;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.message.MessageReceipt;
import net.mamoe.mirai.message.data.At;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;

@Service("bind")
public class BindService implements MessageService {
    private static Logger log = LoggerFactory.getLogger(BindService.class);
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
        log.error("---------------------------------bind1---------------------------------------");
        if (Permission.isSupper(event.getSender().getId())){
            log.error("---------------------------------bind1-1---------------------------------------");

            At at = QQMsgUtil.getType(event.getMessage(), At.class);
            if (matcher.group("un") != null){
                log.error("---------------------------------bind1-2---------------------------------------");
                unbin(at.getTarget());
            }
            if (at != null) {
                log.error("---------------------------------bind1-3---------------------------------------");
                // 只有管理才有权力@人绑定,提示就不改了
                event.getSubject().sendMessage("请发送绑定用户名");
                var lock = ASyncMessageUtil.getLock(event.getSubject().getId(), event.getSender().getId());
                var s = ASyncMessageUtil.getEvent(lock);//阻塞,注意超时判空
                if (s != null) {
                    log.error("---------------------------------bind1-4---------------------------------------");

                    String Oname = s.getMessage().contentToString();
                    var d = osuGetService.getOsuId(Oname);
                    var buser = bindDao.getUserFromOsuid(d);
                    if (buser == null) {
                        event.getSubject().sendMessage("正在为" + at.getTarget() + "绑定 >>(" + d + ")" + Oname);
                        bindDao.saveUser(at.getTarget(), Oname, d);
                    }else {
                        event.getSubject().sendMessage( at.getTarget() + "已绑定 " + buser.getQq() + " ,确定是否覆盖,回复'确定'生效");
                        s = ASyncMessageUtil.getEvent(lock);
                        if (s != null && s.getMessage().contentToString().equals("确定")) {
                            buser.setQq(d);
                            bindDao.saveUser(buser);
                        }
                    }
                }else {
                    event.getSubject().sendMessage("超时或错误,结束接受");
                }
                return;
            }
        }

        log.error("---------------------------------bind2---------------------------------------");
        //将当前毫秒时间戳作为 key
        long timeMillis = System.currentTimeMillis();
        //群聊验证是否绑定
        if ((event instanceof GroupMessageEvent)) {
            log.error("---------------------------------bind3---------------------------------------");

            BinUser user = bindDao.getUser(event.getSender().getId());
            if (user == null){
                log.error("---------------------------------bind4---------------------------------------");

                String state = event.getSender().getId() + "+" + timeMillis;
                //将消息回执作为 value
                state = osuGetService.getOauthUrl(state);
                var send = new At(event.getSender().getId()).plus(state);
                var receipt = event.getSubject().sendMessage(send);
                //默认110秒后撤回
                receipt.recallIn(110 * 1000);
                //此处在 controller.msgController 处理
                BIND_MSG_MAP.put(timeMillis, new bind(timeMillis, receipt, event.getSender().getId()));
                return;
            }
            throw new BindException(BindException.Type.BIND_Client_AlreadyBound);
        }
        log.error("---------------------------------bind5---------------------------------------");

        //私聊不验证是否绑定
        String state = event.getSender().getId() + "+" + timeMillis;
        var receipt = event.getSubject().sendMessage(osuGetService.getOauthUrl(state));
        receipt.recallIn(110 * 1000);
        BIND_MSG_MAP.put(timeMillis, new bind(timeMillis, receipt, event.getSender().getId()));
    }

    private void unbin(Long qqId) throws BindException {
        if (qqId == null) throw new BindException(BindException.Type.BIND_Me_NoBind);
        BinUser user = bindDao.getUser(qqId);
        if (user == null){
            throw new BindException(BindException.Type.BIND_Me_NoBind);
        }

        if (bindDao.unBind(user)){
            throw new BindException(BindException.Type.BIND_Client_RelieveBindSuccess);
        }else {
            throw new BindException(BindException.Type.BIND_Client_RelieveBindFailed);
        }
    }
}
