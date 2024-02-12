package com.now.nowbot.config;

import java.util.Set;

public class PermissionParam {
    //是否为白名单
    private boolean isWhite;

    //直接简化 true->仅超级管理员能操作      false->群管也能操作
    private boolean administrator;
    private boolean adminOnly;
    //列表
    private Set<Long> GroupList = null;
    private Set<Long> UserList = null;

    public PermissionParam(boolean adminOnly) {
        this.adminOnly = adminOnly;
    }
    public PermissionParam(Set<Long> userSet, Set<Long> groupSet) {
        this.UserList = userSet;
        this.GroupList = groupSet;
    }

    public boolean isWhite() {
        return isWhite;
    }

    public void setWhite(boolean white) {
        isWhite = white;
    }

    public boolean isAdministrator() {
        return administrator;
    }

    public void setAdministrator(boolean administrator) {
        this.administrator = administrator;
    }

    public Set<Long> getGroupList() {
        return GroupList;
    }

    public void setGroupList(Set<Long> groupList) {
        GroupList = groupList;
    }

    public Set<Long> getUserList() {
        return UserList;
    }

    public void setUserList(Set<Long> userList) {
        UserList = userList;
    }

    public boolean hasUser(Long id){
        if (UserList != null) {
            return UserList.contains(id);
        }
        else return false;
    }

    public boolean hasGroup(Long id){
        if (GroupList != null) {
            return GroupList.contains(id);
        }
        else return false;
    }

    public boolean isAdminOnly() {
        return adminOnly;
    }

    public void setAdminOnly(boolean adminOnly) {
        this.adminOnly = adminOnly;
    }

    String getMessage(String name){
        if (isAdminOnly()){
            return STR."\{name}为管理员专用";
        }
        StringBuilder sb = new StringBuilder(name);
        sb.append("->").append(isWhite?"白":"黑").append("名单模式\n")
                .append("管理员").append(isAdministrator()?"不":"").append("可修改\n");
        if (GroupList != null) {
            sb.append("\n群组: ");
            for (Long id : getGroupList()) {
                sb.append(id).append(' ');
            }
        }
        if (UserList != null) {
            sb.append("\nQQ: ");
            for (Long id : getUserList()) {
                sb.append(id).append(' ');
            }
        }
        return sb.toString();
    }
}
