package com.now.nowbot.service.msgServiceImpl;

import net.mamoe.mirai.event.events.FriendMessageEvent;


public class ChangePageServiceImpl extends MessageService{

    public ChangePageServiceImpl() {
        super("page");
    }

    @Override
    public void handleMsg(FriendMessageEvent event, String[] page) {
        String oldName;
        String newName;
//        if(NowbotConfig.SUPER_USER.contains(event.getSender().getId())){
//            if (page.length < 2) {
//                StringBuffer pages = new StringBuffer("page [<oldpage> <newpage>]\n当前已有指令:\n");
//                for (Object s : MessageService.servicesName.keySet()) {
//                    pages.append(s+"\n");
//                }
//                event.getSender().sendMessage(pages.toString());
//                return;
//            }
//            if (page.length == 2){
//                var pg = page[1];
//                switch (pg){
//                    case "reboot":{
//                        event.getSender().sendMessage("重启中").recallIn(10*1000);
//                        try {
//                            Runtime.getRuntime().exec("/root/update.sh");
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                        }
//                        break;
//                    }
//                }
//                return;
//            }
//            if(page.length == 3) {
//                oldName = page[1];
//                newName = page[2];
//                MessageService old = MessageService.servicesName.get(oldName);
//                try {
//                    MessageService.servicesName.put(newName, old);
//                    old.setKey(newName);
//                    MessageService.servicesName.remove(oldName);
//                    event.getSender().sendMessage("ok");
//                } catch (Exception e) {
//                    e.printStackTrace();
//                    MessageService.servicesName.put(newName, old);
//                }
//            }
//        }
    }
}
