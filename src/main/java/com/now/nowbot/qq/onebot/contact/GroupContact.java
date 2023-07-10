package com.now.nowbot.qq.onebot.contact;

import com.mikuac.shiro.core.Bot;
import com.now.nowbot.qq.enums.Role;

public class GroupContact extends Contact implements com.now.nowbot.qq.contact.GroupContact {
    long groupId;
    Role role;

    public GroupContact(Bot bot, long id, String name, String role, long groupId) {
        super(bot, id);
        setName(name);
        this.groupId = groupId;
        this.role = Role.fromString(role);
    }

    long getGroupId() {
        return groupId;
    }

    @Override
    public Role getRoll() {
        return null;
    }
}
