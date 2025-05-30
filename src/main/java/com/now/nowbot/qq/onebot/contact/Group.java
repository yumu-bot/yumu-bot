package com.now.nowbot.qq.onebot.contact;

import com.mikuac.shiro.core.Bot;
import com.mikuac.shiro.dto.action.common.ActionData;
import com.mikuac.shiro.dto.action.response.DownloadFileResp;
import com.now.nowbot.qq.contact.GroupContact;
import com.now.nowbot.util.QQMsgUtil;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Group extends Contact implements com.now.nowbot.qq.contact.Group {
    String name = null;

    public Group(Bot bot, long id) {
        super(bot, id);
    }

    public Group(Bot bot, long id, String name) {
        super(bot, id);
        this.name = name;
    }

    @Override
    public String getName() {
        if (name != null) return name;
        try {
            var data = bot.getGroupInfo(getId(), false).getData();
            return data.getGroupName();
        } catch (Exception e) {
            log.error("获取群名{}失败", getId());
            return "unknown group";
        }
    }

    @Override
    public boolean isAdmin() {
        var data = bot.getGroupMemberInfo(getId(), bot.getSelfId(), false).getData();
        return data.getRole().equals("owner") || data.getRole().equals("admin");
    }

    @Override
    public GroupContact getUser(long qq) {
        var data = bot.getGroupMemberInfo(getId(), qq, false).getData();
        return new com.now.nowbot.qq.onebot.contact.GroupContact(bot, data.getUserId(), data.getNickname(), data.getRole(), this.getId());
    }

    @Override
    public List<? extends GroupContact> getAllUser() {
        var data = bot.getGroupMemberList(getId()).getData();
        return data.stream().map(f -> new com.now.nowbot.qq.onebot.contact.GroupContact(bot, f.getUserId(), f.getNickname(), f.getRole(), this.getId())).toList();
    }

    @Override
    public void sendFile(byte[] data, String name) {
        String url;
        if (QQMsgUtil.botInLocal(bot.getSelfId())) {
            url = QQMsgUtil.getFileUrl(data, name);
        } else {
            url = QQMsgUtil.getFilePubUrl(data, name);
        }
        try {
            ActionData<DownloadFileResp> rep = null;
            for (int i = 0; i < 5; i++) {
                rep = bot.customRawRequest(() -> "download_file", Map.of(
                        "name", name,
                        "base64", QQMsgUtil.byte2str(data) //框架说这里要加 base64://，但是看起来加了会直接跑到文件里？
                ), DownloadFileResp.class);
                if (rep != null) break;
            }
            if (Objects.isNull(rep) || Objects.isNull(rep.getData())) {
                rep = bot.downloadFile(url);
            }

            if (Objects.isNull(rep.getData())) {
                log.error("发送文件失败: 客户端不支持接收文件");
                return;
            }

            bot.uploadGroupFile(getId(), rep.getData().getFile(), name);
        } catch (Exception e) {
            log.error("文件上传错误", e);

        } finally {
            QQMsgUtil.removeFileUrl(url);
        }
    }
}
