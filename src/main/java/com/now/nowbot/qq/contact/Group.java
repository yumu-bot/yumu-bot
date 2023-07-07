package com.now.nowbot.qq.contact;

import java.util.List;

public interface Group extends User {
    String getName();

    boolean isAdmin();

    Friend getUser(long qq);

    List<? extends Friend> getAllUser();
}
