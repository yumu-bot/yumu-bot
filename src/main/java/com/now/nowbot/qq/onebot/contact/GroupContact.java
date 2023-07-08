package com.now.nowbot.qq.onebot.contact;

import com.mikuac.shiro.core.Bot;
import com.now.nowbot.qq.enums.Role;

public class GroupContact extends Contact implements com.now.nowbot.qq.contact.GroupContact{
    Role role;
    public GroupContact(Bot bot, long id, String name, String role){
        super(bot, id);
        setName(name);
        this.role = Role.fromString(role);
    }
    @Override
    public Role getRoll() {
        return null;
    }
}
