package com.now.nowbot.qq.enums;

public enum Role {
    OWNER, ADMIN, MEMBER;
    public static Role fromString(String s) {
        return switch (s) {
            case "owner" -> OWNER;
            case "admin" -> ADMIN;
            case null, default -> MEMBER;
            //case "member" -> MEMBER;
        };
    }
}