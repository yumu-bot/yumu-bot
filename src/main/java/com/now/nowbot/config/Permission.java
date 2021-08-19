package com.now.nowbot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@ConfigurationProperties(prefix = "mirai.permission")
public class Permission {
    public Set<Long> groupBlacklist;
    public Set<Long> friendBlacklist;
    public Set<Long> groupWhitelist;
    public Set<Long> friendWhitelist;
    public Set<Long> superUser;

    public Set<Long> getGroupBlacklist() {
        return groupBlacklist;
    }

    public void setGroupBlacklist(Set<Long> groupBlacklist) {
        this.groupBlacklist = groupBlacklist;
    }

    public Set<Long> getFriendBlacklist() {
        return friendBlacklist;
    }

    public void setFriendBlacklist(Set<Long> friendBlacklist) {
        this.friendBlacklist = friendBlacklist;
    }

    public Set<Long> getGroupWhitelist() {
        return groupWhitelist;
    }

    public void setGroupWhitelist(Set<Long> groupWhitelist) {
        this.groupWhitelist = groupWhitelist;
    }

    public Set<Long> getFriendWhitelist() {
        return friendWhitelist;
    }

    public void setFriendWhitelist(Set<Long> friendWhitelist) {
        this.friendWhitelist = friendWhitelist;
    }

    public Set<Long> getSuperUser() {
        return superUser;
    }

    public void setSuperUser(Set<Long> superUser) {
        this.superUser = superUser;
    }
}
