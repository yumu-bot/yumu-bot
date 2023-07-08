package com.now.nowbot.qq.enums;

public enum Role {
    OWNER, ADMIN, MEMBER;
    public static Role fromString(String s) {
        Role r;
        switch (s) {
            case "owner": r = OWNER;break;
            case "admin": r = ADMIN;break;
            case "member": r = MEMBER;break;
            default:r = MEMBER;
        }
        return r;
    }
}