package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.aop.CheckPermission;
import com.now.nowbot.config.Permission;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.qq.message.AtMessage;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.util.QQMsgUtil;
//**************************** 吧net.mamoe.mirai.event.events 包换成
//import net.mamoe.mirai.event.events.GroupMessageEvent;
//import net.mamoe.mirai.event.events.MessageEvent;
//import net.mamoe.mirai.message.data.At;
//************************************  com.now.nowbot.QQ.下面的, 其中At 改成AtMessage
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service("BAN")
public class BanService implements MessageService<Matcher> {
    Permission   permission;
    ImageService imageService;

    private Pattern p1 = Pattern.compile("^[!！]\\s*(?i)(ym)?(super|sp(?!\\w))+");

    @Autowired
    public BanService(Permission permission, ImageService imageService) {
        this.permission = permission;
        this.imageService = imageService;
    }

    @Override
    public boolean isHandle(MessageEvent event, DataValue<Matcher> data) {
        //没想好怎么做
        var m = p1.matcher(event.getRawMessage().trim());
        if (m.find()) {
            data.setValue(m);
            return true;
        }
        return false;
    }

    @Override
    @CheckPermission
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        boolean ban = false;
        long sendQQ = event.getSender().getId();
        //*******************************************************************************************/
        //消息改成 event.getMessage()
        //String msg = event.getMessage().contentToString(); //原来
        String msg = event.getRawMessage(); // 对
        //*******************************************************************************************/
        int index;
        var at = QQMsgUtil.getType(event.getMessage(), AtMessage.class);
        if ((index = msg.indexOf("list")) != -1) {
            if (Permission.isSuper(sendQQ)) {
                Set<Long> groups = Permission.getAllW().getGroupList();
                StringBuilder sb = new StringBuilder("白名单包含:\n");
                for (Long id : groups) {
                    sb.append(id).append("\n");
                }
                QQMsgUtil.sendImage(event.getSubject(), imageService.drawLine(sb));

            }
//            我都忘了这个分支是做什么的
//            else if (event instanceof GroupMessageEvent groupMessageEvent && Permission.isGroupAdmin(groupMessageEvent.getGroup().getId(), sendQQ)){}
        } else if ((index = msg.indexOf("add")) != -1) {
            if (Permission.isSuper(sendQQ)) {
                matcher = Pattern.compile("add\\s*(?<id>\\d+)").matcher(msg);
                if (matcher.find()) {
                    var add = permission.addGroup(Long.parseLong(matcher.group("id")), true, false);
                    if (add) {
                        /***********************************  消息改成相应的类  ************************************************/
                        event.getSubject().sendText("添加成功");
//                        如果是 复杂的消息 使用 com.now.nowbot.QQ.message.MessageChain 构造
                        /*
                        var aaa = new MessageChain.MessageChainBuilder()
                                .addText("第1句话")
                                .addImage("图图连接")
                                .addText("第2句话")
                                .addAt(112233L)
                                .addAtAll()
                                .build();
                        event.getSender().sendMessage(aaa);

                         */
                        /*******************************************************************************************/
                    }
                }
            }
//            else if (event instanceof GroupMessageEvent groupMessageEvent && Permission.isGroupAdmin(groupMessageEvent.getGroup().getId(), sendQQ)){}
        }
    }
}
