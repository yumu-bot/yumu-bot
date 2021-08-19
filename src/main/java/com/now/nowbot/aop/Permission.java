package com.now.nowbot.aop;

import java.util.HashSet;
import java.util.Set;


public class Permission {
    final Set<Long> WfriendList = new HashSet<>();
    final Set<Long> BfriendList = new HashSet<>();
    final Set<Long> WGroupList = new HashSet<>();
    final Set<Long> BGroupList = new HashSet<>();

    public Set<Long> getWfriendList() {
        return WfriendList;
    }

    public Set<Long> getBfriendList() {
        return BfriendList;
    }

    public Set<Long> getWGroupList() {
        return WGroupList;
    }

    public Set<Long> getBGroupList() {
        return BGroupList;
    }
}
