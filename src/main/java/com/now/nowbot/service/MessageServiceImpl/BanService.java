package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.config.Permission;
import com.now.nowbot.model.Service.BanParam;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.qq.message.AtMessage;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.throwable.TipsException;
import com.now.nowbot.util.Instructions;
import com.now.nowbot.util.QQMsgUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Set;

@Service("BAN")
public class BanService implements MessageService<BanParam> {
    Permission permission;
    ImageService imageService;

    @Autowired
    public BanService(Permission permission, ImageService imageService) {
        this.permission = permission;
        this.imageService = imageService;
    }

    @Override
    public boolean isHandle(MessageEvent event, String messageText, DataValue<BanParam> data) {
        var matcher = Instructions.BAN.matcher(messageText);
        if (!matcher.find()) return false;

        var at = QQMsgUtil.getType(event.getMessage(), AtMessage.class);

        var qq = matcher.group("qq");
        var group = matcher.group("group");
        var name = matcher.group("name");
        var operate = matcher.group("operate");

        if (Objects.nonNull(at)) {
            data.setValue(new BanParam(at.getTarget(), null, operate, true));
            return true;
        }

        if (Objects.nonNull(qq)) {
            data.setValue(new BanParam(Long.parseLong(qq), null, operate, true));
            return true;
        }

        if (Objects.nonNull(group)) {
            data.setValue(new BanParam(Long.parseLong(group), null, operate, false));
            return true;
        }

        if (Objects.nonNull(name)) {
            data.setValue(new BanParam(null, name, operate, true));
            return true;
        }

        data.setValue(new BanParam(null, null, operate, false));
        return true;
    }


    @Override
    public void HandleMessage(MessageEvent event, BanParam param) throws Throwable {
        if (!Permission.isSuper(event.getSender().getId())) {
            throw new TipsException("只有超级管理员可以使用此功能！");
        }

        var from = event.getSubject();

        switch (param.operate()) {
            case "list", "l" -> SendPic(event, Permission.getAllW().getGroupList(), "白名单包含：");
            case "blacklist", "k" -> SendPic(event, Permission.getAllB().getGroupList(), "黑名单包含：");
            case "add", "a" -> {
                if (Objects.nonNull(param.qq()) && param.isUser()) {
                    var add = permission.addUser2PerMissionGroup(param.qq(), true, false);
                    if (add) {
                        from.sendMessage("成功添加用户进白名单");
                    }
                } else if (Objects.nonNull(param.qq())) {
                    throw new TipsException("群组功能还在制作中");
                    /*
                    var add = permission.addUser2PerMissionGroup(param.qq(), true, false);
                    if (add) {
                        from.sendMessage("成功添加群组");
                    }
                     */
                } else {
                    throw new TipsException("add 操作必须输入 qq！\n格式：!sp add qq=114514 / group=1919810");
                }
            }
            case "remove", "r" -> {
                if (Objects.nonNull(param.qq()) && param.isUser()) {
                    var remove = permission.removeUser4PermissionGroup(param.qq(), true);
                    if (remove) {
                        from.sendMessage("成功移除用户出白名单");
                    }
                } else if (Objects.nonNull(param.qq())) {
                    throw new TipsException("群组功能还在制作中");
                    /*
                    var add = permission.addUser2PerMissionGroup(param.qq(), false);
                    if (add) {
                        from.sendMessage("成功添加群组");
                    }
                     */
                } else {
                    throw new TipsException("remove 操作必须输入 qq！\n格式：!sp remove qq=114514 / group=1919810");
                }
            }
            case "ban", "b" -> {
                if (Objects.nonNull(param.qq()) && param.isUser()) {
                    var add = permission.addUser2FriendGroup(param.qq());
                    if (add) {
                        from.sendMessage("成功拉黑用户");
                    }
                } else if (Objects.nonNull(param.qq())) {
                    throw new TipsException("群组功能还在制作中");
                    /*
                    var add = permission.addUser2PerMissionGroup(param.qq(), true, false);
                    if (add) {
                        from.sendMessage("成功添加群组");
                    }
                     */
                } else {
                    //ban 玩家名也可以吧？
                    throw new TipsException("ban 操作必须输入 qq！\n格式：!sp ban qq=114514 / group=1919810");
                }
            }
            case "unban", "u" -> {
                if (Objects.nonNull(param.qq()) && param.isUser()) {
                    var add = permission.removeUser4PermissionGroup(param.qq(), true);
                    if (add) {
                        from.sendMessage("成功恢复用户");
                    }
                } else if (Objects.nonNull(param.qq())) {
                    throw new TipsException("群组功能还在制作中");
                    /*
                    var add = permission.addUser2PerMissionGroup(param.qq(), true, false);
                    if (add) {
                        from.sendMessage("成功添加群组");
                    }
                     */
                } else {
                    //ban 玩家名也可以吧？
                    throw new TipsException("unban 操作必须输入 qq！\n格式：!sp unban qq=114514 / group=1919810");
                }
            }

            case null, default -> throw new TipsException("请输入 super 操作！超管可用的操作有：\nlist：查询白名单\nblacklist：查询黑名单\nadd：添加用户至白名单\nremove：移除用户出白名单\nban：添加用户至黑名单\nunban：移除用户出黑名单");
        }
    }

    private void SendPic(MessageEvent event, Set<Long> groups, String introduction) {
        StringBuilder sb = new StringBuilder(introduction + "\n");
        for (Long qq : groups) {
            sb.append(qq).append("\n");
        }
        QQMsgUtil.sendImage(event.getSubject(), imageService.getPanelAlpha(sb));
    }
}
