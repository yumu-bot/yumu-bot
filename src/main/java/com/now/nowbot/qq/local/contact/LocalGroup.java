package com.now.nowbot.qq.local.contact;

import com.now.nowbot.qq.enums.Role;

import java.util.List;

public class LocalGroup extends LocalContact implements com.now.nowbot.qq.contact.Group {
    public LocalGroup() {
    }

    @Override
    public boolean isAdmin() {
        return true;
    }

    @Override
    public com.now.nowbot.qq.contact.GroupContact getUser(long qq) {
        return new GroupContact();
    }

    @Override
    public List<? extends com.now.nowbot.qq.contact.GroupContact> getAllUser() {
        return List.of();
    }

    @Override
    public void sendFile(byte[] data, String name) {
        var path = super.saveFile(name, data);
        log.info("bot: 发送文件 {}", path);
    }

    static class GroupContact extends LocalContact implements com.now.nowbot.qq.contact.GroupContact {

        @Override
        public Role getRole() {
            return Role.ADMIN;
        }
    }
}
