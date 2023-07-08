package com.now.nowbot.service.MessageService;

import com.now.nowbot.config.Permission;
import com.now.nowbot.dao.BindDao;
import com.now.nowbot.model.BinUser;
import com.now.nowbot.qq.event.GroupMessageEvent;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.qq.message.MessageChain;
import com.now.nowbot.service.OsuGetService;
import com.now.nowbot.throwable.ServiceException.BindException;
import com.now.nowbot.util.ASyncMessageUtil;
import com.now.nowbot.util.QQMsgUtil;
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
    public static final Map<Long, Bind> BIND_MSG_MAP = new ConcurrentHashMap<>();
    private static Logger               log          = LoggerFactory.getLogger(BindService.class);
    OsuGetService osuGetService;
    BindDao bindDao;
    @Autowired
    public BindService(OsuGetService osuGetService, BindDao bindDao) {
        this.osuGetService = osuGetService;
        this.bindDao = bindDao;
    }

    @Override
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        var from = event.getSubject();
        if (Permission.isSupper(event.getSender().getId())) {
            At at = QQMsgUtil.getType(event.getMessage(), At.class);
            if (matcher.group("un") != null && at != null) {
                unbind(at.getTarget());
            }
            if (at != null) {
                // 只有管理才有权力@人绑定,提示就不改了
                from.sendMessage("你叫啥名呀？告诉我吧");
                // throw new BindException(BindException.Type.BIND_Client_BindingNoName);
                var lock = ASyncMessageUtil.getLock(from.getId(), event.getSender().getId());
                var s = ASyncMessageUtil.getEvent(lock);//阻塞,注意超时判空
                if (s != null) {
                    String Oname = s.getMessage();
                    Long id;
                    try {
                        id = osuGetService.getOsuId(Oname);
                    } catch (Exception e) {
                        throw new BindException(BindException.Type.BIND_Player_NotFound);
                    }
                    try {
                        var buser = bindDao.getUserLiteFromOsuid(id);
                        if (buser.getQq() == null) {
                            from.sendMessage("正在将" + at.getTarget() + "绑定到 (" + id + ")" + Oname + "上");
                            buser.setQq(at.getTarget());
                            bindDao.update(buser);
                            throw new BindException(BindException.Type.BIND_Me_Success);
                            //from.sendMessage("绑定成功");
                        } else {
                            from.sendMessage(buser.getOsuName() + "您已绑定在 QQ " + at.getTarget() + " 上，是否覆盖？回复 OK 生效");
                            s = ASyncMessageUtil.getEvent(lock);
                            if (s != null && s.getMessage().startsWith("OK")) {
                                buser.setQq(at.getTarget());
                                bindDao.update(buser);
                                throw new BindException(BindException.Type.BIND_Me_Success);
                                //from.sendMessage("绑定成功");
                            } else {
                                throw new BindException(BindException.Type.BIND_Client_BindingRefused);
                                //from.sendMessage("已取消");
                            }
                        }
                    } catch (BindException e) {
                        from.sendMessage("正在将" + at.getTarget() + "绑定到 (" + id + ")" + Oname + "上");
                        bindDao.saveUser(at.getTarget(), Oname, id);
                        throw new BindException(BindException.Type.BIND_Me_Success);
                    }
                    //return;
                } else {
                    throw new BindException(BindException.Type.BIND_Client_BindingOvertime);
                    //from.sendMessage("超时或错误,结束接受"); return;
                }
            }
        }else if (matcher.group("un") != null){
            from.sendMessage("解绑请联系管理员");
            return;
        }
        var name = matcher.group("name");
        if (name != null){
            long d;
            try {
                 d = osuGetService.getOsuId(name);
            } catch (Exception e) {
                throw new BindException(BindException.Type.BIND_Player_NotFound);
                //from.sendMessage("未找到osu用户"+name); return;
            }
            BinUser nuser = null;
            try {
                nuser = bindDao.getUser(event.getSender().getId());
            } catch (BindException e) {
                //未绑定
            }
            if (nuser != null){
                throw new BindException(BindException.Type.BIND_Client_AlreadyBound);
            }
            try {
                var buser = bindDao.getUserFromOsuid(d);
                from.sendMessage(name + " 已绑定 (" + buser.getQq() + ")，若绑定错误，请尝试重新绑定！\n(!ymbind / !ymbi / !bi)");
            } catch (BindException e) {
                bindDao.saveUser(event.getSender().getId(), name, d);
                from.sendMessage("正在将 " + event.getSender().getId() + " 绑定到 (" + d + ") " + name + " 上");
            }
            return;
        }
        //将当前毫秒时间戳作为 key
        long timeMillis = System.currentTimeMillis();
        //群聊验证是否绑定
        if ((event instanceof GroupMessageEvent)) {
            BinUser user = null;
            try {
                user = bindDao.getUser(event.getSender().getId());
            } catch (BindException ignore) {
                // do nothing
            }
            if (user != null && user.getAccessToken() != null){
                from.sendMessage("您已绑定 ("+user.getOsuID()+") "+user.getOsuName()+"。\n但您的令牌仍有可能已经失效。回复 OK 重新绑定。");
                var lock = ASyncMessageUtil.getLock(from.getId(), event.getSender().getId());
                var s = ASyncMessageUtil.getEvent(lock);
                if(s !=null && s.getMessage().trim().equalsIgnoreCase("OK")){
                }else {
                    return;
                }
            }
            //未绑定或未完全绑定
            String state = event.getSender().getId() + "+" + timeMillis;
            //将消息回执作为 value
            state = osuGetService.getOauthUrl(state);
            var send = new MessageChain.MessageChainBuilder()
                    .addAt(event.getSender().getId())
                    .addText("\n")
                    .addText(state)
                    .build();
            var receipt = from.sendMessage(send);
            //默认110秒后撤回
            from.recallIn(receipt, 110 * 1000);


            //此处在 controller.msgController 处理
            BIND_MSG_MAP.put(timeMillis, new Bind(timeMillis, receipt, event.getSender().getId(), event.getSubject().getId(), event.getBot().getSelfId()));
            //---------------
//            throw new BindException(BindException.Type.BIND_Client_AlreadyBound);
        }else {
            //私聊不验证是否绑定
            String state = event.getSender().getId() + "+" + timeMillis;
            var receipt = from.sendMessage(osuGetService.getOauthUrl(state));
            from.recallIn(receipt, 110 * 1000);
            BIND_MSG_MAP.put(timeMillis, new Bind(timeMillis, receipt, event.getSender().getId(), event.getSubject().getId(), event.getBot().getSelfId()));
        }
    }

    private void unbind(Long qqId) throws BindException {
        if (qqId == null) throw new BindException(BindException.Type.BIND_Player_NotFound);
        BinUser user = bindDao.getUser(qqId);
        if (user == null) {
            throw new BindException(BindException.Type.BIND_Player_NoBind);
        }

        if (bindDao.unBind(user)) {
            throw new BindException(BindException.Type.BIND_Client_RelieveBindSuccess);
        } else {
            throw new BindException(BindException.Type.BIND_Client_RelieveBindFailed);
        }
    }

    public record Bind(Long key, Integer receipt, Long QQ, Long groupQQ, Long botQQ) {
    }

}