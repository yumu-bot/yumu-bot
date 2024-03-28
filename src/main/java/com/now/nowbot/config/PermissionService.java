package com.now.nowbot.config;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class PermissionService {

    /**
     * false 黑名单; true 白名单
     */
    private final boolean isGroupWhite;
    private final boolean isUserWhite;
    private final boolean userSet;
    private final Set<Long> groupList;
    private final Set<Long> userList;
    private final Set<Long> groupSelfBlackList;
    private       boolean enable = true;

    public PermissionService(boolean isGroupWhite, boolean isUserWhite, boolean userSet, List<Long> groups, List<Long> users, List<Long> groupSelfBlock) {
        this.isGroupWhite = isGroupWhite;
        this.isUserWhite = isUserWhite;
        this.userSet = userSet;
        if (userSet)
            this.groupSelfBlackList = new HashSet<>();
        else
            this.groupSelfBlackList = null;
        userList = new HashSet<>(users);
        groupList = new HashSet<>(groups);
    }

    /**
     * 检查服务是否可用
     *
     * @param group 群id, 可为空
     * @param qq    qq
     * @return true 可用
     */
    public boolean check(Long group, Long qq) {
        if (! enable) return false;
        if (isUserWhite) {
            if (! userList.contains(qq)) return false;
        } else {
            if (userList.contains(qq)) return false;
        }
        if (Objects.nonNull(group) && isGroupWhite) {
            if (! groupList.contains(group)) return false;
        } else {
            if (groupList.contains(group)) return false;
            if (Objects.nonNull(groupSelfBlackList) && groupSelfBlackList.contains(group)) return false;
        }
        return true;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    public boolean isDisable() {
        return ! enable;
    }

    public boolean canSet() {
        return userSet;
    }

    public void addGroup(Long id) {
        groupList.add(id);
    }

    public void addSelfGroup(Long id) {
        if (Objects.nonNull(groupSelfBlackList)) groupSelfBlackList.add(id);
    }

    public void addUser(Long id) {
        userList.add(id);
    }
}
