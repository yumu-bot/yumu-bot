package com.now.nowbot.config;

import java.util.Set;

class PermissionData{
    //是否为白名单
    private boolean isWhite;
    //直接简化 true->仅超级管理员能操作      false->群管也能操作
    private boolean Supper;
    //列表
    private Set<Long> GroupList;
    private Set<Long> FriendList;

    public PermissionData(Set<Long> friend, Set<Long> group) {
        this.FriendList = friend;
        this.GroupList = group;
    }

    public boolean isWhite() {
        return isWhite;
    }

    public void setWhite(boolean white) {
        isWhite = white;
    }

    public boolean isSupper() {
        return Supper;
    }

    public void setSupper(boolean supper) {
        Supper = supper;
    }

    public Set<Long> getGroupList() {
        return GroupList;
    }

    public void setGroupList(Set<Long> groupList) {
        GroupList = groupList;
    }

    public Set<Long> getFriendList() {
        return FriendList;
    }

    public void setFriendList(Set<Long> friendList) {
        FriendList = friendList;
    }

    public boolean hasFriend(Long id){
        if (FriendList != null) {
            return FriendList.contains(id);
        }
        else return false;
    }

    public boolean hasGroup(Long id){
        if (GroupList != null) {
            return GroupList.contains(id);
        }
        else return false;
    }
    String getMsg(String name){
        StringBuilder sb = new StringBuilder(name);
        sb.append("->").append(isWhite?"白":"黑").append("名单模式\n")
                .append("管理员").append(isSupper()?"不":"").append("可修改\n");
        if (GroupList != null) {
            sb.append("\n群组: ");
            for (Long id : getGroupList()) {
                sb.append(id).append(' ');
            }
        }
        if (FriendList != null) {
            sb.append("\nqq: ");
            for (Long id : getFriendList()) {
                sb.append(id).append(' ');
            }
        }
        return sb.toString();
    }
}
