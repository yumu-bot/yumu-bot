package com.now.nowbot.qq.contact;

import java.util.List;

public interface Group extends Contact {
    String getName();

    boolean isAdmin();

    GroupContact getUser(long qq);

    List<? extends GroupContact> getAllUser();

    void sendFile(byte[] data, String name);
}
