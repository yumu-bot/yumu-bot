package com.now.nowbot.service.MessageServiceImpl;

import com.now.nowbot.aop.CheckPermission;
import com.now.nowbot.config.Permission;
import com.now.nowbot.qq.event.MessageEvent;
import com.now.nowbot.service.ImageService;
import com.now.nowbot.service.MessageService;
import com.now.nowbot.throwable.ServiceException.SwitchException;
import com.now.nowbot.throwable.TipsRuntimeException;
import com.now.nowbot.util.Instructions;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.reactive.function.client.WebClientResponseException;


@Service("SWITCH") //修改service名 "switch" 一定要修改 Permission
public class SwitchService implements MessageService<SwitchService.SwitchParam> {
    @Resource
    Permission permission;
    @Resource
    ImageService imageService;

    public record SwitchParam(long groupID, String serviceName, Operation operation) {
    }

    public enum Operation {
        REVIEW,
        ON,
        OFF,
    }

    private static Operation getOperation(String str) {
        return switch (str) {
            case "on", "start", "o", "s" -> Operation.ON;
            case "off", "close", "end", "f", "c", "e" -> Operation.OFF;
            case null, default -> Operation.REVIEW;
        };
    }

    @Autowired
    public SwitchService(Permission permission, ImageService imageService) {
        this.permission = permission;
        this.imageService = imageService;
    }

    @Override
    public boolean isHandle(MessageEvent event, String messageText, DataValue<SwitchParam> data) throws Throwable {
        var m = Instructions.SWITCH.matcher(messageText);
        if (!m.find()) {
            return false;
        }

        if (!Permission.isSuperAdmin(event.getSender().getId())) {
            throw new SwitchException(SwitchException.Type.SW_Permission_Admin);
        }

        var service = m.group("service");
        var operate = m.group("operate");
        //var o = Pattern.compile("(black|white)?list|on|off|start|close|[bkwlofsc]+");

        var groupID = m.group("group");

        if (StringUtils.hasText(service)) {
            if (StringUtils.hasText(operate)) {
                if (StringUtils.hasText(groupID)) {
                    data.setValue(new SwitchParam(Long.parseLong(groupID), service.toUpperCase(), getOperation(operate)));
                } else {
                    data.setValue(new SwitchParam(-1L, service.toUpperCase(), getOperation(operate)));
                }
            } else {
                if (StringUtils.hasText(groupID)) {
                    throw new SwitchException(SwitchException.Type.SW_Parameter_OnlyGroup);
                } else {
                    var op = getOperation(service.toUpperCase());
                    if (op != Operation.REVIEW) {
                        throw new SwitchException(SwitchException.Type.SW_Service_Missing);
                    }
                    data.setValue(new SwitchParam(-1L, null, Operation.REVIEW));
                }
            }
        } else {
            if (StringUtils.hasText(groupID)) {
                throw new SwitchException(SwitchException.Type.SW_Parameter_OnlyGroup);
            } else {
                throw new SwitchException(SwitchException.Type.SW_Instructions);
            }
        }

        return true;
    }

    @Override
    @CheckPermission(isSuperAdmin = true)
    public void HandleMessage(MessageEvent event, SwitchParam param) throws Throwable {
        var from = event.getSubject();
        var service = param.serviceName;
        var group = param.groupID;

        switch (param.operation) {
            case ON -> {
                try {
                    if (group == -1L) {
                        Permission.openService(service);
                        from.sendMessage(STR."已启动 \{service} 服务");
                    } else if (group == 0L) {
                        permission.removeGroupAll(service, true);
                        from.sendMessage(STR."已全面清除 \{service} 服务的禁止状态");
                    } else {
                        permission.removeGroup(service, group, true, false);
                        from.sendMessage(STR."已解禁群聊 \{group} 的 \{service} 服务");
                    }
                } catch (TipsRuntimeException e) {
                    throw new SwitchException(SwitchException.Type.SW_Service_RemoveNotExists, service);
                } catch (RuntimeException e) {
                    throw new SwitchException(SwitchException.Type.SW_Service_NotFound, service);
                }
            }

            case OFF -> {
                try {
                    if (group == -1L) {
                        Permission.closeService(service);
                        from.sendMessage(STR."已关闭 \{service} 服务");
                    } else if (group == 0L) {
                        permission.removeGroupAll(service, true);
                        from.sendMessage(STR."已全面清除 \{service} 服务的禁止状态");
                    } else {
                        permission.addGroup(service, group, true, false);
                        from.sendMessage(STR."已禁止群聊 \{group} 的 \{service} 服务");
                    }
                } catch (TipsRuntimeException e) {
                    throw new SwitchException(SwitchException.Type.SW_Service_AddExists, service);
                } catch (RuntimeException e) {
                    throw new SwitchException(SwitchException.Type.SW_Service_NotFound, service);
                }
            }

            case REVIEW -> {
                var md = getServiceListMarkdown();
                try {
                    var image = imageService.getPanelA6(md, "switch");
                    from.sendImage(image);
                } catch (HttpServerErrorException.InternalServerError | WebClientResponseException.InternalServerError e) {
                    throw new SwitchException(SwitchException.Type.SW_Render_Failed);
                }
            }
        }

    }

    private String getServiceListMarkdown() {
        StringBuilder sb = new StringBuilder();
        sb.append("## 服务：开关状态\n");

        sb.append("""
                | 状态 | 服务名 | 无法使用的群聊 |
                | :-: | :-- | :-- |
                """);

        var list = Permission.getCloseServices();

        for (String serviceName : Permission.getAllService()) {
            sb.append("| ").append(list.contains(serviceName) ? "-" : "O")
                    .append(" | ").append(serviceName)
                    .append(" | ").append("-") //114514, 1919810
                    .append(" |\n");
        }

        return sb.toString();
    }
}
