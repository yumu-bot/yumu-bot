package com.now.nowbot.qq.onebot.contact;

import com.now.nowbot.qq.enums.Role;

public class GroupContact extends Contact implements com.now.nowbot.qq.contact.GroupContact {
    long groupId;
    Role role;

    public GroupContact(long botId, long id, String name, String role, long groupId) {
        super(botId, id);
        setName(name);
        this.groupId = groupId;
        this.role = Role.fromString(role);
    }

    long getGroupId() {
        return groupId;
    }

    @Override
    public Role getRole() {
        return role;
    }
}
