package com.now.nowbot.service.msgServiceImpl;

import com.now.nowbot.config.NowbotConfig;
import net.mamoe.mirai.event.events.FriendMessageEvent;
import org.springframework.stereotype.Service;

@Service
public class ChangePageServiceImpl extends MessageService{

    public ChangePageServiceImpl() {
        super("page");
    }

    @Override
    public void handleMsg(FriendMessageEvent event, String[] page) {
        String oldName;
        String newName;
        if(NowbotConfig.SUPER_USER.contains(event.getSender().getId())){
            if (page.length != 3) {
                StringBuffer pages = new StringBuffer("page [<oldpage> <newpage>]\n当前已有指令:\n");
                for (Object s : MessageService.servicesName.keySet()) {
                    pages.append(s+"\n");
                }
                event.getSender().sendMessage(pages.toString());
                return;
            }
            oldName = page[1];
            newName = page[2];
            MessageService old = MessageService.servicesName.get(oldName);
            try {
                MessageService.servicesName.put(newName, old);
                MessageService.servicesName.remove(oldName);
                old.setKey(newName);
                event.getSender().sendMessage("ok");
            } catch (Exception e) {
                e.printStackTrace();
                MessageService.servicesName.put(newName, old);
            }
        }
    }
}
