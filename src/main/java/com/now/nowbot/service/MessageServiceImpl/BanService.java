package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.config.Permission;
import com.now.nowbot.config.PermissionParam;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.qq.message.AtMessage;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.throwable.ServiceException.BanException;
import com.now.nowbot.util.Instructions;
import com.now.nowbot.util.QQMsgUtil;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service("BAN")
public class BanService implements MessageService<BanService.BanParam> {
    @Resource
    Permission permission;
    @Resource
    ImageService imageService;

    public record BanParam(Long qq, String name, String operate, boolean isUser) {

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
        if (!Permission.isSuperAdmin(event.getSender().getId())) {
            throw new BanException(BanException.Type.SUPER_Permission_Admin);
        }

        var from = event.getSubject();

        switch (param.operate()) {
            case "list", "whitelist", "l", "w" -> SendImage(event, Permission.getWhiteList(), "白名单包含：");
            case "banlist", "blacklist", "k" -> SendImage(event, Permission.getBlackList(), "黑名单包含：");
            case "add", "a" -> {
                if (Objects.nonNull(param.qq()) && param.isUser()) {
                    var add = permission.addUser(param.qq(), true);
                    if (add) {
                        from.sendMessage(STR."成功添加用户 \{param.qq()} 进白名单");
                    }
                } else if (Objects.nonNull(param.qq())) {
                    //throw new TipsException("群组功能还在制作中");
                    var add = permission.addGroup(param.qq(), true, true);
                    if (add) {
                        from.sendMessage(STR."成功添加群组 \{param.qq()} 进白名单");
                    }
                } else {
                    throw new BanException(BanException.Type.SUPER_Receive_NoQQ, "add", "add");
                }
            }
            case "remove", "r" -> {
                if (Objects.nonNull(param.qq()) && param.isUser()) {
                    var remove = permission.removeUser(param.qq(), true);
                    if (remove) {
                        from.sendMessage(STR."成功移除用户 \{param.qq()} 出白名单");
                    }
                } else if (Objects.nonNull(param.qq())) {
                    //throw new TipsException("群组功能还在制作中");
                    var add = permission.removeGroup(param.qq(), false, true);
                    if (add) {
                        from.sendMessage(STR."成功移除群组 \{param.qq()} 出白名单");
                    }
                } else {
                    throw new BanException(BanException.Type.SUPER_Receive_NoQQ, "remove", "remove");
                }
            }
            case "ban", "b" -> {
                if (Objects.nonNull(param.qq()) && param.isUser()) {
                    var add = permission.addUser(param.qq(), false);
                    if (add) {
                        from.sendMessage(STR."成功拉黑用户 \{param.qq()}");
                    }
                } else if (Objects.nonNull(param.qq())) {
                    //throw new TipsException("群组功能还在制作中");
                    var add = permission.addGroup(param.qq(), false, true);
                    if (add) {
                        from.sendMessage(STR."成功拉黑群组 \{param.qq()}");
                    }
                } else {
                    throw new BanException(BanException.Type.SUPER_Receive_NoQQ, "ban", "ban");
                }
            }
            case "unban", "u" -> {
                if (Objects.nonNull(param.qq()) && param.isUser()) {
                    var add = permission.removeUser(param.qq(), false);
                    if (add) {
                        from.sendMessage(STR."成功恢复用户 \{param.qq()}");
                    }
                } else if (Objects.nonNull(param.qq())) {
                    //throw new TipsException("群组功能还在制作中");
                    var add = permission.removeGroup(param.qq(), false, true);
                    if (add) {
                        from.sendMessage(STR."成功恢复群组 \{param.qq()}");
                    }
                } else {
                    throw new BanException(BanException.Type.SUPER_Receive_NoQQ, "unban", "unban");
                }
            }

            case null, default -> throw new BanException(BanException.Type.SUPER_Instruction);
        }
    }

    private void SendImage(MessageEvent event, PermissionParam param, String info) {
        var from = event.getSubject();
        var users = param.getUserList();
        var groups = param.getGroupList();


        StringBuilder sb = new StringBuilder(STR."\{info}\nqq:");

        for (Long qq : users) {
            sb.append(qq).append("\n");
        }

        sb.append("group:").append('\n');

        for (Long qq : groups) {
            sb.append(qq).append("\n");
        }

        var image = imageService.getPanelAlpha(sb);
        from.sendImage(image);
    }
}
