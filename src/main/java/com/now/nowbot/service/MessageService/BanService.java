package com.now.nowbot.service.MessageService;

import com.now.nowbot.aop.CheckPermission;
import com.now.nowbot.config.Permission;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.qq.message.Message;
import com.now.nowbot.qq.message.MessageChain;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.util.QQMsgUtil;
//**************************** 吧net.mamoe.mirai.event.events 包换成
//import net.mamoe.mirai.event.events.GroupMessageEvent;
//import net.mamoe.mirai.event.events.MessageEvent;
//import net.mamoe.mirai.message.data.At;
//************************************  com.now.nowbot.QQ.下面的, 其中At 改成AtMessage
import net.mamoe.mirai.message.data.At;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service("ban")
public class BanService implements MessageService{
    Permission permission;
    ImageService imageService;
    @Autowired
    public BanService(Permission permission, ImageService imageService){
        this.permission = permission;
        this.imageService = imageService;
    }

    @Override
    @CheckPermission
    public void HandleMessage(MessageEvent event, Matcher matcher) throws Throwable {
        boolean ban = false;
        long sendQQ = event.getSender().getId();
        /*******************************************************************************************/
//        消息改成 event.getMessage()
//        String msg = event.getMessage().contentToString(); //原来
        String msg = event.getRawMessage(); // 对
        /*******************************************************************************************/
        int index;
        var at = QQMsgUtil.getType(event.getMessage(), At.class);
        if ((index = msg.indexOf("list")) != -1){
            if (Permission.isSupper(sendQQ)){
                Set<Long> groups = Permission.getAllW().getGroupList();
                StringBuilder sb = new StringBuilder("白名单包含:\n");
                for (Long id : groups){
                    sb.append(id).append("\n");
                }
                QQMsgUtil.sendImage(event.getSubject(), imageService.drawLine(sb));

            }
//            我都忘了这个分支是做什么的
//            else if (event instanceof GroupMessageEvent groupMessageEvent && Permission.isGroupAdmin(groupMessageEvent.getGroup().getId(), sendQQ)){}
        } else if ((index = msg.indexOf("add")) != -1) {
            if (Permission.isSupper(sendQQ)){
                matcher = Pattern.compile("add\\s*(?<id>\\d+)").matcher(msg);
                if (matcher.find()) {
                    var add = permission.addGroup(Long.parseLong(matcher.group("id")), true, false);
                    if (add){
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
